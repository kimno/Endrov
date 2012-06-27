package endrov.recording.resolutionConfigWindow;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import net.imglib2.Cursor;
import net.imglib2.Iterator;
import net.imglib2.algorithm.fft.FourierTransform;
import net.imglib2.algorithm.fft.InverseFourierTransform;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.FloatAccess;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.DevUtil;


import endrov.data.EvData;
import endrov.ev.EV;
import endrov.ev.EvLog;
import endrov.ev.EvLogStdout;
import endrov.imageset.BioformatsUtil;
import endrov.imageset.EvPixels;
import endrov.util.Vector2i;




public class ImageDisplacementCorrelation {
	
	
	public static void main(String[] args) {
		
		EvLog.addListener(new EvLogStdout());
		EV.loadPlugins();
		

		try {
			EvPixels imA=new EvPixels(ImageIO.read(new File("/Users/pswadmin/Desktop/b.png")));
			EvPixels imB=new EvPixels(ImageIO.read(new File("/Users/pswadmin/Desktop/a.png")));
			
			
			displacement(imA, imB );
			
			
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IncompatibleTypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		System.exit(0);
		
	}

	public static double[] displacement(EvPixels firstImg, EvPixels secondImg ) throws IncompatibleTypeException{

		int w=firstImg.getWidth();
		int h=firstImg.getHeight();
		float[] firstFloatImg = firstImg.convertToFloat(true).getArrayFloat();
		float[] secondFloatImg = secondImg.convertToFloat(true).getArrayFloat();
		Img<FloatType> imageA = DevUtil.createImageFromArray(firstFloatImg, new long[]{firstImg.getWidth(), firstImg.getHeight()});
		Img<FloatType> imageB = DevUtil.createImageFromArray(secondFloatImg, new long[]{secondImg.getWidth(), secondImg.getHeight()});
		
		if(firstImg.getWidth()!=secondImg.getWidth() || 
				firstImg.getHeight()!=secondImg.getHeight())
			throw new RuntimeException("Images of different size");
	
				
		// FFT on first image
		final FourierTransform< FloatType, ComplexFloatType > procFFTb = new FourierTransform< FloatType, ComplexFloatType >( imageB, new ComplexFloatType() );
        if (!( procFFTb.checkInput() 
        		&& procFFTb.process() ))
        {
        	throw new RuntimeException( "Cannot compute fourier transform: " + procFFTb.getErrorMessage() );
        }
        final Img< ComplexFloatType > bFFT = procFFTb.getResult();

        
        //FFT on second image
        final FourierTransform< FloatType, ComplexFloatType > procFFTa = new FourierTransform< FloatType, ComplexFloatType >( imageA, new ComplexFloatType() );
        if (!( procFFTa.checkInput() 
        		&& procFFTa.process() ))
        {
        	throw new RuntimeException( "Cannot compute fourier transform: " + procFFTa.getErrorMessage() );
        }
        final Img< ComplexFloatType > aFFT = procFFTa.getResult();
        
        // complex invert the kernel
        final ComplexFloatType c = new ComplexFloatType();
        final ComplexFloatType d = new ComplexFloatType();
        final Cursor<ComplexFloatType> cursorKernel=bFFT.cursor();
        final Cursor<ComplexFloatType> cursorImage=aFFT.cursor();
        final Cursor<ComplexFloatType> cursorCorr=bFFT.cursor();
        while(cursorKernel.hasNext())
        {
        	final ComplexFloatType F1=cursorKernel.next();
        	final ComplexFloatType F2=cursorImage.next();
        	final ComplexFloatType C=cursorCorr.next();
        	
        	c.set(F1);
        	d.set(F2);
        	d.complexConjugate();
        	c.mul(d);
        	c.mul(1.0f/c.getPowerFloat());

        	C.set(c);
        }   
        
        // Compute corr in spatial space
        final InverseFourierTransform< FloatType, ComplexFloatType > cIFFT = new InverseFourierTransform< FloatType, ComplexFloatType >( bFFT, new FloatType() );
	    final Img< FloatType > cInverse;
	    if ( cIFFT.checkInput() && cIFFT.process() )
	    	cInverse = cIFFT.getResult();
	    else
	    {
	            System.err.println( "Cannot compute inverse fourier transform: " + cIFFT.getErrorMessage() );
	            return null;
	    }
        
	    //Find dx,dy	    
	    
		/*
	    double sumwx=0;
	    double sumwy=0;
	    double sumw=0;
	    */
	    
		final Cursor<FloatType> loc_cursor = cInverse.localizingCursor();
		float max = 0;
		FloatType newVal;
		double[] maxpos = new double[2];
		
		while(loc_cursor.hasNext()){
			newVal = loc_cursor.next();
			if (max < newVal.get()) {
				max = newVal.get();
//				maxpos[0] = loc_cursor.getIntPosition(0);
//				maxpos[1] = loc_cursor.getIntPosition(1);
				loc_cursor.localize(maxpos);		
				
			}
/*			
			if(newVal.get()>0.3){
			int[] pos = new int[2];
				loc_cursor.localize(pos);
				sumwx+=pos[0]*newVal.get();
				sumwy+=pos[1]*newVal.get();
				sumw+=newVal.get();
			}*/
			
		}
		
		double dx=maxpos[0];
		double dy=maxpos[1];
		
//		dx=sumwx/sumw;
//		dy=sumwy/sumw;
		
		
	    //Wrap around image for negative displacements
	    if(dx>w/2){
	    	dx-=w;
	    	System.out.println("dx problem");
	    }
	    if(dy>h/2){
	    	dy-=h;
		    System.out.println("dy problem");
	    }
	    
	    maxpos[0] = dx;
	    maxpos[1] = dy;

	    //add pixels into a float array
	    int k = 0;
	    float[] cInverseFA = new float[(int) cInverse.size()];
		final Cursor<FloatType> f_cursor = cInverse.localizingCursor();	    
		while(f_cursor.hasNext()){
			cInverseFA[k] = f_cursor.next().get();
			//System.out.println(stuff[k]);
			k++;
		}

	    System.out.println("  "+dx+"   "+dy+"   "+max);

        
	    EvPixels picture = EvPixels.createFromFloat((int)cInverse.dimension(0), (int)cInverse.dimension(1), cInverseFA);
//        int[] bA = blurgh.convertToInt(true).getArrayInt();
        
//        for(int i = 0; i<bA.length;i++){
//        	if(bA[i] != 0)
//        		System.out.println(""+bA[i]);
//        }
        //System.out.println(""+bA[0]);
        
       System.out.println("DONE");
       
       //spara bild
       BufferedImage conv=picture.quickReadOnlyAWT();
       
        return maxpos;
        

	}
	
	
	
	
	
	/*
	public static EvPixels displacementOld(EvPixels firstImg, EvPixels secondImg ){
		//do stuff to turn ev pixel to a img
//		byte[] firstByteImg = firstImg.convertToUByte(true).getArrayUnsignedByte();
//		byte[] secondByteImg = secondImg.convertToUByte(true).getArrayUnsignedByte();
		float[] firstByteImg = firstImg.convertToFloat(true).getArrayFloat();
		float[] secondByteImg = secondImg.convertToFloat(true).getArrayFloat();
//		Image<UnsignedByteType> image = DevUtil.createImageFromArray(firstByteImg, new int[]{firstImg.getWidth(), firstImg.getHeight()});
//		Image<UnsignedByteType> kernel = DevUtil.createImageFromArray(secondByteImg, new int[]{secondImg.getWidth(), secondImg.getHeight()});
		Image<FloatType> image = createImageFromArray(firstByteImg, new int[]{firstImg.getWidth(), firstImg.getHeight()});
		Image<FloatType> kernel = createImageFromArray(secondByteImg, new int[]{secondImg.getWidth(), secondImg.getHeight()});

				
//		Array fe = new Array();
//		Image derp = new Image():
//		FFT<UnsignedByteType> h1;
//		FFT<UnsignedByteType> h2;
//		try {
//			h1 = new FFT<UnsignedByteType>(fftImg1);
//			h2 = new FFT<UnsignedByteType>(fftImg2);
//			
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		// open with LOCI using an ArrayContainer
		final FourierTransform< FloatType, ComplexFloatType > fft = new FourierTransform< FloatType, ComplexFloatType >( kernel, new ComplexFloatType() );
		
        
        if (!( fft.checkInput() 
        		&& fft.process() ))
        {
        	System.err.println( "Cannot compute fourier transform: " + fft.getErrorMessage() );
        	return null;
        }

        final Image< ComplexFloatType > kernelFFT = fft.getResult();

        // complex invert the kernel
        final ComplexFloatType c = new ComplexFloatType();
        for ( final ComplexFloatType t : kernelFFT.createCursor() )
            {
                    c.set( t );
                    t.complexConjugate();
                    c.mul( t );
                    t.div( c );
            }
        
     // compute inverse fourier transform of the kernel
        final InverseFourierTransform< FloatType, ComplexFloatType > kernelIfft = new InverseFourierTransform< FloatType, ComplexFloatType >( kernelFFT, fft );
	    final Image< FloatType > kernelInverse;
	    if ( kernelIfft.checkInput() && kernelIfft.process() )
	            kernelInverse = kernelIfft.getResult();
	    else
	    {
	            System.err.println( "Cannot compute inverse fourier transform: " + kernelIfft.getErrorMessage() );
	            return null;
	    }
        
        
        
     // normalize the kernel
	    NormalizeImageFloat<FloatType> normImage = new NormalizeImageFloat<FloatType>( kernel );
	    	
	    if ( !normImage.checkInput() || !normImage.process() )
	    {
	            System.out.println( "Cannot normalize kernel: " + normImage.getErrorMessage() );
	            return null;
	    }
	
	    kernel.close();
	    kernel = normImage.getResult();
        

	 // display all
        kernel.getDisplay().setMinMax();
        kernel.setName( "kernel" );
//        ImageJFunctions.copyToImagePlus( kernel ).show();

//        kernel.getDisplay().getImage().
//        ImagePlus test = new ImagePlus("kernel");

        kernelInverse.getDisplay().setMinMax();
        kernelInverse.setName( "inverse kernel" );
//        ImageJFunctions.copyToImagePlus( kernelInverse ).show();

        image.getDisplay().setMinMax();
//        ImageJFunctions.copyToImagePlus( image ).show();
        
     // compute fourier convolution
        FourierConvolution<FloatType, FloatType> fourierConvolution = new FourierConvolution<FloatType, FloatType>( image, kernelInverse );

        if ( !fourierConvolution.checkInput() || !fourierConvolution.process() )
        {
                System.out.println( "Cannot compute fourier convolution: " + fourierConvolution.getErrorMessage() );
                return null;
        }

        Image<FloatType> convolved = fourierConvolution.getResult();
        convolved.setName( "("  + fourierConvolution.getProcessingTime() + " ms) Convolution of " + image.getName() );

        convolved.getDisplay().setMinMax();
//        ImageJFunctions.copyToImagePlus( convolved ).show();
        FloatType[] ftemp = convolved.toArray();
        float[] stuff = new float[ftemp.length];
        for(int i=0; i<ftemp.length;i++){
        	stuff[i] = ftemp[i].get();
        	
        }
        
        EvPixels blurgh = EvPixels.createFromFloat(convolved.getDimensions()[0], convolved.getDimensions()[1], stuff);
       
        int[] bA = blurgh.convertToInt(true).getArrayInt();
        
        for(int i = 0; i<bA.length;i++){
        	if(bA[i] != 0)
        		System.out.println(""+bA[i]);
        }
        //System.out.println(""+bA[0]);
        
       System.out.println("DONE");
        
        return blurgh;
//		FourierConvolution<Image<T>, NumericType<S>
//		FourierConvolution<NumericType<T>, NumericType<S>> he = new FourierConvolution<NumericType<T>, NumericType<S>>(image, kernel)
//		Vector2i diffPos = null;
//		
//		return diffPos;
	}*/
//	
//	public class imgTest implements net.imglib2.img 
//	{
//	
//	}

}

