package endrov.recording.recmetMultidim;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.swing.JMenu;
import javax.vecmath.Vector3d;

import org.jdom.Element;

import endrov.basicWindow.BasicWindow;
import endrov.data.EvContainer;
import endrov.data.EvData;
import endrov.data.EvObject;
import endrov.flowBasic.math.EvOpImageAddImage;
import endrov.flowBasic.math.EvOpImageDivScalar;
import endrov.hardware.EvDevicePath;
import endrov.hardware.EvHardware;
import endrov.hardware.EvHardwareConfigGroup;
import endrov.imageset.EvChannel;
import endrov.imageset.EvImage;
import endrov.imageset.EvPixels;
import endrov.imageset.EvStack;
import endrov.imageset.Imageset;
import endrov.recording.CameraImage;
import endrov.recording.EvAcquisition;
import endrov.recording.RecordingResource;
import endrov.recording.ResolutionManager;
import endrov.recording.device.HWCamera;
import endrov.recording.device.HWTrigger;
import endrov.recording.device.HWTrigger.TriggerListener;
import endrov.recording.positionsWindow.Position;
import endrov.recording.widgets.RecSettingsChannel;
import endrov.recording.widgets.RecSettingsDimensionsOrder;
import endrov.recording.widgets.RecSettingsPositions;
import endrov.recording.widgets.RecSettingsRecDesc;
import endrov.recording.widgets.RecSettingsSlices;
import endrov.recording.widgets.RecSettingsTimes;
import endrov.recording.widgets.RecSettingsTimes.TimeType;
import endrov.util.EvDecimal;
import endrov.util.ProgressHandle;


/**
 * Simple multidimensional acquisition - positions, times, stacks, channels
 * 
 * @author Johan Henriksson
 * @author Kim Nordlöf, Erik Vernersson
 */
