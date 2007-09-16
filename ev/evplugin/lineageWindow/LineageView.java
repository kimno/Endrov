package evplugin.lineageWindow;

import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

import evplugin.basicWindow.BasicWindow;
import evplugin.nuc.*;

//TODO: kill internal which are not in use, especially roots

/**
 * The lineage view is so specific to the view that there is no point in separating it.
 * @author Johan Henriksson
 */
public class LineageView extends JPanel
	{
	static final long serialVersionUID=0;

	public int camB, camC;
	private int frameDist=5;
	public double currentFrame=0;
	public boolean displayHorizontalTree=true;
	public NucLineage lin=null;
	
	public boolean showFrameLines=true;
	public boolean showKeyFrames=true;
	
	/** Cached information about nuclei */
	TreeMap<String, Internal> nucInternal=new TreeMap<String, Internal>();
	
	/** Size of expander icon */
	private static int expanderSize=4;
	
	
	
	

	/**
	 * Go to the first frame at which there is a nucleus
	 */
	public void goRoot()
		{	
		Integer allMinFrame=null;
		
		Vector<String> roots=getRootNuc();
		for(String nucName:roots)
			{
			NucLineage.Nuc nuc=lin.nuc.get(nucName);
			if(allMinFrame==null || nuc.pos.firstKey()<allMinFrame)
				allMinFrame=nuc.pos.firstKey();
			}
		if(allMinFrame!=null)
			{
			goFrame(allMinFrame);
			/*
			camC=(allMinFrame-5)*frameDist;
			camB=0;
			repaint();
			*/
			}
		}

	/**
	 * Go to one selected nucleus
	 */
	public void goSelected()
		{
		if(!NucLineage.selectedNuclei.isEmpty())
			{
			String nucName=NucLineage.selectedNuclei.iterator().next();
			Internal internal=getNucinfo(nucName);
			if(displayHorizontalTree)
				{
				camB+=internal.lastB-getHeight()/2;
				camC+=internal.lastC-getWidth()/2;
				}
			else
				{
				camB+=internal.lastB-getWidth()/2;
				camC+=internal.lastC-getWidth()/2;
				}
			repaint();
			}
		}
	
	/**
	 * Move according to mouse movement
	 * @param dx Change in pixel x
	 * @param dy Change in pixel y
	 */
	public void pan(int dx, int dy)
		{
		if(displayHorizontalTree)
			{
			camC-=dx;
			camB-=dy;
			}
		else
			{
			camC-=dy;
			camB-=dx;
			}
		}
	

	/**
	 * Change the frame distance but keep the camera reasonably fixed
	 * @param s New frame distance, >=1
	 */
	public void setFrameDist(int s)
		{
		if(s<1)	s=1; //Not allowed to happen, quick fix
		
		int h;
		if(displayHorizontalTree)
			h=getWidth()/2;
		else
			h=getHeight()/2;
		double curmid=(camC+h)/frameDist;
		frameDist=s;
		camC=(int)(curmid*frameDist-h);
		}
	
	
	/**
	 * Move camera to show some frame
	 */
	public void goFrame(int frame)
		{
		camC=frame*frameDist;
		if(displayHorizontalTree)
			camC-=getWidth()/2;
		else
			camC-=getHeight()/2;
		repaint();
		}
	
	/**
	 * Get all root nuclei
	 */
	private Vector<String> getRootNuc()
		{
		Vector<String> list=new Vector<String>();
		if(lin!=null)
			for(String nucName:lin.nuc.keySet())
				if(lin.nuc.get(nucName).parent==null)
					list.add(nucName);
		return list;
		}
	
	
	public void foldAll()
		{
		Vector<String> roots=getRootNuc();
		for(String nucName:roots)
		recursiveExpand(nucName, false);
		repaint();
		}

	public void unfoldAll()
		{
		Vector<String> roots=getRootNuc();
		for(String nucName:roots)
		recursiveExpand(nucName, true);
		repaint();
		}

	
	/**
	 * Draw the component
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	public void paintComponent(Graphics g)
		{
		//Redo list of clickable regions
		regionClickList.clear();
		
		//Fill background
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());

		//Draw frame lines
		drawFrameLines(g);

		//Clean up
		removeUnusedInternal();

		//Update tree structure
		Vector<String> roots=getRootNuc();
		for(String nucName:roots)
			updateTreeFormat(g,nucName);
		
		//Draw all trees
		int displacement=0;
		boolean first=true;
		for(String nucName:roots)
			{
			Internal nuc=getNucinfo(nucName);
			if(first)	first=false; else displacement+=nuc.sizer/2; //I don't like this layout really
			//updateTreeFormat(g,nucName);
			if(displayHorizontalTree)
				drawTree(g, nucName,displacement+getHeight()/2-camB); 
			else
				drawTree(g, nucName,displacement+getWidth()/2-camB); 
			displacement+=nuc.sizer/2; //maybe really half this and half next?
			}
		}
	
	/**
	 * Remove unused Internal nodes
	 */
	public void removeUnusedInternal()
		{
		//TODO. clean-up not really needed.
		}
	
	/**
	 * Draw the frame lines in the background
	 * @param g Graphics context
	 */
	public void drawFrameLines(Graphics g)
		{
		if(showFrameLines)
			{
			Color frameLineColor=new Color(220,220,220);
			Color frameStringColor=new Color(100,100,100);
			int frameLineSkip=20/frameDist;
			if(frameLineSkip<1)
				frameLineSkip=1;
			int starti=camC/frameDist;
			while(starti%frameLineSkip!=0)
				starti--;
			if(displayHorizontalTree)
				{
				Graphics2D g2=(Graphics2D)g;
				for(int i=starti;i<getWidth()/frameDist+1+frameLineSkip+camC/frameDist;i+=frameLineSkip)
					{
					int x=i*frameDist-camC;
					g.setColor(frameLineColor);
					g.drawLine(x, 0, x, getHeight());
					g.setColor(frameStringColor);
					g2.translate(x, 5);
					g2.rotate(Math.PI/2);
					g.drawString(""+i, 0, 0);
					g2.rotate(-Math.PI/2);
					g2.translate(-x, -5);
					}
				}
			else
				{
				for(int i=starti;i<getHeight()/frameDist+1+frameLineSkip+camC/frameDist;i+=frameLineSkip)
					{
					int y=i*frameDist-camC;
					g.setColor(frameLineColor);
					g.drawLine(0, y, getWidth(), y);
					g.setColor(frameStringColor);
					g.drawString(""+i, 5, y); 
					}		
				}
			}
		}
	
	
	
	
	/**
	 * Recursive function to draw a tree
	 * @param g Graphics context
	 * @param internal Which node to recurse from
	 */
	private void drawTree(Graphics g, String nucName, int midr)
		{
		NucLineage.Nuc nuc=lin.nuc.get(nucName);
		if(nuc==null)
			{
			//This is a hack in my opinion. Better if it can be found during tree structure planner
			//but this is more flexible right now. will give some artifacts
			System.out.println(nucName+" not found while drawing. bug!!!?");
			nucInternal.remove(nucName);
			return;
			}
		
		
		Internal internal=getNucinfo(nucName);
		int firstFrame=nuc.pos.firstKey();
		int startc=f2c(firstFrame);
		int endc=f2c(nuc.lastFrame());
		
		//Draw line spanning frames
		g.setColor(Color.BLACK);
		if(displayHorizontalTree)
			g.drawLine(startc, midr, endc, midr);
		else
			g.drawLine(midr, startc, midr, endc);
		if(nuc.end!=null && nuc.child.size()==0)
			drawNucEnd(g, f2c(nuc.end), midr);
		internal.lastC=startc;
		internal.lastB=midr;
		
		//Draw keyframes
		final int keyFrameSize=2;
		if(showKeyFrames && midr>-keyFrameSize && midr<getMaxVisibleR()+keyFrameSize)
			{
			g.setColor(Color.RED);
			for(int frame:nuc.pos.keySet())
				{
				int y=f2c(frame);
				if(displayHorizontalTree)
					g.drawOval(y-keyFrameSize, midr-keyFrameSize, 2*keyFrameSize, 2*keyFrameSize);
				else
					g.drawOval(midr-keyFrameSize, y-keyFrameSize, 2*keyFrameSize, 2*keyFrameSize);
				}
			}
		
		//Draw children
		if(internal.expanded)
			{
			for(String cName:nuc.child)
				{
				NucLineage.Nuc c=lin.nuc.get(cName);
				Internal cInternal=nucInternal.get(cName);
				//Draw connecting line
				g.setColor(Color.BLACK);
				if(displayHorizontalTree)
					g.drawLine(endc,midr,f2c(c.pos.firstKey()),midr+cInternal.centerDisplacement);
				else
					g.drawLine(midr,endc,midr+cInternal.centerDisplacement,f2c(c.pos.firstKey()));
				//Recurse down
				drawTree(g,cName, midr+cInternal.centerDisplacement);
				}
			}
		
		//Draw expander
		if(nuc.child.size()>0)
			{
			if(displayHorizontalTree)
				drawExpanderSymbol(g,nucName, endc,midr,internal.expanded);
			else
				drawExpanderSymbol(g,nucName, midr,endc,internal.expanded);
			}

		//Draw name of nucleus. Warn if something is wrong
		if(nuc.end!=null && nuc.child.size()>0)
			drawNucName(g, "!!! "+nucName, midr, endc);
		else
			drawNucName(g, nucName, midr, endc);
		}

	/**
	 * Get maximum visible c-value
	 */
	public int getMaxVisibleC()
		{
		if(displayHorizontalTree)
			return getWidth();
		else
			return getHeight();
		}

	
	/**
	 * Get maximum visible r-value
	 */
	public int getMaxVisibleR()
		{
		if(displayHorizontalTree)
			return getHeight();
		else
			return getWidth();
		}
	
	/**
	 * Draw arrow pointing out that the nucleus continue existing
	 */
	private void drawNucEnd(Graphics g, int endc, int midr)
		{
		int size=10;
		g.setColor(Color.BLUE);
		if(displayHorizontalTree)
			g.drawLine(endc, midr-size, endc, midr+size);
		else
			g.drawLine(midr-size, endc, midr+size, endc); //not tested yet
		}
	
	
	/**
	 * Convert frame position to coordinate
	 */
	private int f2c(int f)
		{
		return f*frameDist-camC;
		}
	
	
	
	/**
	 * Draw text name
	 */
	private void drawNucName(Graphics g, String nucName, int midr, int endc)
		{
		int fontHeight=g.getFontMetrics().getHeight();
		int fontWidth=g.getFontMetrics().stringWidth(nucName);
		int textc=endc+5;
		Graphics2D g2=(Graphics2D)g;
		if(NucLineage.selectedNuclei.contains(nucName))
			g2.setColor(Color.RED);
		else
			g2.setColor(Color.BLUE);
		if(displayHorizontalTree)
			{
			//Graphics
			int textr=midr+fontHeight/4;
			g2.translate(textc, textr);
			g2.drawString(nucName, 0, 0);
			g2.translate(-textc, -textr);
			//Make it clickable
			regionClickList.add(new ClickRegionName(nucName, textc, textr-3*fontHeight/4, fontWidth,fontHeight));
			//g.drawRect(textc, textr-3*fontHeight/4, fontWidth,fontHeight);
			}
		else
			{
			//Graphics
			int textr=midr-fontHeight/4;
			g2.translate(textr, textc);
			g2.rotate(Math.PI/2);
			g2.drawString(nucName, 0, 0);
			g2.rotate(-Math.PI/2);
			g2.translate(-textr, -textc);
			//Make it clickable
			regionClickList.add(new ClickRegionName(nucName, textr-fontHeight/4, textc, fontHeight,fontWidth)); 
			//g.drawRect(textr-fontHeight/4, textc, fontHeight,fontWidth);
			}
		}
	
	/**
	 * Draw the [+] and [-] symbol
	 * @param g Graphics context
	 * @param nucname Name of corresponding nucleus
	 * @param x Mid x coordinate
	 * @param y Mid y coordinate
	 * @param expanded If a + or - should be shown
	 */
	private void drawExpanderSymbol(Graphics g, String nucname, int x, int y, boolean expanded)
		{
		//Do graphics
		g.setColor(Color.WHITE);
		g.fillRect(x-expanderSize, y-expanderSize, 2*expanderSize, 2*expanderSize);
		g.setColor(Color.BLACK);
		g.drawRect(x-expanderSize, y-expanderSize, 2*expanderSize, 2*expanderSize);		
		g.drawLine(x-expanderSize, y, x+expanderSize,y);
		if(expanded)
			g.drawLine(x, y+expanderSize, x,y-expanderSize);
		//Make it clickable
		regionClickList.add(new ClickRegionExpander(nucname, x-expanderSize, y-expanderSize, 2*expanderSize,2*expanderSize));

		}
	

	
	/**
	 * Prepare rendering sizes
	 */
	private void updateTreeFormat(Graphics g, String nucName)
		{		
		Internal internal=getNucinfo(nucName);
		NucLineage.Nuc nuc=lin.nuc.get(nucName);

		//Total width of children. 0 if none expanded
		int totw=0;
		
		//Only recurse if children are visible
		if(internal.expanded)
			{
			//Sum up total width for children
			for(String cName:nuc.child)
				{
				Internal cInternal=getNucinfo(cName);
				updateTreeFormat(g,cName);
				totw+=cInternal.sizer;
				}
			//Set displacements
			if(nuc.child.size()==1)
				{
				Internal cInternal=getNucinfo(nuc.child.first());
				cInternal.centerDisplacement=10;
				}
			else if(nuc.child.size()==2 && false) //why did I set this to false?
				{
				//Divide evenly. this does not work properly with multitrees
				Internal cInternal1=getNucinfo(nuc.child.first());
				Internal cInternal2=getNucinfo(nuc.child.last());
				cInternal1.centerDisplacement=-totw/2;
				cInternal2.centerDisplacement=+totw/2;
				}
			else
				{
				int fromleft=0;
				for(String cName:nuc.child)
					{
					Internal cInternal=nucInternal.get(cName);
					cInternal.centerDisplacement=fromleft+cInternal.sizer/2-totw/2;
					fromleft+=cInternal.sizer;
					}
				}
			}
		
		//Compute width for this node
		internal.sizer=totw;
		int fontHeight=g.getFontMetrics().getHeight()*2;
		if(internal.sizer<fontHeight)
			internal.sizer=fontHeight;
		}
	
	
	
	
	/**
	 * Cached information about nuclei
	 */
	public static class Internal
		{
		public boolean expanded=true;
		public int sizer=0;
		public int centerDisplacement=0;
		public int lastB, lastC;
		}

	
	/**
	 * Get information structure about nucleus
	 * @param nuc Name of nucleus
	 * @return Existing structure or a new one
	 */
	public Internal getNucinfo(String nuc)
		{
		Internal i=nucInternal.get(nuc);
		if(i==null)
			{
			i=new Internal();
			nucInternal.put(nuc, i);
			}
		return i;
		}
	
	
	/** List of all mouse click handlers */
	LinkedList<ClickRegion> regionClickList=new LinkedList<ClickRegion>();
	
	/**
	 * Handle mouse click in view
	 */
	public void clickRegion(MouseEvent e)
		{
		for(ClickRegion r:regionClickList)
			if(e.getX()>=r.x && e.getY()>=r.y && e.getX()<=r.x+r.w && e.getY()<=r.y+r.h)
				{
				r.clickRegion(e);
				return;
				}
		NucLineage.selectedNuclei.clear();
		BasicWindow.updateWindows();
		//repaint();
		}

	/**
	 * Mouse click handler
	 */
	private abstract class ClickRegion
		{
		public int x=0,y=0,w=0,h=0;
		public abstract void clickRegion(MouseEvent e);
		}

	/**
	 * Mouse click handler: on a name panel
	 */
	private class ClickRegionName extends ClickRegion
		{
		String nucname;
		public ClickRegionName(String nucname, int x, int y, int w, int h)
			{this.nucname=nucname; this.x=x; this.y=y; this.w=w; this.h=h;}
		public void clickRegion(MouseEvent e)
			{
			NucLineage.mouseSelectNuc(nucname, (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK)!=0);
			BasicWindow.updateWindows();
			}
		}
	
	
	/**
	 * Mouse click handler: on an expander
	 */
	private class ClickRegionExpander extends ClickRegion
		{
		String nucname;
		public ClickRegionExpander(String nucname, int x, int y, int w, int h)
			{this.nucname=nucname; this.x=x; this.y=y; this.w=w; this.h=h;}
		public void clickRegion(MouseEvent e)
			{
			Internal internal=getNucinfo(nucname);
			if(SwingUtilities.isLeftMouseButton(e))
				internal.expanded=!internal.expanded;
			else if(SwingUtilities.isRightMouseButton(e))
				recursiveExpand(nucname, !internal.expanded);
			repaint();
			}
		}

	/**
	 * Recursively expand/un-expand
	 */
	public void recursiveExpand(String nucName, boolean expand)
		{
		if(lin!=null)
			{
			Internal internal=getNucinfo(nucName);
			internal.expanded=expand;
			NucLineage.Nuc nuc=lin.nuc.get(nucName);
			for(String childName:nuc.child)
				recursiveExpand(childName, expand);
			}
		}
	
	
	}