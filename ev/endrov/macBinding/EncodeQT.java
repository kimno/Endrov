/***
 * Copyright (C) 2010 Johan Henriksson
 * This code is under the Endrov / BSD license. See www.endrov.net
 * for the full text and how to cite.
 */
package endrov.macBinding;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import endrov.makeMovie.EvMovieMaker;
import endrov.makeMovie.EvMovieMakerFactory;

/**
 * Encode movies using Quicktime
 * @author Johan Henriksson
 */
public class EncodeQT
	{
	/******************************************************************************************************
	 * Plugin declaration
	 *****************************************************************************************************/
	public static void initPlugin()	{}
	static
		{
		for(final String codec:QTMovieMaker.codecs)
			{
			EvMovieMakerFactory.makers.add(new EvMovieMakerFactory()
			{
				public EvMovieMaker getInstance(File path, int w, int h, String quality) throws Exception
					{
					return new QTMovieMaker(path,w,h,codec,quality);
					}

				public String getName() 
					{
					return "QT: "+codec;
					}

				public String toString()
					{
					return getName();
					}
				
				public List<String> getQualities() 
					{
					return Arrays.asList(QTMovieMaker.qualityStrings);
					}

				public String getDefaultQuality()
					{
					return QTMovieMaker.qualityStrings[2];
					}
				
				
			});
			}
		}
	
	
	
	}
