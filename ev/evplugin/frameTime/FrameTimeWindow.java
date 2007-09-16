package evplugin.frameTime;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import java.util.*;
import org.jfree.data.xy.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;

import evplugin.basicWindow.*;
import evplugin.ev.EV;
import evplugin.imageWindow.*;
import evplugin.metadata.MetaObject;
import evplugin.metadata.Metadata;
import org.jdom.*;

//TODO: auto-replicate down to metadata

/**
 * Adjust Frame-Time mapping
 * @author Johan Henriksson
 */
public class FrameTimeWindow extends BasicWindow implements ActionListener, ChangeListener, ObjectCombo.comboFilterMetaObject
	{
	static final long serialVersionUID=0;

	public static void initPlugin() {}
	static
		{
		BasicWindow.addBasicWindowExtension(new FrameTimeBasic());
		
		ImageWindow.addImageWindowExtension(new ImageWindowExtension()
			{
			public void newImageWindow(ImageWindow w)
				{
				w.imageWindowTools.add(new ToolSetFrametime(w));
				}
			});
		}
	
	
	//GUI components
	private JButton bAdd=new JButton("Add");
	private JButton bApply=new JButton("Apply");
	private JButton bRefresh=new JButton("Refresh");
	private JButton bSaveText=new JButton("Save text");
	
	private JPanel datapart=new JPanel();
	private XYSeries frametimeSeries=new XYSeries("FT");
	private Vector<JSpinner[]> inputVector=new Vector<JSpinner[]>();
	
	private ObjectCombo objectCombo=new ObjectCombo(this, false);

	/**
	 * Store down settings for window into personal config file
	 */
	public void windowPersonalSettings(Element root)
		{
		}

	

	/**
	 * Make a new window at default location
	 */
	public FrameTimeWindow()
		{
		this(100,100,1000,600);
		}
	
	/**
	 * Make a new window at some specific location
	 */
	public FrameTimeWindow(int x, int y, int w, int h)
		{				
		bAdd.addActionListener(this);
		bApply.addActionListener(this);
		bRefresh.addActionListener(this);
		bSaveText.addActionListener(this);
		objectCombo.addActionListener(this);
		
		XYDataset xyDataset = new XYSeriesCollection(frametimeSeries);
				
		JFreeChart chart = ChartFactory.createXYLineChart
            ("","Time","Frame",xyDataset,PlotOrientation.HORIZONTAL,false/*legend*/, false/*tooltips*/, false/*urls*/);
		ChartPanel graphpanel = new ChartPanel(chart);
		
		//Put GUI together
		JPanel datapanel=new JPanel(new BorderLayout());
		JPanel dataparto=new JPanel(new BorderLayout());
		dataparto.add(datapart,BorderLayout.NORTH);
		JScrollPane datapartscroll=new JScrollPane(dataparto, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		JPanel buttonpanel=new JPanel(new GridLayout(2,2));
		buttonpanel.add(bAdd);
		buttonpanel.add(bApply);
		buttonpanel.add(bRefresh);
		buttonpanel.add(bSaveText);
		datapanel.add(buttonpanel, BorderLayout.SOUTH);
		datapanel.add(datapartscroll, BorderLayout.CENTER);
		setLayout(new BorderLayout());
		add(datapanel, BorderLayout.EAST);		
		
		JPanel leftPanel=new JPanel(new BorderLayout());
		leftPanel.add(graphpanel,BorderLayout.CENTER);
		leftPanel.add(objectCombo, BorderLayout.SOUTH);
		add(leftPanel,BorderLayout.CENTER);
		
		
		loadData();
		
		//Window overall things
		setTitle(EV.programName+" Frame/Time");
		pack();
		setVisible(true);
		setBounds(x,y,w,h);
		}

	/**
	 * For combo box - which meta objects to list
	 */
	public boolean comboFilterMetaObjectCallback(MetaObject ob)
		{
		return ob instanceof FrameTime;
		}
	
	/**
	 * Add special options for the combo box
	 */
	public ObjectCombo.Alternative[] comboAddAlternative(final ObjectCombo combo, final Metadata meta)
		{
		ObjectCombo.Alternative a=new ObjectCombo.Alternative(meta, null, "<Create frame/time>",new ActionListener()
			{
			public void actionPerformed(ActionEvent e)
				{
				//Create a new Frame/Time metaobject
				FrameTime ft=new FrameTime();
				meta.addMetaObject(ft);
				//TODO: select in combo as well?
				BasicWindow.updateWindows();
				}
			});
		return new ObjectCombo.Alternative[]{a};
		}

	
	/**
	 * Add an entry. Does not update UI
	 * @param frame
	 * @param time
	 */
	public void addEntry(int frame, double time)
		{
		JSpinner field[]=new JSpinner[2];
		SpinnerModel numModel1 =new SpinnerNumberModel(frame,-1, 100000000,  1  );
		SpinnerModel numModel2 =new SpinnerNumberModel(time, 0.0,100000000.0,1.0);
		field[0]=new JSpinner(numModel1);
		field[1]=new JSpinner(numModel2);
		inputVector.add(field);
		field[0].addChangeListener(this);
		field[1].addChangeListener(this);
		}
	
	/**
	 * Load data from SQL
	 */
	public void loadData()
		{
		inputVector.clear();
		/*
		DB db=DB.getCurrentDB();
		if(db!=null)
			{
			FrameTime frametime=new FrameTime(db, DB.currentSample);
			frametime.fromSql();
			for(FrameTime.Pair p:frametime.list)
				addEntry(p.frame, p.frametime);
			}		
		*/
		
		
		FrameTime meta=(FrameTime)objectCombo.getObject();
		if(meta!=null)
			for(Pair p:meta.list)
				addEntry(p.frame, p.frametime);
		
		fillGraphpart();
		fillDatapart();
		}
	
	
	
	/**
	 * Save data to SQL
	 */
	public void saveData()
		{
		//should save even exist?
		FrameTime meta=(FrameTime)objectCombo.getObject();
		if(meta!=null)
			{
			meta.list.clear();
			for(int i=0;i<inputVector.size();i++)
				meta.add((Integer)inputVector.get(i)[0].getValue(), (Double)inputVector.get(i)[1].getValue());
			meta.metaObjectModified=true;
			}
		}
	
	/**
	 * Save data to text file
	 */
	public void saveTextData()
		{
		JFileChooser chooser = new JFileChooser();
		int returnVal = chooser.showSaveDialog(this);
		if(returnVal == JFileChooser.APPROVE_OPTION)
			{
			String filename=chooser.getSelectedFile().getAbsolutePath();

			FrameTime frametime=(FrameTime)objectCombo.getObject();//new FrameTime(db, DB.currentSample);
			if(frametime!=null)
				{
				for(int i=0;i<inputVector.size();i++)
					frametime.add((Integer)inputVector.get(i)[0].getValue(), (Double)inputVector.get(i)[1].getValue());
				frametime.storeTextFile(filename);
				}
			}
		}

	/**
	 * Regenerate all points in the graph
	 */
	public void fillGraphpart()
		{
		frametimeSeries.clear();
		for(int i=0;i<inputVector.size();i++)
			{
			double frame=(Integer)inputVector.get(i)[0].getValue();
			double time=(Double)inputVector.get(i)[1].getValue();
			frametimeSeries.add(time, frame);
			}
		//would we want to list special times here?
		}	
	
	/**
	 * Regenerate UI
	 */
	public void fillDatapart()
		{
		datapart.removeAll();
		datapart.setLayout(new GridLayout(1+inputVector.size(),2));
		datapart.add(new JLabel("Frame"));
		datapart.add(new JLabel("Time"));
		for(int i=0;i<inputVector.size();i++)
			{
			datapart.add(inputVector.get(i)[0]);
			datapart.add(inputVector.get(i)[1]);
			}
		setVisible(true);
		}
	
	
	/*
	 * (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e)
		{
		if(e.getSource()==objectCombo)
			{
			loadData();
			}
		if(e.getSource()==bAdd)
			{
			addEntry(0, 0);
			fillGraphpart();
			fillDatapart();
			}
		else if(e.getSource()==bRefresh)
			{
			loadData();
			}
		else if(e.getSource()==bApply)
			{
			saveData();
			loadData();
			}
		else if(e.getSource()==bSaveText)
			{
			
			}
			
		
		}
	
	
	/*
	 * (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent e)
		{
		for(int i=0;i<inputVector.size();i++)
			if(inputVector.get(i)[0]==e.getSource())
				{
				//Negative frame = delete
				if((Integer)inputVector.get(i)[0].getValue()==-1)
					{
					inputVector.remove(i);
					fillGraphpart();
					fillDatapart();
					}
				}		
		
		fillGraphpart();
		}
	
	/*
	 * (non-Javadoc)
	 * @see client.BasicWindow#dataChanged()
	 */
	public void dataChangedEvent()
		{
		objectCombo.updateObjectList();
		loadData();
		}
	
	
	
	
	}