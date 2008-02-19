package evplugin.modelWindow.slice3d;

import java.awt.*;
import java.awt.event.*;
import java.lang.ref.WeakReference;
import java.util.*;

import javax.media.opengl.*;
import javax.swing.*;
import javax.swing.event.*;

import org.jdom.Element;

import evplugin.basicWindow.BasicWindow;
import evplugin.basicWindow.ChannelCombo;
import evplugin.basicWindow.ColorCombo;
import evplugin.data.*;
import evplugin.ev.*;
import evplugin.imageset.*;
import evplugin.modelWindow.*;


/**
 * 
 * @author Johan Henriksson
 */
public class Slice3DExtension implements ModelWindowExtension
	{
	public static void initPlugin() {}
	static
		{
		ModelWindow.modelWindowExtensions.add(new Slice3DExtension());
		}
	
	public void newModelWindow(ModelWindow w)
		{
		w.modelWindowHooks.add(new Hook(w)); 
		}

	
	private class Hook implements ModelWindowHook, ActionListener
		{
		private ModelWindow w;
		private Vector<ToolIsolayer> isolayers=new Vector<ToolIsolayer>();
		private JButton addIsolevel=new JButton("Add slice");

		public Hook(ModelWindow w)
			{
			this.w=w;
			addIsolevel.addActionListener(this);
			}
		
		
		public Collection<Double> adjustScale()
			{
			return Collections.emptySet(); //TODO
			}
		public Collection<Vector3D> autoCenterMid()
			{
			return Collections.emptySet(); //TODO
			}
		public Collection<Double> autoCenterRadius(Vector3D mid, double FOV)
			{
			return Collections.emptySet(); //TODO
			}
		public boolean canRender(EvObject ob){return false;}
		public void displayInit(GL gl){}
		public void displaySelect(GL gl){}
		public void readPersonalConfig(Element e){}
		public void savePersonalConfig(Element e){}
		public void select(int id){}
		public void datachangedEvent(){}
		public void fillModelWindomMenus()
			{
			w.sidepanelItems.add(addIsolevel);
			for(ToolIsolayer ti:isolayers)
				w.sidepanelItems.add(ti);
			}

		
		
		public void actionPerformed(ActionEvent e)
			{
			isolayers.add(new ToolIsolayer());
			w.updateToolPanels();
			}
		
		
		private double getFrame()
			{
			return this.w.frameControl.getFrame();
			}

		
		
		public void displayFinal(GL gl)
			{
			for(ToolIsolayer ti:isolayers)
				ti.render(gl);
			}
		

		
		
		
		private class ToolIsolayer extends JPanel implements ChangeListener, ActionListener
			{
			static final long serialVersionUID=0;
			private JSpinner zplaneSpinner=new JSpinner(new SpinnerNumberModel((int)0.0,(int)-99.0,(int)999.0,(int)1));
			private ChannelCombo chanCombo=new ChannelCombo(null,true);
			private JButton bDelete=new JButton(BasicWindow.getIconDelete());
			private JCheckBox zProject=new JCheckBox("@Z=0");
			private ColorCombo colorCombo=new ColorCombo();
			private WeakReference<Imageset> lastImageset=new WeakReference<Imageset>(null);
			private Slice3D slice=new Slice3D();
			
			
			public ToolIsolayer()
				{
				JPanel q3=new JPanel(new BorderLayout());
				q3.add(zProject,BorderLayout.CENTER);
				q3.add(bDelete,BorderLayout.EAST);
				JPanel q4=new JPanel(new GridLayout(1,2));
				q4.add(withLabel("Slice:",zplaneSpinner));
				q4.add(colorCombo);
				setLayout(new GridLayout(3,1));
				setBorder(BorderFactory.createEtchedBorder());
				add(chanCombo);
				add(q4);
				add(q3);
				
				zplaneSpinner.addChangeListener(this);
				chanCombo.addActionListener(this);
				bDelete.addActionListener(this);
				zProject.addActionListener(this);
				}
			
			
			public void stateChanged(ChangeEvent arg0)
				{
				slice.rebuild();
				w.view.repaint();
//				w.repaint();
				}


			public void actionPerformed(ActionEvent e)
				{
				if(e.getSource()==bDelete)
					{
					isolayers.remove(this);
					w.updateToolPanels();
					}
				stateChanged(null);
				}

			/**
			 * Embed control with a label
			 */
			private JComponent withLabel(String text, JComponent right)
				{
				JPanel p=new JPanel(new BorderLayout());
				p.add(new JLabel(text),BorderLayout.WEST);
				p.add(right,BorderLayout.CENTER);
				return p;
				}
			
			/**
			 * Render according to these controls. Create surfaces as needed.
			 */
			public void render(GL gl)
				{
				chanCombo.updateChannelList();
				
				//Make sure surfaces are for the right imageset
				Imageset im=chanCombo.getImageset();
				if(lastImageset.get()!=im)
					slice.rebuild();
				lastImageset=new WeakReference<Imageset>(im);
				
				
				String channelName=chanCombo.getChannel();
				Imageset.ChannelImages ch=im.channelImages.get(channelName);
				if(ch!=null)
					{
					int cframe=ch.closestFrame((int)getFrame());
					int zplane=(Integer)zplaneSpinner.getModel().getValue();

					//Create surface if it wasn't there before
					if(slice.needBuild(cframe))
						slice.build(gl, cframe, im, ch, zplane);
					
					//Finally render
					int z=0;
					if(!zProject.isSelected())
						z=zplane;
					slice.render(gl,colorCombo.getColor(), z);
					}
				}
			
			}
		
		
		}
	
	}