package endrov.recording.resolutionConfigWindow;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import net.imglib2.exception.IncompatibleTypeException;

import org.jdom.Element;

import endrov.basicWindow.BasicWindow;
import endrov.basicWindow.BasicWindowExtension;
import endrov.basicWindow.BasicWindowHook;
import endrov.data.EvData;
import endrov.hardware.EvDevice;
import endrov.hardware.EvDevicePath;
import endrov.hardware.EvHardware;
import endrov.hardware.EvHardwareConfigGroup.State;
import endrov.imageset.EvPixels;
import endrov.recording.CameraImage;
import endrov.recording.RecordingResource;
import endrov.recording.ResolutionManager;
import endrov.recording.ResolutionManager.ResolutionState;
import endrov.recording.device.HWCamera;
import endrov.recording.widgets.RecWidgetComboDevice;
import endrov.recording.widgets.RecWidgetSelectProperties;
import endrov.util.EvSwingUtil;

/**
 * Configuring resolutions
 * 
 * @author Johan Henriksson
 *
 */
public class ResolutionConfigWindow extends BasicWindow implements ActionListener
	{
	private static final long serialVersionUID = 1L;

	private EvPixels[] lastCameraImage=null;
	
	private RecWidgetSelectProperties wProperties=new RecWidgetSelectProperties();
	private JList listCalibrations=new JList(new DefaultListModel());
	
	private JButton bDetect=new JButton("Detect");
	private JButton bEnter=new JButton("Enter manually");
	private JButton bDelete=new JButton("Delete");
	
	private RecWidgetComboDevice cCaptureDevice=new RecWidgetComboDevice()
		{
		private static final long serialVersionUID = 1L;
		protected boolean includeDevice(EvDevicePath path, EvDevice device)
			{
			return device instanceof HWCamera;
			}
		};
		
	private static class ListItem
		{
		public String name;
		public HWCamera cam;
		
		public ListItem(String name, HWCamera cam)
			{
			this.name = name;
			this.cam = cam;
			}

		public String toString()
			{
			return name;
			}
		}
	
	/**
	 * Create window
	 */
	public ResolutionConfigWindow()
		{
		setLayout(new BorderLayout());
		
		JScrollPane scrollList=new JScrollPane(listCalibrations, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		add(EvSwingUtil.withLabel("Capture device: ", cCaptureDevice), BorderLayout.NORTH);
		add(EvSwingUtil.layoutEvenVertical(wProperties,scrollList),
				BorderLayout.CENTER);
		add(EvSwingUtil.layoutEvenHorizontal(bDetect, bEnter, bDelete),
				BorderLayout.SOUTH);
		
		generateList();
		
		
		bDetect.addActionListener(this);
		bEnter.addActionListener(this);
		bDelete.addActionListener(this);
		
		//Window overall things
		setTitleEvWindow("Configure resolution");
		packEvWindow();
		setVisibleEvWindow(true);
		}


	private void generateList()
		{
		DefaultListModel model=(DefaultListModel)listCalibrations.getModel();
		model.removeAllElements();
		for(HWCamera cam:ResolutionManager.resolutions.keySet())
			for(String name:ResolutionManager.resolutions.get(cam).keySet())
				model.addElement(new ListItem(name, cam));
		}
	
	/**
	 * Handle button presses
	 */
	public void actionPerformed(ActionEvent e)
		{
		if(e.getSource()==bDelete)
			{
			ListItem item=(ListItem)listCalibrations.getSelectedValue();
			if(item!=null)
				{
				ResolutionManager.resolutions.get(item.cam).remove(item.name);
				generateList();
				}
			}
		
		
		EvDevicePath campath=cCaptureDevice.getSelectedDevice();
		if(campath==null)
			return;
		
		
		if(e.getSource()==bEnter)
			{
			
			try
				{
				//Get resolution
				String sResX=JOptionPane.showInputDialog("Resolution X [um/px]?");
				if(sResX==null)
					return;
				double resX=Double.parseDouble(sResX);
				String sResY=JOptionPane.showInputDialog("Resolution Y [um/px]?");
				if(sResY==null)
					return;
				double resY=Double.parseDouble(sResY);
				
				//Find all names in use
				Set<String> usedNames=new HashSet<String>();
				for(HWCamera cam2:ResolutionManager.resolutions.keySet())
					usedNames.addAll(ResolutionManager.resolutions.get(cam2).keySet());
				
				//Generate an unused name
				String name;
				int resi=0;
				do
					{
					name=campath.getLeafName()+" "+resi;
					resi++;
					} while(usedNames.contains(name));

				name=JOptionPane.showInputDialog("Name of resolution?",name);
				if(name==null)
					return;
				
				
				
				//Create the resolution state
				ResolutionState rstate=new ResolutionState();
				rstate.cameraRes=new ResolutionManager.Resolution(resX, resY);
				rstate.state=State.recordCurrent(wProperties.getSelectedProperties());
				ResolutionManager.getCreateResolutionStatesMap(campath).put(name, rstate);
				
				generateList();
				}
			catch (NumberFormatException e1)
				{
				BasicWindow.showErrorDialog("Invalid number");
				return;
				}
				
			
			}
		else if(e.getSource()==bDetect)
			{
			//BasicWindow.showErrorDialog("Not implemented yet");
			
			HWCamera cam=getCurrentCamera();	
			
			if(cam!=null){	
				CameraImage cim=cam.snap();
				lastCameraImage=cim.getPixels();
				EvPixels imageA = lastCameraImage[0];
				
				int cameraDisplacment = 50;
				
				double newX = RecordingResource.getCurrentStageX()-cameraDisplacment;
				double newY = RecordingResource.getCurrentStageY()-cameraDisplacment;
				
				Map<String, Double> pos=new HashMap<String, Double>();					
				pos.put("X",newX);
				pos.put("Y",newY);
				RecordingResource.setStagePos(pos);
				
//				while(RecordingResource.getCurrentStageX() != newX && 
//						RecordingResource.getCurrentStageY() != newY){
//					RecordingResource.setStagePos(pos);
//					
//				}
				
				cim=cam.snap();
				lastCameraImage=cim.getPixels();
				EvPixels imageB = lastCameraImage[0];
				
				
				double[] corrV = new double[2];
				try {
					corrV = ImageDisplacementCorrelation.displacement(imageA, imageB);
				} catch (IncompatibleTypeException e1) {
					// TODO Auto-generated catch block
					System.out.println("Invalid picture");
				}

				//[um/px]
				double resX, resY;
				resX = cameraDisplacment/corrV[0];
				resY = cameraDisplacment/corrV[1];
				
				String name = "Detected Resolution";
				//Create the resolution state
				ResolutionState rstate=new ResolutionState();
				rstate.cameraRes=new ResolutionManager.Resolution(resX, resY);
				rstate.state=State.recordCurrent(wProperties.getSelectedProperties());
				ResolutionManager.getCreateResolutionStatesMap(campath).put(name, rstate);
				generateList();
				System.out.println(resX + " " + resY);
				
			
			
			}
			
			}
			
		/*
		if(e.getSource()==bOk)
			{
			String name=tName.getText();
			if(!name.equals(""))
				{
				EvHardwareConfigGroup group=new EvHardwareConfigGroup();
				group.propsToInclude.addAll(wProperties.getSelectedProperties());
				
				EvHardwareConfigGroup.putConfigGroup(name, group);
				
				dispose();
				}
			else
				BasicWindow.showErrorDialog("No name specified for the group");			
			
			}
		else if(e.getSource()==bCancel)
			{
			dispose();
			}*/
		
		}
		
	
	private HWCamera getCurrentCamera()
	{
		EvDevicePath camname=(EvDevicePath) cCaptureDevice.getSelectedDevice();
		if(camname!=null)
			return (HWCamera)EvHardware.getDevice(camname);
		else
			return null;
	}
	

	@Override
	public void dataChangedEvent()
		{
		//cCaptureDevice.updateOptions();
		}


	@Override
	public void windowSavePersonalSettings(Element root)
		{
		}


	@Override
	public void loadedFile(EvData data)
		{
		}


	@Override
	public void freeResources()
		{
		}
	
	
	/******************************************************************************************************
	 * Plugin declaration
	 *****************************************************************************************************/
	public static void initPlugin() {}
	static
		{
		BasicWindow.addBasicWindowExtension(new BasicWindowExtension()
			{
			public void newBasicWindow(BasicWindow w)
				{
				w.basicWindowExtensionHook.put(this.getClass(),new Hook());
				}
			class Hook implements BasicWindowHook, ActionListener
				{
				public void createMenus(BasicWindow w)
					{
					JMenuItem mi=new JMenuItem("Configure resolution",new ImageIcon(getClass().getResource("jhResolutionConfigWindow.png")));
					mi.addActionListener(this);
					BasicWindow.addMenuItemSorted(w.getCreateMenuWindowCategory("Recording"), mi);
					}
	
				public void actionPerformed(ActionEvent e) 
					{
					new ResolutionConfigWindow();
					}
	
				public void buildMenu(BasicWindow w){}
				}
			});
		
		}
	
	
	

	}





//package endrov.recording.resolutionConfigWindow;
//
//import java.awt.BorderLayout;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//
//import javax.swing.DefaultListModel;
//import javax.swing.ImageIcon;
//import javax.swing.JButton;
//import javax.swing.JList;
//import javax.swing.JMenuItem;
//import javax.swing.JOptionPane;
//import javax.swing.JScrollPane;
//
//import net.imglib2.exception.IncompatibleTypeException;
//
//import org.jdom.Element;
//
//import endrov.basicWindow.BasicWindow;
//import endrov.basicWindow.BasicWindowExtension;
//import endrov.basicWindow.BasicWindowHook;
//import endrov.data.EvData;
//import endrov.hardware.EvDevice;
//import endrov.hardware.EvDevicePath;
//import endrov.hardware.EvHardware;
//import endrov.hardware.EvHardwareConfigGroup.State;
//import endrov.imageset.EvPixels;
//import endrov.recording.CameraImage;
//import endrov.recording.RecordingResource;
//import endrov.recording.ResolutionManager;
//import endrov.recording.ResolutionManager.ResolutionState;
//import endrov.recording.device.HWCamera;
//import endrov.recording.widgets.RecWidgetComboDevice;
//import endrov.recording.widgets.RecWidgetSelectProperties;
//import endrov.util.EvSwingUtil;
//
///**
// * Configuring resolutions
// * 
// * @author Johan Henriksson
// *
// */
//public class ResolutionConfigWindow extends BasicWindow implements ActionListener
//	{
//	private static final long serialVersionUID = 1L;
//
//	private EvPixels[] lastCameraImage=null;
//	
//	private RecWidgetSelectProperties wProperties=new RecWidgetSelectProperties();
//	private JList listCalibrations=new JList(new DefaultListModel());
//	
//	private JButton bDetect=new JButton("Detect");
//	private JButton bEnter=new JButton("Enter manually");
//	private JButton bDelete=new JButton("Delete");
//	
//	private RecWidgetComboDevice cCaptureDevice=new RecWidgetComboDevice()
//		{
//		private static final long serialVersionUID = 1L;
//		protected boolean includeDevice(EvDevicePath path, EvDevice device)
//			{
//			return device instanceof HWCamera;
//			}
//		};
//		
//	private static class ListItem
//		{
//		public String name;
//		public EvDevicePath campath;
//		
//		public ListItem(String name, EvDevicePath campath)
//			{
//			this.name = name;
//			this.campath = campath;
//			}
//
//		public String toString()
//			{
//			return name;
//			}
//		}
//	
//	/**
//	 * Create window
//	 */
//	public ResolutionConfigWindow()
//		{
//		setLayout(new BorderLayout());
//		
//		JScrollPane scrollList=new JScrollPane(listCalibrations, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//		
//		add(EvSwingUtil.withLabel("Capture device: ", cCaptureDevice), BorderLayout.NORTH);
//		add(EvSwingUtil.layoutEvenVertical(wProperties,scrollList),
//				BorderLayout.CENTER);
//		add(EvSwingUtil.layoutEvenHorizontal(bDetect, bEnter, bDelete),
//				BorderLayout.SOUTH);
//		
//		generateList();
//		
//		
//		bDetect.addActionListener(this);
//		bEnter.addActionListener(this);
//		bDelete.addActionListener(this);
//		
//		//Window overall things
//		setTitleEvWindow("Configure resolution");
//		packEvWindow();
//		setVisibleEvWindow(true);
//		}
//
//
//	private void generateList()
//		{
//		DefaultListModel model=(DefaultListModel)listCalibrations.getModel();
//		model.removeAllElements();
//		for(EvDevicePath campath:ResolutionManager.resolutions.keySet())
//			for(String name:ResolutionManager.resolutions.get(campath).keySet())
//				model.addElement(new ListItem(name, campath));
//		}
//	
//	/**
//	 * Handle button presses
//	 */
//	public void actionPerformed(ActionEvent e)
//		{
//		if(e.getSource()==bDelete)
//			{
//			ListItem item=(ListItem)listCalibrations.getSelectedValue();
//			if(item!=null)
//				{
//				ResolutionManager.resolutions.get(item.campath).remove(item.name);
//				generateList();
//				}
//			}
//		
//		
//		EvDevicePath campath=cCaptureDevice.getSelectedDevice();
//		if(campath==null)
//			return;
//		
//		
//		if(e.getSource()==bEnter)
//			{
//			
//			try
//				{
//				//Get resolution
//				String sResX=JOptionPane.showInputDialog("Resolution X [um/px]?");
//				if(sResX==null)
//					return;
//				double resX=Double.parseDouble(sResX);
//				String sResY=JOptionPane.showInputDialog("Resolution Y [um/px]?");
//				if(sResY==null)
//					return;
//				double resY=Double.parseDouble(sResY);
//				
//				//Find all names in use
//				Set<String> usedNames=new HashSet<String>();
//				for(EvDevicePath campath2:ResolutionManager.resolutions.keySet())
//					usedNames.addAll(ResolutionManager.resolutions.get(campath2).keySet());
//				
//				//Generate an unused name
//				String name;
//				int resi=0;
//				do
//					{
//					name=campath.getLeafName()+" "+resi;
//					resi++;
//					} while(usedNames.contains(name));
//
//				name=JOptionPane.showInputDialog("Name of resolution?",name);
//				if(name==null)
//					return;
//				
//				
//				
//				//Create the resolution state
//				ResolutionState rstate=new ResolutionState();
//				rstate.cameraRes=new ResolutionManager.Resolution(resX, resY);
//				rstate.state=State.recordCurrent(wProperties.getSelectedProperties());
//				ResolutionManager.getCreateResolutionStatesMap(campath).put(name, rstate);
//				
//				generateList();
//				}
//			catch (NumberFormatException e1)
//				{
//				BasicWindow.showErrorDialog("Invalid number");
//				return;
//				}
//				
//			
//			}
//		else if(e.getSource()==bDetect)
//			{
//			//BasicWindow.showErrorDialog("Not implemented yet");
//			
//			HWCamera cam=getCurrentCamera();	
//			
//			if(cam!=null){	
//				CameraImage cim=cam.snap();
//				lastCameraImage=cim.getPixels();
//				EvPixels imageA = lastCameraImage[0];
//				
//				double newX = RecordingResource.getCurrentStageX()-50;
//				double newY = RecordingResource.getCurrentStageY()-50;
//				
//				Map<String, Double> pos=new HashMap<String, Double>();					
//				pos.put("X",newX);
//				pos.put("Y",newY);
//				RecordingResource.setStagePos(pos);
//				
////				while(RecordingResource.getCurrentStageX() != newX && 
////						RecordingResource.getCurrentStageY() != newY){
////					RecordingResource.setStagePos(pos);
////					
////				}
//				
//				cim=cam.snap();
//				lastCameraImage=cim.getPixels();
//				EvPixels imageB = lastCameraImage[0];
//				
//				
//				double[] corrV = new double[2];
//				try {
//					corrV = ImageDisplacementCorrelation.displacement(imageA, imageB);
//				} catch (IncompatibleTypeException e1) {
//					// TODO Auto-generated catch block
//					System.out.println("Invalid picture");
//				}
//				
//				System.out.println(corrV[0] + " " + corrV[1]);
//				
//				//[um/px]
//				double resX, resY;
//				resX = 50/corrV[0];
//				resY = 50/corrV[1];
//				
//				String name = "Detected Resolution";
//				//Create the resolution state
//				ResolutionState rstate=new ResolutionState();
//				rstate.cameraRes=new ResolutionManager.Resolution(resX, resY);
//				rstate.state=State.recordCurrent(wProperties.getSelectedProperties());
//				ResolutionManager.getCreateResolutionStatesMap(campath).put(name, rstate);
//				
//				generateList();
//				
//				System.out.println(resX + " " + resY);
//				
//			
//			
//			}
//			
//			}
//			
//		/*
//		if(e.getSource()==bOk)
//			{
//			String name=tName.getText();
//			if(!name.equals(""))
//				{
//				EvHardwareConfigGroup group=new EvHardwareConfigGroup();
//				group.propsToInclude.addAll(wProperties.getSelectedProperties());
//				
//				EvHardwareConfigGroup.putConfigGroup(name, group);
//				
//				dispose();
//				}
//			else
//				BasicWindow.showErrorDialog("No name specified for the group");			
//			
//			}
//		else if(e.getSource()==bCancel)
//			{
//			dispose();
//			}*/
//		
//		}
//		
//	
//	private HWCamera getCurrentCamera()
//	{
//		EvDevicePath camname=(EvDevicePath) cCaptureDevice.getSelectedDevice();
//		if(camname!=null)
//			return (HWCamera)EvHardware.getDevice(camname);
//		else
//			return null;
//	}
//	
//
//	@Override
//	public void dataChangedEvent()
//		{
//		//cCaptureDevice.updateOptions();
//		}
//
//
//	@Override
//	public void windowSavePersonalSettings(Element root)
//		{
//		}
//
//
//	@Override
//	public void loadedFile(EvData data)
//		{
//		}
//
//
//	@Override
//	public void freeResources()
//		{
//		}
//	
//	
//	/******************************************************************************************************
//	 * Plugin declaration
//	 *****************************************************************************************************/
//	public static void initPlugin() {}
//	static
//		{
//		BasicWindow.addBasicWindowExtension(new BasicWindowExtension()
//			{
//			public void newBasicWindow(BasicWindow w)
//				{
//				w.basicWindowExtensionHook.put(this.getClass(),new Hook());
//				}
//			class Hook implements BasicWindowHook, ActionListener
//				{
//				public void createMenus(BasicWindow w)
//					{
//					JMenuItem mi=new JMenuItem("Configure resolution",new ImageIcon(getClass().getResource("jhResolutionConfigWindow.png")));
//					mi.addActionListener(this);
//					BasicWindow.addMenuItemSorted(w.getCreateMenuWindowCategory("Recording"), mi);
//					}
//	
//				public void actionPerformed(ActionEvent e) 
//					{
//					new ResolutionConfigWindow();
//					}
//	
//				public void buildMenu(BasicWindow w){}
//				}
//			});
//		
//		}
//	
//	
//	
//
//	}
