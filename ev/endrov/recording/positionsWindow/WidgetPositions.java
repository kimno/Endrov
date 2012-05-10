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
import java.util.LinkedHashMap;
import java.util.LinkedList;

import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import javax.swing.ListSelectionModel;

import endrov.hardware.EvDevice;
import endrov.hardware.EvHardware;
import endrov.recording.RecordingResource;
import endrov.recording.RecordingResource.PositionListListener;
import endrov.recording.device.HWStage;
import endrov.util.EvSwingUtil;

/**
 * 
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
	
	private CheckBoxList<HWStage> infoList;
	private LinkedHashMap<HWStage, Boolean> infoModel;
	private JScrollPane infoScroller;
	
	private JButton bAdd=new JButton("Add");
	private JButton bRemove=new JButton("Remove");
	private JButton bGoTo=new JButton("Go To Position");
	private JButton bMoveUp=new JButton("Move Up");
	private JButton bMoveDown=new JButton("Move Down");
	private JButton bSave=new JButton("Save Positions");
	private JButton bLoad=new JButton("Load Positions");
	
	
	public WidgetPositions()
	{
		
		bAdd.addActionListener(this);
		bRemove.addActionListener(this);
		bGoTo.addActionListener(this);
		bMoveUp.addActionListener(this);
		bMoveDown.addActionListener(this);
		bSave.addActionListener(this);
		bLoad.addActionListener(this);
		
		JPanel bPanel = new JPanel();
		JPanel posPanel=new JPanel(new BorderLayout());
		this.setLayout(new BorderLayout());
		
		bPanel.add(EvSwingUtil.layoutCompactVertical(bAdd,bRemove,bGoTo,bMoveUp,bMoveDown,bSave,bLoad));
		
		listModel = new DefaultListModel();
		infoModel = new LinkedHashMap<HWStage, Boolean>();
		
		infoList = new CheckBoxList<HWStage>(infoModel);
		
		posList = new JList(listModel);
		posList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		posList.setLayoutOrientation(JList.VERTICAL);
		
		listScroller = new JScrollPane(posList);
		posPanel.add(listScroller,BorderLayout.NORTH);
		
		
		infoScroller = new JScrollPane(infoList);
		posPanel.add(infoScroller,BorderLayout.CENTER);
		
		
//		infoModel.put("sovande troll", true);
//		infoModel.put("tarmen mår bra av en deg", true);
////		infoList.se
//		infoList = new CheckBoxList<String>(infoModel);
		
		add(EvSwingUtil.withTitledBorder("Positions", posPanel),BorderLayout.CENTER); 
		add(EvSwingUtil.withTitledBorder("", bPanel),BorderLayout.WEST); 
		
		
		
		
		
		for(HWStage stage:EvHardware.getDeviceMapCast(HWStage.class).values())
		{
			infoModel.put(stage, true);
		}
		
		
//		infoModel.put("sovande troll", true);
//		infoModel.put("tarmen mår bra av en deg", true);

		
		infoList.updateModel(infoModel);
		
//		for(HWStage stage:EvHardware.getDeviceMapCast(HWStage.class).values())
//		{
//		String[] aname=stage.getAxisName();
//		for(int i=0;i<aname.length;i++)
//			if(aname[i].equals("x"))
//				return stage.getStagePos()[i];
//		}
		
		RecordingResource.posListListeners.addWeakListener(this);
		
	}

	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource()==bAdd)
		{
			Position newPos = new Position(getStageX(), getStageY(), 0);
			//listModel.addElement(newPos);
			
			RecordingResource.posList.add(newPos);
			RecordingResource.posListUpdated();
			
			
			
			//RecordingResource.posListListeners.addWeakListener((PositionListListener) listModel);
			//här jobbar vi just nUUUU!!
			
		}
		else if(e.getSource()==bRemove)
		{
//			int index = posList.getSelectedIndex();
//			if(index >=0)
//				listModel.remove(index);	
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
//				pos = (Position)listModel.get(index);
//				
//				Map<String, Double> gotoPos=new HashMap<String, Double>();		
//
//				gotoPos.put("x",pos.getX());
//				gotoPos.put("y",pos.getY());
//				RecordingResource.setStagePos(gotoPos);
//				System.out.println(""+pos);
				pos = (Position) RecordingResource.posList.get(index);
				
				Map<String, Double> gotoPos=new HashMap<String, Double>();
				gotoPos.put("x",pos.getX());
				gotoPos.put("y",pos.getY());
				RecordingResource.setStagePos(gotoPos);
				System.out.println(""+pos);
			}
		}
		else if(e.getSource()==bMoveUp)
		{
			int index = posList.getSelectedIndex();
			if(index >0){

//				Position[] anArray = new Position[listModel.getSize()];
//				listModel.copyInto(anArray);
//				DefaultListModel newList = new DefaultListModel();
//				for(int i =0; i< anArray.length; i++){
//					newList.addElement(anArray[i]);
//				}
//				newList.add(index+1, newList.get(index-1));
//				newList.remove(index-1);
//				listModel = newList;
				
//				Position[] anArray = new Position[RecordingResource.posList.size()];
				
//				Position[] anArray;
//				anArray = (Position[]) RecordingResource.posList.toArray();
//				
//				DefaultListModel newList = new DefaultListModel();
//				for(int i =0; i< anArray.length; i++){
//					newList.addElement(anArray[i]);
//				}
				
				
				
				
//				DefaultListModel newList = new DefaultListModel();
//				for(int i =0; i< RecordingResource.posList.size(); i++){
//					newList.addElement(RecordingResource.posList.get(i));
//				}
				LinkedList<Position> newList = (LinkedList<Position>) RecordingResource.posList.clone();
				newList.add(index+1, newList.get(index-1));
				newList.remove(index-1);
				RecordingResource.posList = newList;		
				RecordingResource.posListUpdated();
				
//				listModel.add(index+1, listModel.get(index-1));
//				listModel.remove(index-1);
				
			}
			
		}
		else if(e.getSource()==bMoveDown)
		{
			int index = posList.getSelectedIndex();
			if(index >= 0 && index < listModel.getSize()-1){
				
//				Position[] anArray = new Position[listModel.getSize()];
//				listModel.copyInto(anArray);
//				DefaultListModel newList = new DefaultListModel();
//				
//				for(int i =0; i< anArray.length; i++){
//					newList.addElement(anArray[i]);
//				}
//				newList.add(index, newList.get(index+1));
//				newList.remove(index+2);
//				listModel = newList;
				
				
				LinkedList<Position> newList = (LinkedList<Position>) RecordingResource.posList.clone();
				newList.add(index, newList.get(index+1));
				newList.remove(index+2);
				RecordingResource.posList = newList;		
				RecordingResource.posListUpdated();
				
				
//				listModel.add(index, listModel.get(index+1));
//				listModel.remove(index+2);
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
		
			//listModel.removeAllElements();
//			for(int i =0; i< anArray.length; i++){
//				listModel.addElement(anArray[i]);
//			}		
			for(int i =0; i< anArray.length; i++){
				RecordingResource.posList.add(anArray[i]);
			}		
			RecordingResource.posListUpdated();
			//System.out.println( anArray[0] );
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
