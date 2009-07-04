package endrov.imageWindow.tools;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.vecmath.Vector2d;

import endrov.consoleWindow.ConsoleWindow;
import endrov.ev.EvLog;
import endrov.imageWindow.ImageWindow;
import endrov.imageWindow.ImageWindowTool;
import endrov.imageset.EvChannel;
import endrov.imageset.EvImage;
import endrov.imageset.EvPixels;
import endrov.imageset.EvPixelsType;
import endrov.imageset.EvStack;
import endrov.util.EvDecimal;

/**
 * Get information about a pixel
 * @author Johan Henriksson
 */
public class ImageWindowToolPixelInfo implements ImageWindowTool
	{
	private final ImageWindow w;
	
	public ImageWindowToolPixelInfo(ImageWindow w)
		{
		this.w=w;
		}
	public JMenuItem getMenuItem()
		{
		JCheckBoxMenuItem mi=new JCheckBoxMenuItem("Pixel information");
		mi.setSelected(w.getTool()==this);
		final ImageWindowTool This=this;
		mi.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){w.setTool(This);}
		});
		return mi;
		}
	
	public void mouseDragged(MouseEvent e, int dx, int dy)
		{
		}
	
	public void mouseClicked(MouseEvent e)
		{
		EvChannel c=w.getSelectedChannel();
		if(c!=null)
			{

			
			EvDecimal frame=w.frameControl.getFrame();
			EvDecimal slice=w.frameControl.getModelZ();
			frame=c.closestFrame(frame);
			EvStack stack=c.imageLoader.get(frame);
			slice=stack.closestZ(slice);
			
			Vector2d mpos=w.transformS2W(new Vector2d(e.getX(),e.getY()));
			Vector2d ppos=stack.transformWorldImage(mpos);
			int px=(int)ppos.x;
			int py=(int)ppos.y;
			
			ConsoleWindow.openConsole();
			
			EvImage image=stack.get(slice);
			if(image!=null && px<stack.getWidth() && py<stack.getHeight() && px>=0 && py>=0)
				{
				EvPixels pix=image.getPixels().getReadOnly(EvPixelsType.DOUBLE);
				EvLog.printLog("Pixel ("+px+" "+py+") Intensity: "+pix.getArrayDouble()[pix.getPixelIndex(px, py)]);
				}
			else
				EvLog.printLog("Outside image");
				
			}
		}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseMoved(MouseEvent e, int dx, int dy) {}
	public void mouseExited(MouseEvent e) {}
	public void keyPressed(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}
	public void paintComponent(Graphics g) {}
	public void deselected() {}
	}

//TODO: for channel displacement. mark dirty imageset, save