public class EvMultidimAcquisition extends EvAcquisition
	{

	
	
	/******************************************************************************************************
	 *                               Static                                                               *
	 *****************************************************************************************************/
	
	private static final String metaType="multidimAcq";
	

	/******************************************************************************************************
	 *                               Instance                                                             *
	 *****************************************************************************************************/
	
	public RecSettingsDimensionsOrder order;
	public RecSettingsChannel channel;
	public RecSettingsRecDesc desc;
	public RecSettingsSlices slices;
	public RecSettingsTimes times;
	public RecSettingsPositions positions;

	
	
	/**
	 * Thread to perform acquisition
	 */
	public class AcqThread extends Thread implements EvAcquisition.AcquisitionThread, TriggerListener
		{
		private EvMultidimAcquisition settings;
		private boolean toStop=true;

//		private Imageset imset=new Imageset();
		private EvDevicePath cam=null;
		private int currentFrameCount;
		private EvDecimal currentFrame;
		
		private int currentZCount;
		private EvDecimal dz;
		
		private String currentPos;
		
		private RecSettingsChannel.OneChannel currentChannel;
		
		
		
		/**
		 * Handle dimensions by recursing
		 */
		private abstract class RecOp
			{
			public RecOp recurse;
			public abstract void exec();
			}

		/**
		 * Acquire one plane
		 */
		private class RecOpSnap extends RecOp	
			{
			@Override
			public void exec()
				{
				HWCamera thecam=(HWCamera)cam.getDevice();
				
				//Check if this frame should be included
				if(currentChannel.z0>=currentZCount &&
						(currentZCount-currentChannel.z0)%currentChannel.zInc==0 &&
						currentFrameCount%currentChannel.tinc==0)
					{
					//Snap image, average if needed
					CameraImage camIm=thecam.snap();
					EvPixels pix=camIm.getPixels()[0];
					if(currentChannel.averaging!=1)
						{
						ProgressHandle ph=new ProgressHandle();
						for(int i=1;i<currentChannel.averaging;i++)
							{
							camIm=thecam.snap();
							EvPixels pix2=camIm.getPixels()[0];
							pix=new EvOpImageAddImage().exec1(ph, pix,pix2);
							}
						pix=new EvOpImageDivScalar(currentChannel.averaging).exec1(ph, pix);
						}
					EvImage evim=new EvImage(pix);

					
//					EvContainer container=imset;
					Imageset thisImset;
					if(currentPos==null)
					{
						thisImset=(Imageset)container;
					}
					else
					{
						EvObject thisOb=container.metaObject.get(currentPos);
						if(thisOb==null)
							container.metaObject.put(currentPos, thisOb=new Imageset());
						thisImset=(Imageset)thisOb;
					}
					
					
					
					/*
					private Imageset imset=new Imageset();
					
					String channelName=settings.containerStoreName;
					boolean isRGB=false;
					if(isRGB)
						{
						imset.metaObject.put(channelName+"R", new EvChannel());
						imset.metaObject.put(channelName+"G", new EvChannel());
						imset.metaObject.put(channelName+"B", new EvChannel());
						}
					else
						imset.metaObject.put(channelName, new EvChannel());
					*/
					
					//TODO fix container store name!!!!??
					
					//Get a stack, fill in metadata
					EvChannel ch=thisImset.getCreateChannel(settings.containerStoreName);
					EvStack stack=new EvStack();//.getCreateFrame(currentFrame);
					ch.putStack(currentFrame, stack);
					
					ResolutionManager.Resolution res=ResolutionManager.getCurrentResolutionNotNull(cam);
					
					stack.setRes(
						res.x,
						res.y,
						dz.multiply(currentChannel.zInc).doubleValue()
					);
					stack.setDisplacement(new Vector3d(
							RecordingResource.getCurrentStageX(),  //Always do this?
							RecordingResource.getCurrentStageY(),
							dz.multiply(currentChannel.z0).doubleValue() //scary!!!!
							));
					
					int zpos=(currentZCount-currentChannel.z0)/currentChannel.zInc;
					
					stack.putInt(zpos,evim);   //Need to account for the possibility to skip slices!!! and offset!!!
					//int zpos=currentZCount-currentChannel.z0;
					//stack.putInt(zpos, evim);
					
					//Update the GUI
					BasicWindow.updateWindows(); //TODO use hooks
					for(AcquisitionListener listener:listeners)
						listener.acquisitionEventStatus(""+currentChannel.name+"/"+currentFrameCount+"/"+dz.multiply(currentZCount));
					
					}
				
				}
			}
		
		
		
		
		/**
		 * Change channel and recurse
		 */
		private class RecOpChannel extends RecOp
			{
			public void exec()
				{
				for(RecSettingsChannel.OneChannel ch:channel.channels)
					{
					System.out.println("Channel "+ch.name);
					currentChannel=ch;

					//TODO test with proper groups
					EvHardwareConfigGroup.getConfigGroup(channel.configGroup).getState(ch.name).activate();
					
					recurse.exec();
					}
				}
			
			}

		/**
		 * Slice dimension: move to the next Z, recurse
		 */
		private class RecOpStack extends RecOp
			{
			public void exec()
				{
				if(slices.zType==RecSettingsSlices.ZType.ONEZ)
					{
					//Do not move along Z
					currentZCount=0;
					dz=EvDecimal.ONE;
					//currentZ=EvDecimal.ZERO;
					recurse.exec();
					}
				else if(slices.zType==RecSettingsSlices.ZType.NUMZ)
					{
					//Figure out number of slices and spacing
					int numz;
					if(slices.zType==RecSettingsSlices.ZType.NUMZ)
						{
						dz=slices.end.subtract(slices.start).divide(new EvDecimal(slices.numZ));
						numz=slices.numZ;
						}
					else //DZ
						{
						numz=slices.end.subtract(slices.start).divide(slices.dz).intValue();
						dz=slices.dz;
						}

					//Iterate through planes
					for(int az=0;az<numz;az++)
						{
						RecordingResource.setCurrentStageZ(slices.start.add(dz.multiply(az)).doubleValue());
						currentZCount=az;
						//currentZ=dz.multiply(az);
						recurse.exec();
						if(toStop)
							return;
						}
					}
				}
			
			}
		
		private Semaphore semTrigger=new Semaphore(0);

		/**
		 * Only makes sure to wait until the next frame
		 */
		private class RecOpTime extends RecOp
			{
			public void exec()
				{
				if(times.tType==RecSettingsTimes.TimeType.ONET)
					{
					currentFrameCount=0;
					currentFrame=EvDecimal.ZERO;
					recurse.exec();
					}
				else if(times.tType==TimeType.TRIGGER)
					{
					currentFrameCount=0;
					currentFrame=EvDecimal.ZERO;
					long startTimeMillis=System.currentTimeMillis();
					for(;;)
						{
						//Wait for triggerer or for user to request a stop
						try
							{
							semTrigger.acquire();
							semTrigger.drainPermits();
							}
						catch (InterruptedException e)
							{
							}
						if(toStop)
							return;
						
						recurse.exec();
						
						//Calculate next frame
						long currentTimeMillis=System.currentTimeMillis();
						long dt=currentTimeMillis-startTimeMillis;
						currentFrame=new EvDecimal(dt).divide(1000);
						currentFrameCount++;
						}
					}
				else if(times.freq==null)
					{
					//Run at maximum rate - best effort
					long startTime=System.currentTimeMillis();
					for(int i=0;;i++)
						{
						long thisTime=System.currentTimeMillis();
						currentFrame=new EvDecimal(thisTime-startTime).divide(1000);
						currentFrameCount=i;

						if((times.tType==RecSettingsTimes.TimeType.NUMT && i==times.numT) ||
								(times.tType==TimeType.SUMT && currentFrame.greaterEqual(currentFrame)) ||
								toStop)
							return;

						recurse.exec();
						}
					}
				else
					{
					//Run at fixed controlled rate
					EvDecimal dt=times.freq;
					int numt;

					if(times.tType==RecSettingsTimes.TimeType.NUMT)
						numt=times.numT;
					else
						numt=times.sumTime.divide(dt).intValue();

					for(int at=0;at<numt;at++)
						{
						long timeBefore=System.currentTimeMillis();
						currentFrameCount=at;
						currentFrame=dt.multiply(at);

						recurse.exec();

						//Wait until next frame
						long timeAfter;
						do
							{
							timeAfter=System.currentTimeMillis();
							if(toStop)
								return;
							}while((new EvDecimal(timeAfter-timeBefore)).less(dt.multiply(1000)));
						}

					}



				
				}
			}

		/**
		 * Move to the next position, recurse
		 */
		private class RecOpPos extends RecOp
		{
			public void exec()
			{	
				for(Position pos:positions.positions)
				{
					Map<String, Double> gotoPos=new HashMap<String, Double>();			
					
					//get all the axes
					for(int i = 0; i<pos.getAxisInfo().length; i++){
						gotoPos.put(pos.getAxisInfo()[i].getDevice().getAxisName()[pos.getAxisInfo()[i].getAxis()],
								pos.getAxisInfo()[i].getValue());
					}
					//go to position
					RecordingResource.setStagePos(gotoPos);
					System.out.println(gotoPos);
					currentPos=pos.getName();		
					
					recurse.exec();
					
				}			
			}
		}
	

		/**
		 * Build a call stack out of the operations. Returns the first operation.
		 */
		public RecOp chainOps(final RecOp... ops)
			{
			for(int i=0;i<ops.length-1;i++)
				ops[i].recurse=ops[i+1];
			return ops[0];
			}
		
		
		public boolean isRunning()
			{
			return !toStop || isAlive();
			}
		
		private AcqThread(EvMultidimAcquisition settings)
			{
			this.settings=settings;
			
			if(settings.times.trigger!=null)
				{
				HWTrigger triggerDevice=(HWTrigger)settings.times.trigger.getDevice();
				triggerDevice.addTriggerListener(this);
				}
			
			}

		
		
		@Override
		public void run()
			{
			//TODO need to choose camera, at least!
			Iterator<EvDevicePath> itcam=EvHardware.getDeviceMapCast(HWCamera.class).keySet().iterator();
			if(itcam.hasNext())
				cam=itcam.next();
			
			
			try
				{
				//Check that there are enough parameters
				if(cam!=null && container!=null)
					{

					
					//Iterator for all different orders!!!! there are 6. function composition possible?
					//Pass an iterator to an iterator to an iterator

					/**
					 * One iterator for each dimensional order
					 */
					RecOp preop[]=new RecOp[4];
					for(int i=0;i<3;i++)
						if(order.entrylist.get(i).id.equals(RecSettingsDimensionsOrder.ID_POSITION))
							preop[i]=new RecOpPos();
						else if(order.entrylist.get(i).id.equals(RecSettingsDimensionsOrder.ID_CHANNEL))
							preop[i]=new RecOpChannel();
						else if(order.entrylist.get(i).id.equals(RecSettingsDimensionsOrder.ID_SLICE))
							preop[i]=new RecOpStack();
					preop[3]=new RecOpSnap();

					/**
					 * -----time refers to----
					 * pos, chan, slice: one position
					 * pos, slice, chan: one position
					 * chan, pos, slice: all positions
					 * chan, slice, pos: all positions
					 * slice, chan, pos: all positions
					 * slice, pos, chan: all positions
					 */
					RecOp timeOp=new RecOpTime();
					RecOp ops[];
					if(order.entrylist.get(0).id.equals(RecSettingsDimensionsOrder.ID_POSITION))
						ops=new RecOp[]{preop[0],timeOp,preop[1],preop[2],preop[3]};
					else
						ops=new RecOp[]{timeOp, preop[0],preop[1],preop[2],preop[3]};
					
					
					
					
					/** ----Autofocus----
					 * 
					 * 
					 * 
					 * ------------
					 * 
					 * 
					 */

					
					
					/**
					 * Prepare object etc
					 */
					if(positions.positions.isEmpty())
					{
						Imageset imset=new Imageset();
						for(int i=0;;i++)
							if(container.getChild("im"+i)==null)
								{
								container.metaObject.put("im"+i, imset);
								container=imset;
								break;
								}
						
					}

					//TODO signal update on the object
					BasicWindow.updateWindows();

					
					
					
					/**
					 * Set up stack and run recording
					 */
					chainOps(ops);
					ops[0].exec();
					

					
					}
				else
					System.out.println("No camera no container");
				
				}
			catch (Exception e)
				{
				e.printStackTrace();
				}
			
			
			
			
			System.out.println("---------stop-----------");
			toStop=false;
			for(EvAcquisition.AcquisitionListener l:listeners)
				l.acquisitionEventStopped();
			}
		
		
		public void stopAcquisition()
			{
			semTrigger.release();
			toStop=true;
			}
		
		
		private void startAcquisition()
			{
			if(!isRunning())
				{
				toStop=false;
				start();
				}
			}


		public void triggered()
			{
			semTrigger.release();
			}


		
		}
	
	
	public void setStoreLocation(EvContainer con, String name)
		{
		container=con;
		containerStoreName=name;
		}
	
	
	
	/**
	 * Get acquisition thread that links to this data
	 */
	public EvAcquisition.AcquisitionThread startAcquisition()
		{
		AcqThread th=new AcqThread(this);
		th.startAcquisition();
		return th;
		}


	@Override
	public void buildMetamenu(JMenu menu, EvContainer parentObject)
		{
		}


	@Override
	public String getMetaTypeDesc()
		{
		return "Acquisition: Multidim";
		}


	@Override
	public void loadMetadata(Element e)
		{
		
		// TODO Auto-generated method stub
		
		}


	@Override
	public String saveMetadata(Element e)
		{
		/*
		Element eRate=new Element("rate");
		eRate.setAttribute("value",rate.toString());
		eRate.setAttribute("unit",rateUnit);
		e.addContent(eRate);
		
		Element eDur=new Element("duration");
		eDur.setAttribute("value",duration.toString());
		eDur.setAttribute("unit",durationUnit);
		e.addContent(eDur);
		*/
		return metaType;
		}

	
	@Override
	public EvObject cloneEvObject()
		{
		return cloneUsingSerialize();
		}

	
	/******************************************************************************************************
	 * Plugin declaration
	 *****************************************************************************************************/
	public static void initPlugin() {}
	static
		{
		EvData.supportedMetadataFormats.put(metaType,EvMultidimAcquisition.class);
		}

	
	
	
	
	
	
	
	
	}
