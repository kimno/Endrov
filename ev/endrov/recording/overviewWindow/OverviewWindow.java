/***
 * Copyright (C) 2010 Johan Henriksson
 * This code is under the Endrov / BSD license. See www.endrov.net
 * for the full text and how to cite.
 */
package endrov.recording.overviewWindow;


import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.*;

import javax.swing.*;
import javax.vecmath.Vector2d;

import org.jdom.*;

import endrov.basicWindow.*;
import endrov.data.EvContainer;
import endrov.data.EvData;
import endrov.hardware.*;
import endrov.imageWindow.GeneralTool;
import endrov.imageWindow.ImageWindow;
import endrov.imageWindow.ImageWindowInterface;
import endrov.imageWindow.ImageWindowRenderer;
import endrov.imageWindow.ImageWindowRendererExtension;
import endrov.imageset.EvPixels;
import endrov.imageset.EvPixelsType;
import endrov.recording.CameraImage;
import endrov.recording.RecordingResource;
import endrov.recording.RecordingResource.PositionListListener;
import endrov.recording.ResolutionManager;
import endrov.recording.device.HWAutoFocus;
import endrov.recording.device.HWCamera;
import endrov.recording.liveWindow.LiveHistogramViewRanged;
import endrov.roi.GeneralToolROI;
import endrov.roi.ImageRendererROI;
import endrov.roi.ROI;
import endrov.roi.window.GeneralToolDragCreateROI;
import endrov.util.EvDecimal;
import endrov.util.EvSwingUtil;
import endrov.util.JImageButton;
import endrov.util.JImageToggleButton;
import endrov.util.Vector2i;

/**
 * 
 */
