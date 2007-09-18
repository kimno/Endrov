package evplugin.metadata;

import java.io.*;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.jdom.Document;

import evplugin.basicWindow.BasicWindow;

/**
 * Metadata stored in an ordinary XML-file
 * @author Johan Henriksson
 */
public class XmlMetadata extends Metadata
	{
	public File filename=null;
	
	public String getMetadataName()
		{
		if(filename==null)
			return "(Unnamed XML)";
		else
			return filename.getName();
		}
	public String toString()
		{
		return getMetadataName();
		}

	
	public XmlMetadata()
		{
		}
	
	public XmlMetadata(String filename)
		{
		loadXmlMetadata(filename);
		this.filename=new File(filename);
		}
	
	/**
	 * Save metadata. Will present a dialog. Is this a good idea really?
	 */
	public void saveMeta()
		{
		JFileChooser fc=getFileChooser();
		if(filename!=null)
			fc.setSelectedFile(filename);
		else
			fc.setCurrentDirectory(new File(Metadata.lastDataPath));
//			fc.setCurrentDirectory(filename.getParentFile());
		int ret=fc.showSaveDialog(null);
		if(ret==JFileChooser.APPROVE_OPTION)
			{
			Metadata.lastDataPath=fc.getSelectedFile().getParent();
			filename=fc.getSelectedFile();
			if(!filename.getName().endsWith(".xml"))
				filename=new File(filename.getAbsolutePath()+".xml");
			Document document=saveXmlMetadata();
			writeXmlData(document, filename);
			setMetadataModified(false);
			}
		}

	
	/**
	 * Load metadata by showing open dialog
	 */
	public static void loadMeta()
		{
		JFileChooser fc=getFileChooser();
		fc.setCurrentDirectory(new File(Metadata.lastDataPath));
		int ret=fc.showOpenDialog(null);
		if(ret==JFileChooser.APPROVE_OPTION)
			{
			Metadata.lastDataPath=fc.getSelectedFile().getParent();
			File filename=fc.getSelectedFile();
			XmlMetadata m=new XmlMetadata(filename.getAbsolutePath());
			Metadata.addMetadata(m);
			BasicWindow.updateWindows();
			}
		}
	
	
	
	public static JFileChooser getFileChooser()
		{
		JFileChooser fc=new JFileChooser();
		fc.setFileFilter(new FileFilter()
			{
			public boolean accept(File f)
				{
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml");
				}
			public String getDescription()
				{
				return "OST .xml files";
				}
			});
		return fc;
		}
	
	}
