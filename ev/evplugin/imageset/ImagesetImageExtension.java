package evplugin.imageset;

import java.awt.event.*;
import javax.swing.*;



import evplugin.basicWindow.BasicWindow;
import evplugin.imageWindow.*;

public class ImagesetImageExtension implements ImageWindowExtension
	{
	JMenuItem miRemoveChannel=new JMenuItem("Remove channel");
	JMenuItem miRemoveFrame=new JMenuItem("Remove frame");
	JMenuItem miRemoveSlice=new JMenuItem("Remove slice");
	
	public void newImageWindow(final ImageWindow w)
		{
		//Create menus
		w.menuImage.add(miRemoveChannel);
		w.menuImage.add(miRemoveFrame);
		w.menuImage.add(miRemoveSlice);

		
		//The listener
		ActionListener listener=new ActionListener()
			{
			public void actionPerformed(ActionEvent e)
				{
				if(e.getSource()==miRemoveChannel)
					{
					String ch=w.getCurrentChannelName();
					if(JOptionPane.showConfirmDialog(null, "Do you really want to remove channel "+ch+"?","EV",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
						{
						w.getImageset().removeChannel(ch);
						BasicWindow.updateWindows();
						}
					}
				else if(e.getSource()==miRemoveFrame)
					{
					String ch=w.getCurrentChannelName();
					int frame=(int)w.frameControl.getFrame();
					
					if(JOptionPane.showConfirmDialog(null, "Do you really want to remove channel "+ch+", frame "+frame+"?","EV",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
						{
						w.getImageset().getChannel(ch).imageLoader.remove(frame);
						BasicWindow.updateWindows();
						}
					}
				else if(e.getSource()==miRemoveSlice)
					{
					String ch=w.getCurrentChannelName();
					int frame=(int)w.frameControl.getFrame();
					int z=w.frameControl.getZ();
					
					if(JOptionPane.showConfirmDialog(null, "Do you really want to remove channel "+ch+", frame "+frame+", slice "+z+"?","EV",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
						{
						w.getImageset().getChannel(ch).imageLoader.get(frame).remove(z);
						BasicWindow.updateWindows();
						}
					}
				
				
				
				}	
			};
		

		//Add listeners
		miRemoveChannel.addActionListener(listener);
		miRemoveFrame.addActionListener(listener);
		miRemoveSlice.addActionListener(listener);
		}

	
	
	
	
	}