public class OverviewWindow extends BasicWindow implements ActionListener, ImageWindowInterface, PositionListListener
	{
	/******************************************************************************************************
	 *                               Static                                                               *
	 *****************************************************************************************************/
	static final long serialVersionUID=0;
	
	
	public static final ImageIcon iconAutoFocus=new ImageIcon(OverviewWindow.class.getResource("jhAutoFocus.png"));
	public static final ImageIcon iconCameraToROI=new ImageIcon(OverviewWindow.class.getResource("jhCameraToROI.png"));
	public static final ImageIcon iconGoToROI=new ImageIcon(OverviewWindow.class.getResource("jhGoToROI.png"));
	public static final ImageIcon iconRectROI=new ImageIcon(OverviewWindow.class.getResource("jhRect.png"));
	public static final ImageIcon iconSelectROI=new ImageIcon(OverviewWindow.class.getResource("jhSelect.png"));

	
	
	/******************************************************************************************************
	 *                               Instance                                                             *
	 *****************************************************************************************************/
	
	private EvPixels[] lastCameraImage=null;
	private Vector2d imageOffset=new Vector2d(0,0);

	private int imgWidth=0;
	private int imgHeight=0;
	
	private JComboBox cameraCombo;
	
	private JCheckBox tAutoRange=new JCheckBox("Auto", true);
	private JButton bSetFullRange=new JButton("Full");
	private LiveHistogramViewRanged histoView=new LiveHistogramViewRanged();
	private JCheckBox tUpdateView=new JCheckBox("Update", true);
	private JButton bSnap=new JButton("Snap");
	private EvHidableSidePaneBelow sidepanel;
	private JPanel pHisto=new JPanel(new BorderLayout());
	private JButton bResetView = new JButton("Reset");

	private JButton bAutoFocus=new JImageButton(iconAutoFocus, "Autofocus");
	private JButton bCameraToROI=new JImageButton(iconCameraToROI, "Adapt camera limits to ROI");	
	private JButton bGoToROI=new JImageButton(iconGoToROI, "Move stage to focus on ROI");
	
	
	/**
	 * Surface for the image
	 */
	private OverviewWindowImageView drawArea=new OverviewWindowImageView()
		{
		private static final long serialVersionUID = 1L;
		public int getUpper(){return histoView.upper;}
		public int getLower(){return histoView.lower;}
		@Override
		public Vector2d getOffset() {return imageOffset;}
		
		
		protected void paintComponent(java.awt.Graphics g)
			{
			super.paintComponent(g);
			}
		@Override
		public EvDevicePath getCameraPath()
			{
			return getCurrentCameraPath();
			}
		
		};
	

	private Vector<JToggleButton> toolButtons=new Vector<JToggleButton>();
	private JToggleButton bSelectROI=new JImageToggleButton(iconSelectROI, "Select ROI");

	
	public void setTool(GeneralTool tool)
		{
		//TODO?
		drawArea.currentTool=tool;
		}

	public void unsetTool()
		{
		drawArea.currentTool=null;
		
		//Make sure all tool buttons are unselected
		for(JToggleButton bb:toolButtons)
			{
			
			bb.setSelected(false);
			
			}
		}
	
	public OverviewWindow()
		{
		this(new Rectangle(800,600));
		}
	
	
	public OverviewWindow(Rectangle bounds)
		{
		toolButtons.addAll(Arrays.asList(/*bEllipseROI,bFreehandROI,bLineROI,bPointROI,bPolygonROI,bRectROI,*/bSelectROI));

		bSelectROI.setSelected(true);
		setTool(new GeneralToolROI(OverviewWindow.this));
		
		bSelectROI.addActionListener(new ActionListener()
			{public void actionPerformed(ActionEvent e)
				{
				if(((JToggleButton)e.getSource()).isSelected())
					{
					//ImageRendererROI renderer=getRendererClass(ImageRendererROI.class);
					setTool(new GeneralToolROI(OverviewWindow.this));
					
					//setTool(new GeneralToolDragCreateROI(CamWindow.this,rt.makeInstance(),renderer));
					}
				}});
		
		
		for(final ROI.ROIType rt:ROI.getTypes())
			{
			if(rt.canPlace() && !rt.isCompound())
				{
				JToggleButton miNewROIthis;
				
				//toolButtons.addAll(Arrays.asList(bEllipseROI,bFreehandROI,bLineROI,bPointROI,bPolygonROI,bRectROI,bSelectROI));

				
				if(rt.getIcon()==null)
					miNewROIthis=new JToggleButton(rt.name());
				else
					miNewROIthis=new JImageToggleButton(rt.getIcon(),rt.name());
				miNewROIthis.addActionListener(new ActionListener()
					{public void actionPerformed(ActionEvent e)
						{
						if(((JToggleButton)e.getSource()).isSelected())
							{
							ImageRendererROI renderer=getRendererClass(ImageRendererROI.class);
							setTool(new GeneralToolDragCreateROI(OverviewWindow.this,rt.makeInstance(),renderer));
							}
						}});
				
				//TODO would be best if it was sorted
				toolButtons.add(miNewROIthis);
				//BasicWindow.addMenuItemSorted(miNew, miNewROIthis);
				}
			}

		
		
		///////////////////////
		
		
		
		drawArea.setToolButtons(toolButtons.toArray(new JToggleButton[0]));
		
		
		
		for(ImageWindowRendererExtension e:ImageWindow.imageWindowRendererExtensions)
			e.newImageWindow(this);
		
		cameraCombo=new JComboBox(new Vector<EvDevicePath>(EvHardware.getDeviceMap(HWCamera.class).keySet()));
		
		bSnap.setToolTipText("Manually take a picture and update. Does not save image.");
		//tHistoView.setToolTipText("Show histogram controls");
		tAutoRange.setToolTipText("Automatically adjust visible range");
		bSetFullRange.setToolTipText("Set visible range of all of camera range");

		bCameraToROI.addActionListener(this);
		bSnap.addActionListener(this);
		//tHistoView.addActionListener(this);
		tAutoRange.addActionListener(this);
		bSetFullRange.addActionListener(this);
		histoView.addActionListener(this);
		bAutoFocus.addActionListener(this);
		bGoToROI.addActionListener(this);
		bResetView.addActionListener(this);
		
		//pHisto.setBorder(BorderFactory.createTitledBorder("Range adjustment"));
		pHisto.add(
				EvSwingUtil.layoutCompactVertical(tAutoRange, bSetFullRange),
				BorderLayout.WEST);
		pHisto.add(histoView, BorderLayout.CENTER);
		
		List<JComponent> blistleft=new LinkedList<JComponent>();
		blistleft.addAll(toolButtons);
		blistleft.add(bCameraToROI);
		blistleft.add(bGoToROI);
		blistleft.add(bAutoFocus);
		JComponent pLeft=EvSwingUtil.layoutACB(
				EvSwingUtil.layoutEvenVertical(
						blistleft.toArray(new JComponent[0])
						/*
						bSelectROI,	bEllipseROI, bFreehandROI, bLineROI, bPointROI, bPolygonROI, bRectROI,
						bCameraToROI,
						bGoToROI,
						bAutoFocus*/
						),
						null,
						null
				);
		
		
		JPanel pCenter=new JPanel(new BorderLayout());
		pCenter.add(EvSwingUtil.layoutCompactHorizontal(cameraCombo, bResetView, bSnap, tUpdateView)
				,BorderLayout.SOUTH);
		pCenter.add(drawArea,BorderLayout.CENTER);
		
		sidepanel=new EvHidableSidePaneBelow(pCenter, pHisto, true);
		sidepanel.addActionListener(this);
		
		setLayout(new BorderLayout());
		add(sidepanel,BorderLayout.CENTER);
		add(pLeft,BorderLayout.WEST);
		
		for(JToggleButton b:toolButtons)
			b.addActionListener(this);
		
		//Window overall things
		setTitleEvWindow("Overview");
		packEvWindow();
		setVisibleEvWindow(true);
		setBoundsEvWindow(bounds);
	
		//setResizable(false);
		
		RecordingResource.posListListeners.addWeakListener(this);
		
		}
	
	
		
	
	/**
	 * Find out how many bits the camera is
	 */
	public Integer getNumCameraBits()
		{
		return 8;
		}
	
	/**
	 * Handle GUI interaction
	 */
	public void actionPerformed(ActionEvent e) 
		{
		if(e.getSource()==bSnap)
			snapCamera();
		else if(e.getSource()==bResetView){
			resetView();
		}
		else if(e.getSource()==tAutoRange)
			{
			if(lastCameraImage!=null)
				histoView.calcAutoRange(lastCameraImage);
			histoView.repaint();
			drawArea.repaint();
			}
		else if(e.getSource()==bSetFullRange)
			{
			histoView.lower=0;
			histoView.upper=(int)Math.pow(2, getNumCameraBits())-1;
			drawArea.repaint();
			histoView.repaint();
			}
		else if(e.getSource()==bAutoFocus)
			autofocus();
		else if(e.getSource()==bGoToROI)
			moveStageFocusROI();
		else if(e.getSource()==bCameraToROI)
			showErrorDialog("Not implemented yet");
		else if(e.getSource()==histoView)
			{
			drawArea.repaint();
			}
		else if(e.getSource()==sidepanel)
			{
			Rectangle bounds=getBoundsEvWindow();
			int dh=pHisto.getBounds().height;
			if(!sidepanel.isPanelVisible())
				dh=-dh;
			setBoundsEvWindow(new Rectangle(
					bounds.x,bounds.y,
					(int)(bounds.getWidth()),
					(int)(bounds.getHeight()+dh)
					));
			}
		else
			for(JToggleButton b:toolButtons)
				if(e.getSource()==b)
					{
					
					//Make sure all other tool buttons are unselected
					for(JToggleButton bb:toolButtons)
						{
						if(bb!=b)
							{
							//bb.removeActionListener(this);
							bb.setSelected(false);
							//bb.addActionListener(this);
							}
						}
					
					}
		
		}
		
	private HWCamera getCurrentCamera()
		{
		EvDevicePath camname=(EvDevicePath)cameraCombo.getSelectedItem();
		if(camname!=null)
			return (HWCamera)EvHardware.getDevice(camname);
		else
			return null;
		}

	private EvDevicePath getCurrentCameraPath()
		{
		return (EvDevicePath)cameraCombo.getSelectedItem();
		}
	
//	/**
//	 * Take one picture from the camera	
//	 */
//	private void snapCamera()
//		{	
//		HWCamera cam=getCurrentCamera();	
//		
//		if(cam!=null){	
//			CameraImage cim=cam.snap();
//			lastCameraImage=cim.getPixels();
//			EvPixels newImage = lastCameraImage[0];
//			EvPixels oldImage = drawArea.overviewImage;		
//			
//			Vector2d newImgPos=new Vector2d(0,0);
//			Vector2d oldImgPos=new Vector2d(0,0);
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//			
//			
//			if(drawArea.overviewImage.getWidth() == 1 && drawArea.overviewImage.getHeight() == 1){
//				
//				drawArea.setWorldOffset(getStageX(), getStageY());			
//			}
//			
//			
//			
//			if(getStageX()>imageOffset.x){
//				oldImgPos.x = getStageX()-imageOffset.x;
//				imageOffset.x = getStageX();
//				imgWidth = oldImage.getWidth() + (int)oldImgPos.x;		
//				
//			}else{			
//				if(oldImage.getWidth() >= -getStageX() + newImage.getWidth()+imageOffset.x){
//					imgWidth = (int)(oldImage.getWidth());
//					System.out.println("A");
//				}
//				else{
//					imgWidth = (int)(-getStageX() + newImage.getWidth()+imageOffset.x);
//					System.out.println("B");
//				}
//			}
//			
//			if(getStageY()>imageOffset.y){
//				oldImgPos.y = getStageY()-imageOffset.y;
//				imageOffset.y= getStageY();
//				imgHeight = oldImage.getHeight() + (int)oldImgPos.y;			
//			}else{
//				if(oldImage.getHeight() >= -getStageY() + newImage.getHeight()+imageOffset.y){
//					imgHeight = (int)(oldImage.getHeight());
//					System.out.println("C");
//				}
//				else{
//					imgHeight = (int)(-getStageY() + newImage.getHeight()+imageOffset.y);
//					System.out.println("D");		
//				}
//			}
//			
//			newImgPos = new Vector2d(imageOffset.x-getStageX(), imageOffset.y-getStageY());
//			
//			
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//	
//			
//			
//			EvPixels done = new EvPixels(EvPixelsType.INT, imgWidth, imgHeight);
//			
//			int[] doneA= done.convertToInt(true).getArrayInt();
//			int[] oldA = oldImage.convertToInt(true).getArrayInt();
//			int[] newA = newImage.convertToInt(true).getArrayInt();
//			
//			System.out.println("old "+oldA.length +" new "+ newA.length +" done "+ doneA.length);
//			System.out.println("wide "+done.getWidth() +" high" + done.getHeight());
//			
//			for(int y=0; y<oldImage.getHeight();y++){
//				for(int x=0; x<oldImage.getWidth();x++){
//					doneA[(y+(int)oldImgPos.y)*done.getWidth() + (x+(int)oldImgPos.x)] 
//							= oldA[y*oldImage.getWidth() + x];
//				}
//			}
//			for(int y=0; y<newImage.getHeight();y++){
//				for(int x=0; x<newImage.getWidth();x++){
//					doneA[(y+(int)newImgPos.y)*done.getWidth() + (x+(int)newImgPos.x)]
//							= newA[y*newImage.getWidth() + x];
//				}
//			}
//			
//			
//			drawArea.overviewImage = done;
//
//		}
//
//		drawArea.repaint();
//	
//		//drawArea
//		
//		//stoppa i overviewimage
//		//kan behöva förstora bild. fråga: upplösning? offset?
//		
//		//kolla i förra snapcamera		
//		
//		}
	
	/**
	 * Take one picture from the camera	
	 */
	private void snapCamera()
		{	
		HWCamera cam=getCurrentCamera();	
		
		if(cam!=null){	
			CameraImage cim=cam.snap();
			lastCameraImage=cim.getPixels();
			EvPixels newImage = lastCameraImage[0];
			EvPixels oldImage = drawArea.overviewImage;		
			
			Vector2d newImgPos=new Vector2d(0,0);
			Vector2d oldImgPos=new Vector2d(0,0);
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			
					
				//ResolutionManager.Resolution res=ResolutionManager.getCurrentResolutionNotNull(campath);
				ResolutionManager.Resolution res = new ResolutionManager.Resolution(0.5, 0.5);
			
			
			if(-getStageX()*res.x >imageOffset.x){
				oldImgPos.x = -getStageX()*res.x-imageOffset.x;
				imageOffset.x = -getStageX()*res.x;
				imgWidth = oldImage.getWidth() + (int)oldImgPos.x;		
				
			}else{			
				if(oldImage.getWidth() >= getStageX()*res.x + newImage.getWidth()+imageOffset.x){
					imgWidth = (int)(oldImage.getWidth());
					System.out.println("A");
				}
				else{
					imgWidth = (int)(getStageX()*res.x + newImage.getWidth()+imageOffset.x);
					System.out.println("B");
				}
			}
			
			if(-getStageY()*res.y>imageOffset.y){
				oldImgPos.y = -getStageY()*res.y-imageOffset.y;
				imageOffset.y= -getStageY()*res.y;
				imgHeight = oldImage.getHeight() + (int)oldImgPos.y;			
			}else{
				if(oldImage.getHeight() >= getStageY()*res.y + newImage.getHeight()+imageOffset.y){
					imgHeight = (int)(oldImage.getHeight());
					System.out.println("C");
				}
				else{
					imgHeight = (int)(getStageY()*res.y + newImage.getHeight()+imageOffset.y);
					System.out.println("D");		
				}
			}
			
			newImgPos = new Vector2d(imageOffset.x+getStageX()*res.x, imageOffset.y+getStageY()*res.y);
			
			
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
			
			
			EvPixels done = new EvPixels(EvPixelsType.INT, imgWidth, imgHeight);
			
			int[] doneA= done.convertToInt(true).getArrayInt();
			int[] oldA = oldImage.convertToInt(true).getArrayInt();
			int[] newA = newImage.convertToInt(true).getArrayInt();
			
			System.out.println("old "+oldA.length +" new "+ newA.length +" done "+ doneA.length);
			System.out.println("wide "+done.getWidth() +" high" + done.getHeight());
			
			for(int y=0; y<oldImage.getHeight();y++){
				for(int x=0; x<oldImage.getWidth();x++){
					doneA[(y+(int)oldImgPos.y)*done.getWidth() + (x+(int)oldImgPos.x)] 
							= oldA[y*oldImage.getWidth() + x];
				}
			}
			for(int y=0; y<newImage.getHeight();y++){
				for(int x=0; x<newImage.getWidth();x++){
					doneA[(y+(int)newImgPos.y)*done.getWidth() + (x+(int)newImgPos.x)]
							= newA[y*newImage.getWidth() + x];
				}
			}
			
			
			drawArea.overviewImage = done;

		}

		drawArea.repaint();
	
		//drawArea
		
		//stoppa i overviewimage
		//kan behöva förstora bild. fråga: upplösning? offset?
		
		//kolla i förra snapcamera		
		
		}
	
	
//	public Vector2i getOffset()
//	{
//	double dx=getStageX()-lastImageStagePos.x;
//	double dy=getStageY()-lastImageStagePos.y;
//	return new Vector2i((int)(dx/getCameraResolution().x), (int)(dy/getCameraResolution().y));
//	}
		
	public void resetView(){
		lastCameraImage=null;
		imageOffset=new Vector2d(0,0);
		imgWidth=0;
		imgHeight=0;
		drawArea.resetCameraPos();
		drawArea.overviewImage = new EvPixels(EvPixelsType.INT,512,512);
		repaint();
	}
	
	
	public void dataChangedEvent()
		{
		}

	public void loadedFile(EvData data){}

	public void windowSavePersonalSettings(Element e)
		{
		} 
	public void freeResources()
		{
		resetView();
		}
	

	
	
	
	
	
	public void addImageWindowRenderer(ImageWindowRenderer renderer)
		{
		drawArea.imageWindowRenderers.add(renderer);
		}


	public EvDecimal getFrame()
		{
		return EvDecimal.ZERO;
		}

	public EvDecimal getZ()
		{
		return EvDecimal.ZERO; //Unclear what is the best. 3D rois?
		}

	@SuppressWarnings("unchecked")
	public <E> E getRendererClass(Class<E> cl)
		{
		for(ImageWindowRenderer r:drawArea.imageWindowRenderers)
			if(cl.isInstance(r))
				return (E)r;
		throw new RuntimeException("No such renderer exists - " + cl);
		}

	
	
	
	public EvContainer getRootObject()
		{
		return RecordingResource.getData();
		}

	public double getRotation()
		{
		//Never any rotation
		return 0;
		}

	
	/**
	 * [um/px]
	 */
	public ResolutionManager.Resolution getCameraResolution()
		{
		return ResolutionManager.getCurrentResolutionNotNull(getCurrentCameraPath());
		/*
		HWCamera cam=getCurrentCamera();
		if(cam!=null)
			return ResolutionManager.getCurrentTotalMagnification(cam);
		else
			return 1;
			*/
		}
	
	public double getStageX() // um
		{
		return RecordingResource.getCurrentStageX();
		}

	public double getStageY() // um
		{
		return RecordingResource.getCurrentStageY();
		}

	public double s2wz(double sz)
		{
		return sz;
		}

	public double scaleS2w(double s)
		{
		return s*getCameraResolution().x;
		}

	public double scaleW2s(double w)
		{
		return w/getCameraResolution().x;
		}

//	public Vector2d transformPointS2W(Vector2d v)
//		{
//		return new Vector2d(v.x*getCameraResolution().x-getStageX(), v.y*getCameraResolution().y-getStageY()); 
//		}
//
//	public Vector2d transformPointW2S(Vector2d v)
//		{
//		return new Vector2d((v.x+getStageX())/getCameraResolution().x, (v.y+getStageY())/getCameraResolution().y);
//		}
	
//	public Vector2d transformPointS2W(Vector2d v)
//		{
//		return new Vector2d((v.x*getCameraResolution().x-getStageX()-drawArea.getCameraPos().x+imageOffset.x)/drawArea.getScale(), 
//				(v.y*getCameraResolution().y-getStageY()-drawArea.getCameraPos().y+imageOffset.y)/drawArea.getScale()); 
//		}
	public Vector2d transformPointS2W(Vector2d v)
	{
	return new Vector2d((v.x*getCameraResolution().x/drawArea.getScale()-drawArea.getCameraPos().x/drawArea.getScale()-imageOffset.x), 
			(v.y*getCameraResolution().y/drawArea.getScale()-drawArea.getCameraPos().y/drawArea.getScale()-imageOffset.y)); 
	}
	
	public Vector2d transformPointW2S(Vector2d v)
		{
		return new Vector2d((v.x+imageOffset.x), (v.y+imageOffset.y));
		}

	public double w2sz(double z)
		{
		return z; //TODO
		}

	public String getCurrentChannelName()
		{
		return "";
		}

	public void updateImagePanel()
		{
		drawArea.repaint();
		}
	
	
	/**
	 * Autofocus, with whatever device there is
	 */
	public void autofocus()
		{
		HWAutoFocus af=RecordingResource.getOneAutofocus();
		if(af==null)
			showErrorDialog("No autofocus device found");
		else
			{
			try
				{
				af.fullFocus();
				}
			catch (IOException e)
				{
				e.printStackTrace();
				showErrorDialog("Failed to focus");
				}
			}
		
		
		}
	
	
	/**
	 * Move the stage such that one ROI is in focus
	 */
	public void moveStageFocusROI()
		{
		Set<ROI> rois=new HashSet<ROI>(ROI.getSelected());
		
		if(rois.size()!=1)
			showErrorDialog("Select 1 ROI first");
		else
			{
			ROI roi=rois.iterator().next();
			
			double x=roi.getPlacementHandle1().getX();
			double y=roi.getPlacementHandle2().getY();
			//Best would be to be able to get a bounding box
			
			Map<String, Double> pos=new HashMap<String, Double>();
			pos.put("x",x);
			pos.put("y",y);
			RecordingResource.setStagePos(pos);
			
			//TODO move to center. must take into account camera etc in that case 
			
			//TODO
			//Probably useful in a wider context - put in resource
			}
			
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
				JMenuItem mi=new JMenuItem("Overview",new ImageIcon(getClass().getResource("tangoCamera.png")));
				mi.addActionListener(this);
				BasicWindow.addMenuItemSorted(w.getCreateMenuWindowCategory("Recording"), mi);
				}

			public void actionPerformed(ActionEvent e) 
				{
				new OverviewWindow();
				}

			public void buildMenu(BasicWindow w){}
			}
			});
		
		}
	public void positionsUpdated() {
		repaint();
		
	}
	
	
	
	}
