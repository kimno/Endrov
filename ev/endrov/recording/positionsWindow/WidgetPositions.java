/***
 * Copyright (C) 2010 Johan Henriksson
 * This code is under the Endrov / BSD license. See www.endrov.net
 * for the full text and how to cite.
 */
package endrov.recording.positionsWindow;

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;

import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import javax.swing.ListSelectionModel;

import endrov.hardware.EvDevice;
import endrov.hardware.EvDevicePath;
import endrov.hardware.EvHardware;
import endrov.recording.RecordingResource;
import endrov.recording.ResolutionManager;
import endrov.recording.RecordingResource.PositionListListener;
import endrov.recording.device.HWCamera;
import endrov.recording.device.HWStage;
import endrov.util.EvSwingUtil;

/**
 * Widget used by PositionsWindow to handle positions
 */
public class WidgetPositions extends JPanel implements ActionListener, PositionListListener
{
	private static final long serialVersionUID = 1L;
	
	//which device here
	//MM: switch of hw autofocus while moving xy
	//MM: switch of hw autofocus while moving z
	
	private JList posList;
	private DefaultListModel listModel;
	private JScrollPane listScroller;
	
	//private CheckBoxList<HWStage> infoList;
	private CheckBoxList2 infoList;
	//private LinkedHashMap<HWStage, Boolean> infoModel;
	private JScrollPane infoScroller;
	
	private JButton bAdd=new JButton("Add");
	private JButton bRemove=new JButton("Remove");
	private JButton bGoTo=new JButton("Go To Position");
	private JButton bMoveUp=new JButton("Move Up");
	private JButton bMoveDown=new JButton("Move Down");
	private JButton bSave=new JButton("Save Positions");
	private JButton bLoad=new JButton("Load Positions");
	
