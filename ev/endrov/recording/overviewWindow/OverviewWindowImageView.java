/***
 * Copyright (C) 2010 Johan Henriksson
 * This code is under the Endrov / BSD license. See www.endrov.net
 * for the full text and how to cite.
 */
package endrov.recording.overviewWindow;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.vecmath.Vector2d;

import endrov.basicWindow.EvColor;
import endrov.hardware.EvDevicePath;
import endrov.imageWindow.GeneralTool;
import endrov.imageWindow.ImageWindowRenderer;
import endrov.imageset.EvPixels;
import endrov.imageset.EvPixelsType;
import endrov.recording.CameraImage;
import endrov.recording.RecordingResource;
import endrov.recording.ResolutionManager;
import endrov.recording.device.HWCamera;
import endrov.recording.positionsWindow.Position;
import endrov.util.Vector2i;

/**
 * 
 */
public abstract class OverviewWindowImageView extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener
	{
	static final long serialVersionUID=0;
	
	private Vector2i lastMousePosition=new Vector2i();
	public GeneralTool currentTool=null;
	public final Vector<ImageWindowRenderer> imageWindowRenderers=new Vector<ImageWindowRenderer>();

	
	private Vector2d cameraPos=new Vector2d();
	
	public EvPixels overviewImage = new EvPixels(EvPixelsType.INT,512,512);
	private double scale;

	public Vector2d getCameraPos(){
		return cameraPos;
	}
	
	public void resetCameraPos(){
		cameraPos=new Vector2d();
	}
	
	public double getScale(){
		return scale;
	}
	
	public EvPixels[] getImage()
		{
		return new EvPixels[]{overviewImage};
		}

	public abstract Vector2d getOffset();

	public abstract int getLower();
	public abstract int getUpper();
		
	public abstract EvDevicePath getCameraPath();
		
	
	public JToggleButton[] toolButtons;
	
	/*
	public CamWindowImageView(JToggleButton[] toolButtons){
		this();
		this.toolButtons = toolButtons;
	}
*/	
	public OverviewWindowImageView()
		{
		//skall kolla upplösning?
		scale = 1.0;
		
		//overviewImage=new EvPixels();	
		
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		}
	
	
	public void setToolButtons(JToggleButton[] toolButtons)
		{
		this.toolButtons = toolButtons;
		}
	
	protected void paintComponent(Graphics g2)
		{
		
		Graphics2D g=(Graphics2D)g2;
				
		//Make sure background is filled with something
		//g.setColor(new Color(0.3f, 0.1f, 0.3f));
		g.setColor(new Color(0, 0, 0));
		g.fillRect(0, 0, getWidth(), getHeight());

		
		//övning åt er		
		Vector2d offset=cameraPos;
		g.translate((int)offset.x, (int)offset.y);
//		g.scale(sx, sy)
		g.scale(scale, scale);
		
		//Convert pixels into the right range. Mark under- and overflow
		EvPixels[] pq=getImage();
		if(pq!=null)
			{
			if(pq.length==1)
				{
				//Grayscale
			
				EvPixels p=pq[0];
				
				int lower=getLower();
				int upper=getUpper();
				int diff=upper-lower;
				if(diff==0)
					diff=1; //Just to avoid divison by zero errors
				int[] parr=p.convertToInt(true).getArrayInt();
				int w=p.getWidth();
				int h=p.getHeight();
				BufferedImage toDraw=new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
				int[] arrR=new int[parr.length];
				int[] arrG=new int[parr.length];
				int[] arrB=new int[parr.length];
				for(int i=0;i<parr.length;i++)
					{
					int v=parr[i];
					int out=(v-lower)*255/diff;
					if(out<0)
						{
						arrR[i]=0;
						arrG[i]=0;
						arrB[i]=255;
						}
					else if(out>255)
						{
						arrR[i]=255;
						arrG[i]=0;
						arrB[i]=0;
						}
					else
						{
						arrR[i]=out;
						arrG[i]=out;
						arrB[i]=out;
						}
					}
				WritableRaster raster=toDraw.getRaster();
				raster.setSamples(0, 0, w, h, 0, arrR);
				raster.setSamples(0, 0, w, h, 1, arrG);
				raster.setSamples(0, 0, w, h, 2, arrB);
				
				g.drawImage(toDraw, 0, 0, null);
				
				}
			else
				{
				//RGB
				
				int lower=getLower();
				int upper=getUpper();
				int diff=upper-lower;
				if(diff==0)
					diff=1; //Just to avoid divison by zero errors
				
				int[] parrR=pq[0].convertToInt(true).getArrayInt();
				int[] parrG=pq[1].convertToInt(true).getArrayInt();
				int[] parrB=pq[2].convertToInt(true).getArrayInt();
				int w=pq[0].getWidth();
				int h=pq[0].getHeight();
				BufferedImage toDraw=new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
				int[] arrR=new int[parrR.length];
				int[] arrG=new int[parrG.length];
				int[] arrB=new int[parrB.length];
				for(int i=0;i<parrR.length;i++)
					{
					int vR=parrR[i];
					int outR=(vR-lower)*255/diff;
					int vG=parrG[i];
					int outG=(vG-lower)*255/diff;
					int vB=parrB[i];
					int outB=(vB-lower)*255/diff;

					if(vR<0 || vG<0 || vB<0 || vR>255 || vG>255 || vB>255)
						{
						arrR[i]=255;
						arrG[i]=0;
						arrB[i]=0;
						}
					else
						{
						arrR[i]=outR;
						arrG[i]=outG;
						arrB[i]=outB;
						}
					
					}
				WritableRaster raster=toDraw.getRaster();
				raster.setSamples(0, 0, w, h, 0, arrR);
				raster.setSamples(0, 0, w, h, 1, arrG);
				raster.setSamples(0, 0, w, h, 2, arrB);
				
				g.drawImage(toDraw, 0, 0, null);
				
				}

			}
		
		
		for(ImageWindowRenderer r:imageWindowRenderers)
			r.draw(g);
		
		
		
		for(Position pos:RecordingResource.posList){

			g.setColor(pos.getColor().getAWTColor());

//			g.setColor(new Color(255,255,255));

			g.fillOval((int) (-pos.getX()+getOffset().x+256), (int) (-pos.getY()+getOffset().y+256), 5, 5);
			g.setFont(new Font("Arial", Font.PLAIN, 12));
			g.drawString(pos.toString(), (int) (-pos.getX()+getOffset().x+256), (int) (-pos.getY()+getOffset().y+256));
		}		
		g.translate(-offset.x, -offset.y);
		}
	
	
	
	public void mouseClicked(MouseEvent e)
		{
		lastMousePosition=new Vector2i(e.getX(),e.getY());
		EvDevicePath campath=getCameraPath();
		if(campath!=null)
			{
			ResolutionManager.Resolution res=ResolutionManager.getCurrentResolutionNotNull(campath);
			

			Map<String, Double> diff=new HashMap<String, Double>();		
	
			
			diff.put("x",-(lastMousePosition.x/scale-256-cameraPos.x/scale-getOffset().x));
			diff.put("y",-(lastMousePosition.y/scale-256-cameraPos.y/scale-getOffset().y));
			
			System.out.println("camera X "+ cameraPos.x + " Y "+ cameraPos.y + " scale "+scale);
			System.out.println("mousePos X " +lastMousePosition.x +" mousePos Y " + lastMousePosition.y);
			System.out.println("diff X "+ diff.get("x") + " diff Y " + diff.get("y") );
		
			RecordingResource.setStagePos(diff);
			repaint();
			//TODO update manual view
			
			
			}
		else
			System.out.println("No camera to move");
		
		
		
		if(currentTool!=null)
			currentTool.mouseClicked(e, this);
		}
	public void mouseEntered(MouseEvent e)
		{
		}
	public void mouseExited(MouseEvent e)
		{
		if(currentTool!=null)
			currentTool.mouseExited(e);
		}
	public void mousePressed(MouseEvent e)
		{
		lastMousePosition=new Vector2i(e.getX(),e.getY());
		if(currentTool!=null)
			currentTool.mousePressed(e);
		}
	public void mouseReleased(MouseEvent e)
		{
		if(currentTool!=null)
			currentTool.mouseReleased(e);
		}
	public void mouseDragged(MouseEvent e)
		{
//		for(JToggleButton b:toolButtons)
//			if(b.isSelected()) move = false;
		int dx=e.getX()-lastMousePosition.x;
		int dy=e.getY()-lastMousePosition.y;
		lastMousePosition=new Vector2i(e.getX(),e.getY());
	
		if(SwingUtilities.isLeftMouseButton(e))
			{
			//Left mouse button is for the tool
			if(currentTool!=null)
				currentTool.mouseDragged(e, dx, dy);

			}
		else if(SwingUtilities.isRightMouseButton(e))
			{
			
			
			cameraPos.x+=dx;
			cameraPos.y+=dy;
			
			
			repaint();
			
			/*
			EvDevicePath campath=getCameraPath();
			if(campath!=null)
				{
				ResolutionManager.Resolution res=ResolutionManager.getCurrentResolutionNotNull(campath);
				

				Map<String, Double> diff=new HashMap<String, Double>();
				diff.put("x",dx*res.x);
				diff.put("y",dy*res.y);
				RecordingResource.setRelStagePos(diff);
				repaint();
				//TODO update manual view
				
				}
			else
				System.out.println("No camera to move");
				*/
			}
		}
	public void mouseMoved(MouseEvent e)
		{
		int dx=e.getX()-lastMousePosition.x;
		int dy=e.getY()-lastMousePosition.y;
		lastMousePosition=new Vector2i(e.getX(),e.getY());
		
		if(currentTool!=null)
			currentTool.mouseMoved(e, dx, dy);
		
		}
	public void mouseWheelMoved(MouseWheelEvent e)
		{
		//övning!!!
		
		double dz=e.getWheelRotation();

		scale += (double)dz/10;
		if(scale<0.1){
			scale = 0.1;
		}
		
		/*
		//TODO magnification
		int dz=e.getWheelRotation();
		Map<String, Double> diff=new HashMap<String, Double>();
		diff.put("z",dz*10.0);
		RecordingResource.setRelStagePos(diff);
		*/
		repaint();
		}
	
	
	
	}
