package evplugin.lociBioformats;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import evplugin.basicWindow.*;
import evplugin.ev.*;
import evplugin.imageset.*;
import evplugin.metadata.*;
import evplugin.script.Script;

import loci.formats.*;

/**
 * Support for proprietary formats through LOCI Bioformats
 * 
 * @author Johan Henriksson (binding to library only)
 */
public class BioformatsImageset extends Imageset
	{
	/******************************************************************************************************
	 *                               Static                                                               *
	 *****************************************************************************************************/
	public static void initPlugin() {}
	static
		{
	
		Script.addCommand("dbio", new CmdDBIO());
		
		MetadataBasic.extensions.add(new MetadataExtension()
			{
			public void buildOpen(JMenu menu)
				{
				final JMenuItem miLoadBioformats=new JMenuItem("Load other imageset (Bioformats)");
				menu.add(miLoadBioformats);
				
				ActionListener listener=new ActionListener()
					{
					/**
					 * Show dialog for opening a new native imageset
					 */
					public void actionPerformed(ActionEvent e)
						{
						if(e.getSource()==miLoadBioformats)
							{
							JFileChooser chooser = new JFileChooser();
					    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					    chooser.setCurrentDirectory(new File(Imageset.lastImagesetPath));
					    int returnVal = chooser.showOpenDialog(null); //null=window
					    if(returnVal == JFileChooser.APPROVE_OPTION)
					    	{
					    	String filename=chooser.getSelectedFile().getAbsolutePath();
					    	Imageset.lastImagesetPath=chooser.getSelectedFile().getParent();
					    	load(filename);
					    	}
							}
						}

					public void load(String filename)
						{
			    	//doesn't really show, but better than nothing
			    	JFrame loadingWindow=new JFrame(EV.programName); 
			    	loadingWindow.setLayout(new GridLayout(1,1));
			    	loadingWindow.add(new JLabel("Loading imageset"));
			    	loadingWindow.pack();
			    	loadingWindow.setBounds(200, 200, 300, 50);
			    	loadingWindow.setVisible(true);
			    	loadingWindow.repaint();
			    	
			    	try
							{
							Metadata.metadata.add(new BioformatsImageset(filename));
							}
						catch (Exception e)
							{
							evplugin.ev.Log.printError("bioformats", e);
							}
			    	BasicWindow.updateWindows();
			    	loadingWindow.dispose();
						}
					
					};
				miLoadBioformats.addActionListener(listener);
				}
			public void buildSave(JMenu menu, Metadata meta)
				{
				}
			});
		
		
		
		}
	
	
	
	/******************************************************************************************************
	 *                               Instance                                                             *
	 *****************************************************************************************************/

	
	
	/** Path to imageset */
	public String basedir;

	
	IFormatReader imageReader=null;
	
	/**
	 * Open a new recording
	 */
	public BioformatsImageset(String basedir) throws Exception
		{
		this.basedir=basedir;
		this.imageset=(new File(basedir)).getName();
		if(!(new File(basedir)).exists())
			throw new Exception("File does not exist");

		imageReader=new ImageReader();
		imageReader.setId(basedir);
		
		buildDatabase();
		}
	
	

	public File datadir()
		{
		return new File("");
		}

	
	public void saveMeta()
		{
		}
	
	

	
	/**
	 * Scan recording for channels and build a file database
	 */
	public void buildDatabase()
		{
		int numx=imageReader.getSizeX();
		int numy=imageReader.getSizeY();
		int numz=imageReader.getSizeZ();
		int numt=imageReader.getSizeT();
		int numc=imageReader.getSizeC();
		

		System.out.println("# XYZ "+numx+" "+numy+" "+numz+ " T "+numt+" C "+numc);
		
		meta=new ImagesetMeta();

		channelImages.clear();
		if(imageReader.isRGB())
			{
			/////////////// One fat RGB //////////////////////
			for(int channelnum=0;channelnum<numc;channelnum++)
				{
				String channelName="ch"+channelnum;
				ImagesetMeta.Channel mc=meta.getChannel(channelName);
				loadMeta(mc);
	
				//Fill up with image loaders
				Channel c=new Channel(meta.getChannel(channelName));
				channelImages.put(channelName,c);
				for(int framenum=0;framenum<numt;framenum++)
					{
					TreeMap<Integer,ImageLoader> loaderset=new TreeMap<Integer,ImageLoader>();
					for(int slicenum=0;slicenum<numz;slicenum++)
						{
						int effC=0;
						//System.out.println(" "+slicenum+" "+channelnum+" "+framenum);
						loaderset.put(slicenum, new ImageLoaderBioformats(imageReader,imageReader.getIndex(slicenum, effC, framenum), channelnum, ""));
						}
					c.imageLoader.put(framenum, loaderset);
					}
				}
			}
		else
			{
			/////////////// Individual gray-scale images //////////////////////
			for(int channelnum=0;channelnum<numc;channelnum++)
				{
				String channelName="ch"+channelnum;
				ImagesetMeta.Channel mc=meta.getChannel(channelName);
				loadMeta(mc);
	
				//Fill up with image loaders
				Channel c=new Channel(meta.getChannel(channelName));
				for(int framenum=0;framenum<numt;framenum++)
					{
					TreeMap<Integer,ImageLoader> loaderset=new TreeMap<Integer,ImageLoader>();
					for(int slicenum=0;slicenum<numz;slicenum++)
						{
						//System.out.println(" "+slicenum+" "+channelnum+" "+framenum);
						loaderset.put(slicenum, new ImageLoaderBioformats(imageReader,imageReader.getIndex(slicenum, channelnum, framenum), null, ""));
						}
					c.imageLoader.put(framenum, loaderset);
					}
				}
			}
		}

	
	private void loadMeta(ImagesetMeta.Channel mc)
		{
		mc.chBinning=1;
		
		}
	
	/**
	 * Channel - contains methods for building frame database
	 */
	public class Channel extends Imageset.ChannelImages
		{
		public Channel(ImagesetMeta.Channel channelName)
			{
			super(channelName);
			}
		}
	
	
	}