	private JButton bZero=new JButton("Go Zero");
	
	
	public WidgetPositions()
	{
		
		bAdd.addActionListener(this);
		bRemove.addActionListener(this);
		bGoTo.addActionListener(this);
		bMoveUp.addActionListener(this);
		bMoveDown.addActionListener(this);
		bSave.addActionListener(this);
		bLoad.addActionListener(this);
		
		bZero.addActionListener(this);
		
		JPanel bPanel = new JPanel();
		JPanel posPanel=new JPanel(new BorderLayout());
		this.setLayout(new BorderLayout());
		
		bPanel.add(EvSwingUtil.layoutCompactVertical(bAdd,bRemove,bGoTo,bMoveUp,bMoveDown,bSave,bLoad, bZero));
		
		listModel = new DefaultListModel();
		
		infoList = new CheckBoxList2();

		posList = new JList(listModel);
		posList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		posList.setLayoutOrientation(JList.VERTICAL);
		
		listScroller = new JScrollPane(posList);
		posPanel.add(listScroller,BorderLayout.NORTH);
		
		
		infoScroller = new JScrollPane(infoList);
		
		posPanel.add(infoScroller,BorderLayout.CENTER);
		
		add(EvSwingUtil.withTitledBorder("Positions", posPanel),BorderLayout.CENTER); 
		add(EvSwingUtil.withTitledBorder("", bPanel),BorderLayout.WEST); 
		
		RecordingResource.posListListeners.addWeakListener(this);
		
		RecordingResource.posListUpdated();
		
		
	}

	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource()==bAdd)
		{
			AxisInfo[] newInfo = new AxisInfo[infoList.getInfo().length];
			for(int i = 0; i< infoList.getInfo().length; i++){
				newInfo[i] = new AxisInfo(
						infoList.getInfo()[i].getDevice(),
						infoList.getInfo()[i].getAxis(),
						infoList.getInfo()[i].getDevice().getStagePos()[infoList.getInfo()[i].getAxis()]
								);
			}
			
			//Find all names in use
			Set<String> usedNames=new HashSet<String>();
			for(Position pos: RecordingResource.posList)
				usedNames.add(pos.getName());
			
			
			//Generate an unused name
			String newName;
			int posi=0;
			do
				{
				newName="POS"+posi;
				posi++;
				} while(usedNames.contains(newName));

			Position newPos = new Position(newInfo, newName);
			RecordingResource.posList.add(newPos);
			
			RecordingResource.posListUpdated();
			
			
		}
		else if(e.getSource()==bRemove)
		{
			int index = posList.getSelectedIndex();
			if(index >=0){
				RecordingResource.posList.remove(index);
				RecordingResource.posListUpdated();
			}
		}
		else if(e.getSource()==bGoTo)
		{
			Position pos;
			int index = posList.getSelectedIndex();
			if(index >=0){

				pos = (Position) RecordingResource.posList.get(index);
				
				Map<String, Double> gotoPos=new HashMap<String, Double>();
							
				//spara in gamla värden
				
				
				for(int i = 0; i<pos.getAxisInfo().length; i++){
					gotoPos.put(pos.getAxisInfo()[i].getDevice().getAxisName()[pos.getAxisInfo()[i].getAxis()],
							pos.getAxisInfo()[i].getValue());
				}
				
				
				
				RecordingResource.setStagePos(gotoPos);
				
//				System.out.println(""+pos);
			}
		}
		else if(e.getSource()==bMoveUp)
		{
			int index = posList.getSelectedIndex();
			if(index >0){
				LinkedList<Position> newList = (LinkedList<Position>) RecordingResource.posList.clone();
				newList.add(index+1, newList.get(index-1));
				newList.remove(index-1);
				RecordingResource.posList = newList;		
				RecordingResource.posListUpdated();
				
			}
			
		}
		else if(e.getSource()==bMoveDown)
		{
			int index = posList.getSelectedIndex();
			if(index >= 0 && index < listModel.getSize()-1){			
				
				LinkedList<Position> newList = (LinkedList<Position>) RecordingResource.posList.clone();
				newList.add(index, newList.get(index+1));
				newList.remove(index+2);
				RecordingResource.posList = newList;		
				RecordingResource.posListUpdated();

			}
					
		}
		else if(e.getSource()==bSave)
		{
			Position[] anArray = new Position[listModel.getSize()];
			listModel.copyInto(anArray);
			try{
			      //use buffering
			      OutputStream file = new FileOutputStream( "list.ser" );
			      OutputStream buffer = new BufferedOutputStream( file );
			      ObjectOutput output = new ObjectOutputStream( buffer );
			      try{
			        output.writeObject(anArray);
			      }
			      finally{
			        output.close();
			      }
			    }  
			    catch(IOException ex){
			      
			    }
		}
		else if(e.getSource()==bLoad)
		{
			Position[] anArray = null;

			try{
			      //use buffering
			      InputStream file = new FileInputStream( "list.ser" );
			      InputStream buffer = new BufferedInputStream( file );
			      ObjectInput input = new ObjectInputStream ( buffer );
			      try{
			        //deserialize the List
			    	  anArray = (Position[])input.readObject() ;
			        
			      } catch (ClassNotFoundException e2) {
					// TODO Auto-generated catch block
				}
			      finally{
			        input.close();
			      }
			      
			      
			} catch (FileNotFoundException e2) {
				// TODO Auto-generated catch block
				
			} catch (IOException e2) {
				// TODO Auto-generated catch block
			}finally{}
			
			for(int i =0; i< anArray.length; i++){
				RecordingResource.posList.add(anArray[i]);
			}		
			RecordingResource.posListUpdated();

		}
		else if (e.getSource() == bZero){

			Map<String, Double> gotoPos=new HashMap<String, Double>();
	
			gotoPos.put("Z", 88.0);
			gotoPos.put("X", 0.0);
			gotoPos.put("Y", 0.0);
	
			RecordingResource.setStagePos(gotoPos);
		}
	}
	
	
	public void dataChangedEvent()
	{
		//TODO
	}
	
	public double getStageX() // um
	{
		return RecordingResource.getCurrentStageX();
	}

	public double getStageY() // um
	{
		return RecordingResource.getCurrentStageY();
	}


	public void positionsUpdated() {
		listModel.removeAllElements();
		for(int i = 0; i< RecordingResource.posList.size(); i++){
			listModel.addElement(RecordingResource.posList.get(i));	
			
		}
		
	}
	
}
