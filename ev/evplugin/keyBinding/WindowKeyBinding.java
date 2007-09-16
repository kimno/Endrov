package evplugin.keyBinding;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import evplugin.basicWindow.*;
import evplugin.ev.*;
import org.jdom.*;

/**
 * Adjust Frame-Time mapping
 * @author Johan Henriksson
 */
public class WindowKeyBinding extends BasicWindow implements ActionListener
	{
	static final long serialVersionUID=0;


	/**
	 * Make a new window at default location
	 */
	public WindowKeyBinding()
		{
		this(50,100,500,600);
		}

	private JPanel listPane=new JPanel();

	private JButton bNewScriptBinding=new JButton("New script binding");
	
	/**
	 * Make a new window at some specific location
	 */
	public WindowKeyBinding(int x, int y, int w, int h)
		{				
		
		JPanel wholePane=new JPanel(new BorderLayout());
		wholePane.add(listPane,BorderLayout.NORTH);
		JScrollPane spane=new JScrollPane(wholePane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		bNewScriptBinding.addActionListener(this);
		
		setLayout(new BorderLayout());
		add(spane, BorderLayout.CENTER);
		add(bNewScriptBinding, BorderLayout.SOUTH);

		//Window overall things
		setTitle(EV.programName+" Key Bindings");
		pack();
		setVisible(true);
		setBounds(x,y,w,h);

		fillList();
		}

	
	public void actionPerformed(ActionEvent e)
		{
		if(e.getSource()==bNewScriptBinding)
			{
			ScriptBinding.list.add(new ScriptBinding());
			fillList();
			}
		}

	/**
	 * Get a constraint for the layout
	 */
	private GridBagConstraints gconstraint(int x, int y, int w)
		{
		GridBagConstraints c=new GridBagConstraints();
		c.gridx=x;
		c.gridy=y;
		c.gridwidth=w;
		c.fill=GridBagConstraints.HORIZONTAL;
		c.weightx=1;
		return c;
		}
	
	/**
	 * Fill list with key bindings
	 */
	public void fillList()
		{
		//One might want to sort
		listPane.removeAll();
		listPane.setLayout(new GridBagLayout());
		
		int y=0;
		
		//All special key bindings
		for(final KeyBinding b:KeyBinding.bindings.values())
			{
			listPane.add(new JLabel(b.pluginName),gconstraint(0,y,1));
			listPane.add(new JLabel(b.description),gconstraint(1,y,1));
			JButton key=new JButton(b.keyDesc());
			listPane.add(key,gconstraint(2,y,1));
			key.addActionListener(new ActionListener()
				{
				public void actionPerformed(ActionEvent e)
					{
					new SetKey(b);
					}
				});
			y++;
			}
		
		//All script key bindings
		for(int i=0;i<ScriptBinding.list.size();i++)
			{
			final ScriptBinding b=ScriptBinding.list.get(i);
			JButton field=new JButton(b.script);
			if(b.script.equals(""))
				field.setText(" ");
			listPane.add(field,gconstraint(0,y,2));
			JButton key=new JButton(b.key.keyDesc());
			listPane.add(key,gconstraint(2,y,1));
			key.addActionListener(new ActionListener()
				{
				public void actionPerformed(ActionEvent e)
					{
					new SetKey(b.key);
					}
				});
			final int icopy=i;
			field.addActionListener(new ActionListener()
				{
				public void actionPerformed(ActionEvent e)
					{
					String newText=JOptionPane.showInputDialog(null, "Script code",b.script);
					if(newText!=null)
						{
						if(newText.equals(""))
							//Remove item
							ScriptBinding.list.remove(icopy);
						else
							//Set script
							b.script=newText;
						fillList();
						}
					}
				});
			y++;
			}

		
		setVisible(true);
		repaint();
		}
	
	/**
	 * Store down settings for window into personal config file
	 */
	public void windowPersonalSettings(Element root)
		{
		}
	
	/*
	 * (non-Javadoc)
	 * @see client.BasicWindow#dataChanged()
	 */
	public void dataChangedEvent()
		{
		}
	
	
	/**
	 * Set new key for key binding
	 */
	private class SetKey extends JDialog implements KeyListener
		{
		static final long serialVersionUID=0;
		KeyBinding b;
		public SetKey(KeyBinding b)
			{
			this.b=b;
			add(new JLabel("Press new key"));
			setEnabled(true);
			addKeyListener(this);
			pack();
			setLocation(200, 200);
			setVisible(true);
			}
		public void keyPressed(KeyEvent e)
			{
			if(e.getKeyCode()==KeyEvent.VK_UNDEFINED)
				b.key=e.getKeyChar();
			else
				{
				b.key=null;
				b.keyCode=e.getKeyCode();
				}
			fillList();
			dispose();
			}
		public void keyReleased(KeyEvent e){}
		public void keyTyped(KeyEvent e){}
		}
	

	
	}