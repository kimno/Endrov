package evplugin.pluginWindow;

import evplugin.basicWindow.*;

import java.awt.event.*;
import javax.swing.JMenuItem;

/**
 * Extension to BasicWindow
 * @author Johan Henriksson
 */
public class PluginWindowBasic implements BasicWindowExtension
	{
	public void newBasicWindow(BasicWindow w)
		{
		w.basicWindowExtensionHook.put(this.getClass(),new Hook());
		}
	private class Hook implements BasicWindowHook, ActionListener
		{
		public void createMenus(BasicWindow w)
			{
			JMenuItem mi=new JMenuItem("Plugins");
			mi.addActionListener(this);
			w.addMenuWindow(mi);
			}
		
		public void actionPerformed(ActionEvent e) 
			{
			new PluginWindow();
			}
		
		public void buildMenu(BasicWindow w){}
		}
	}