/***
 * Copyright (C) 2010 Johan Henriksson
 * This code is under the Endrov / BSD license. See www.endrov.net
 * for the full text and how to cite.
 */
package endrov.recording.positionsWindow;

import java.awt.Color;
import java.io.Serializable;
import java.util.LinkedHashMap;

import endrov.basicWindow.EvColor;
import endrov.hardware.EvDevicePath;
import endrov.hardware.EvHardware;
import endrov.recording.device.HWStage;

/**
 * 
 */
public class Position implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	private AxisInfo[] info;
	private EvColor color;
	private String name;

	
	
	Position(AxisInfo[] axisInfo){
		
		info = new AxisInfo[axisInfo.length];
		for(int i = 0; i<axisInfo.length; i++){
			info[i] = axisInfo[i];
		}
	
		
		this.color = new EvColor("White", 1, 1, 1, 1);
		
	}
	
	public EvColor getColor(){
		return color;
	}
	
	public AxisInfo[] getAxisInfo(){
		return info;
	}

	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public String toString(){
		String arrayInfo = "";
		for(int i = 0; i<info.length; i++){
			arrayInfo = arrayInfo+" "+info[i];
		}
		return (name+arrayInfo);
	}
}





///***
// * Copyright (C) 2010 Johan Henriksson
// * This code is under the Endrov / BSD license. See www.endrov.net
// * for the full text and how to cite.
// */
//package endrov.recording.positionsWindow;
//
//import java.awt.Color;
//import java.io.Serializable;
//
//import endrov.basicWindow.EvColor;
//
///**
// * 
// */
//public class Position implements Serializable{
//	
//	private static final long serialVersionUID = 1L;
//	
//	private double x;
//	private double y;
//	private double z;
//	private EvColor color;
//	private String name;
//
//	
//	
//	Position(double x, double y, double z){
//		this.x = x;
//		this.y = y;
//		this.z = z;
//		this.color = new EvColor("White", 1, 1, 1, 1);
//
//
//	}
//	
//	public double getX(){
//		return x;
//	}
//	
//	public double getY(){
//		return y;
//	}
//	
//	public double getZ(){
//		return z;
//	}
//	
//	public EvColor getColor(){
//		return color;
//	}
//
//	public String getName(){
//		return name;
//	}
//	
//	public void setName(String name){
//		this.name = name;
//	}
//	
//	public String toString(){
//		return "(x:"+(int)x+" y:"+(int)y+" z:"+(int)z+")";
//	}
//}
