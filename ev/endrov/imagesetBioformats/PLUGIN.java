/***
 * Copyright (C) 2010 Johan Henriksson
 * This code is under the Endrov / BSD license. See www.endrov.net
 * for the full text and how to cite.
 */
package endrov.imagesetBioformats;
import endrov.ev.PluginDef;

public class PLUGIN extends PluginDef
	{
	public String getPluginName()
		{
		return "LOCI Bioformats, fileformats";
		}

	public String getAuthor()
		{
		return 
		"LOCI (library)\n" +
		"Johan Henriksson (library binding)";
		}
	
	public boolean systemSupported()
		{
		return true;
		}
	
	public String cite()
		{
		return "";
		}
	
	public String[] requires()
		{
		return new String[]{};
		}
	
	public Class<?>[] getInitClasses()
		{
		return new Class[]{EvIODataBioformats.class};
		}
	
	public boolean isDefaultEnabled(){return true;};
	}
