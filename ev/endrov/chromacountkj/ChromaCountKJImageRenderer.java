/***
 * Copyright (C) 2010 Johan Henriksson
 * This code is under the Endrov / BSD license. See www.endrov.net
 * for the full text and how to cite.
 */
package endrov.chromacountkj;

import java.awt.*;
import java.util.*;
import javax.vecmath.*;

import endrov.imageWindow.*;

public class ChromaCountKJImageRenderer implements ImageWindowRenderer
	{
	public ImageWindowInterface w;
	
	
	public ChromaCountKJImageRenderer(ImageWindowInterface w)
		{
		this.w=w;
		}

	
	public Collection<ChromaCountKJ> getVisible()
		{
		//TODO: pick out
		return ChromaCountKJ.getObjects(w.getRootObject());
		}
	

	/**
	 * Render nuclei
	 */
	public void draw(Graphics g)
		{
		for(ChromaCountKJ ann:getVisible())
			{
			//Coordinate transformation
			Vector2d so=w.transformPointW2S(new Vector2d(ann.pos.x,ann.pos.y));

			int r=2;
			
			g.setColor(Color.RED);
			g.drawOval((int)(so.x-r),(int)(so.y-r),(int)(2*r),(int)(2*r));

			String s=""+ann.group;
			g.drawString(s, (int)so.x-g.getFontMetrics().stringWidth(s)/2, (int)so.y-2);
			
			}
		}
	
	
	public void dataChangedEvent()
		{
		}

	
	}
