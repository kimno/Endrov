package endrov.flow.std;

import java.awt.Color;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.ImageIcon;

import org.jdom.Element;

import endrov.flow.Flow;
import endrov.flow.FlowType;
import endrov.flow.FlowUnit;
import endrov.flow.FlowUnitBasic;
import endrov.flow.FlowUnitDeclarationTrivial;

public class FlowUnitImserv extends FlowUnitBasic
	{
	private static final String metaType="imserv.imserv";

	public static void initPlugin() {}
	static
		{
		Flow.unitDeclarations.add(new FlowUnitDeclarationTrivial("ImServ","ImServ",metaType){
		public FlowUnit createInstance(){return new FlowUnitImserv();}});
		}
	
	public String getBasicShowName()
		{
		return "ImServ";
		}
	public ImageIcon getIcon(){return null;}

	public static Color bgColor=new Color(200,255,200);
	
	public String storeXML(Element e)
		{
		return metaType;
		}

	
	public Color getBackground()
		{
		return bgColor;
		}

	
	
	/** Get types of flows in */
	public SortedMap<String, FlowType> getTypesIn()
		{
		TreeMap<String, FlowType> types=new TreeMap<String, FlowType>();
		types.put("url", null);
		return types;
		}
	/** Get types of flows out */
	public SortedMap<String, FlowType> getTypesOut()
		{
		TreeMap<String, FlowType> types=new TreeMap<String, FlowType>();
		types.put("imserv", null);
		return types;
		}
	
	public void evaluate(Flow flow) throws Exception
	{
	//TODO flowunit
	}

	
	}
