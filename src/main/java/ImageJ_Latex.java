/*
 * To the extent possible under law, the Fiji developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;
import java.net.*;
import java.io.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.PlugIn;

/**
 * ProcessPixels
 *
 * A template for processing each pixel of either
 * GRAY8, GRAY16, GRAY32 or COLOR_RGB images.
 *
 * @author The Fiji Team
 */
public class ImageJ_Latex implements PlugIn {

	public void run(String arg) {


		/* set up gui */
		//String url_string = "http://www.texify.com/img/%5CLARGE%5C%211_b.gif";
		//String input=IJ.getString("latex code","D=\\frac{k_{\\textrm{B}}T}{6\\pi\\eta r}");
		String inputconvert="";
		GenericDialog gd=new GenericDialog("ImageJ_Latex");
		gd.addStringField("latex math expression:", "D=\\frac{k_{\\textrm{B}}T}{6\\pi\\eta r}",20);
		

		/* user input about scaling */
		gd.addCheckbox("Scale the image using the following dimension?",false);
		gd.addNumericField("Width:",300,3);
		gd.addNumericField("Height:",200,3);
		gd.addCheckbox("Preserv aspect ratio", true);
		String[] width_or_height={"width","height"};
		gd.addChoice("Preserve aspect ratio according to:", width_or_height,"width");
		/* end of user input about scaling */
		

		
	
		//gd.addCheckbox("Use current background and foreground colors?",false);

		gd.showDialog();
		//deal with cancel button
		if (gd.wasCanceled()){
			IJ.error("Bye bye!");
			return;
		}

		/* end of setting up GUI*/
		
		/* get user input*/
		String input=gd.getNextString();
		boolean ifScale=gd.getNextBoolean();
		double width=gd.getNextNumber();
		double height=gd.getNextNumber();
		boolean preserve_ratio=gd.getNextBoolean();
		int width_or_height_choice=gd.getNextChoiceIndex();
		
		
		//boolean color_choice=gd.getNextBoolean();
		/* end of getting user input*/
		
		try{
		inputconvert=URLEncoder.encode(input,"UTF-8");
		//IJ.showMessage(inputconvert);
		} catch (UnsupportedEncodingException e1)
		{IJ.error("Your input is not compatible with my understanding of URL encoding.");
		}
		
		/*replace + by space, then replace by space by %20*/
		/*replace works with characters, replaceAll is for strings, but replaceAll("+","%20") doesn't work. Probably because
		+ means something special in the context of string or regular expression*/
		inputconvert=inputconvert.replace("+"," ");
		inputconvert=inputconvert.replaceAll(" ","%20");
		/**/
		
		/*replace %2F by /  */
		/* For some reason, %2F is not treated as / by texify.com, but a plain / works*/
		//IJ.showMessage(inputconvert);
		inputconvert=inputconvert.replaceAll("%2F","/");
		//IJ.showMessage(inputconvert);
		/* */

		String texifybaseurl="http://www.texify.com/img/%5CLARGE%5C%21";
		//String leebase="http://latex.codecogs.com/gif.latex?";
		
		String input_url=texifybaseurl+inputconvert+".gif";


		//IJ.showMessage(input_url);
		ImagePlus good_image=IJ.openImage(input_url);


		
		/* Modify the image*/
		/* Convert to RGB and then back to Byte in order to force the scaling */		
		ImageProcessor good_image_p=good_image.getProcessor();
		ImageProcessor good_image_p2=good_image_p.convertToRGB();
		good_image_p=good_image_p2.convertToByte(true);
		good_image.setProcessor("scaling",good_image_p);

		/* scale ? */
		if (ifScale){
			ImageProcessor good_image_scaled=scale(good_image_p,ifScale,preserve_ratio,width_or_height_choice,width,height);
			good_image.setProcessor("scaled",good_image_scaled);
		}
		

		/* color */
		/*
		if (color_choice) {
		Color fg=Toolbar.getForegroundColor();
		Color bg=Toolbar.getBackgroundColor();
		//ImageProcessor background_p, foreground_p;
		//background_p.setColor(bg);
		//background_p.fill();
		//foreground_p.setColor(fg);
		//foreground_p.fill();
		}
		//good_image_p.threshold(2);
		//good_image.setProcessor("binary",good_image_p);
		*/


		good_image.show();



		/* user input about copy/pastee */
		int[] windowList= WindowManager.getIDList();
		if (windowList!=null){
			//IJ.showMessage("copy");
			GenericDialog copy_gd=new GenericDialog("Copy to existing images");
			String[] windowTitles=new String[windowList.length];
			for (int i=0; i< windowList.length; i++){
				ImagePlus imp=WindowManager.getImage(windowList[i]);
				if (imp!=null)
					windowTitles[i]=imp.getShortTitle();
				else
					windowTitles[i]="untitled";
			}
			copy_gd.addChoice("Copy to:", windowTitles, windowTitles[0]);
			copy_gd.showDialog();
			
			
		/* end of user input about copy/past */


		/* copy/paste */
			int copyTarget=copy_gd.getNextChoiceIndex();
			//Rectangle allROI=Rectangle(ip_toCopy.getWidth(),ip_toCopy.getHeight());
			//good_image.getProcessor().convertToRGB();
			ImagePlus target_ip=WindowManager.getImage(windowList[copyTarget]);
			if (target_ip.isInvertedLut()) IJ.showMessage("target image is using inverted LUT!");
			ImageWindow targetWindow=target_ip.getWindow();
			WindowManager.setCurrentWindow(targetWindow);
			//int[] target_pixels=(int[]) target_ip.getProcessor().getPixels();
			//byte[] target_pixels_sorted=Arrays.sort(target_pixels);
			//Arrays.sort(target_pixels);
			//int target_max=target_pixels[target_pixels.length-1];
			//IJ.showMessage(IJ.d2s((double)target_max));

			
			

			/* convert good_image to target format */
			int target_type=target_ip.getType();
			ImageConverter ic=new ImageConverter(good_image);
			ImageConverter target_ic=new ImageConverter(target_ip);
			ic.setDoScaling(true);
			switch (target_type){
				case ImagePlus.GRAY8: ic.convertToGray8(); break; 
				case ImagePlus.GRAY16: ic.convertToGray16(); break;
				case ImagePlus.GRAY32: ic.convertToGray32(); break;
				case ImagePlus.COLOR_256: ic.convertToRGB(); target_ic.convertToRGB(); IJ.showMessage("I have to convert the target image to RGB"); break;
				case ImagePlus.COLOR_RGB: ic.convertToRGB(); break;
				default: IJ.error("Target image has unsupported type."); return;
			}
			good_image.setRoi(0,0, good_image.getWidth(), good_image.getHeight());
			good_image.copy(false);
			//IJ.showMessage("");			
			

			targetWindow.paste();
		


		/* end of copy/paste */

		}
		
		



		/**/

	}
	
	/* method to scale */
	ImageProcessor scale(ImageProcessor ip, boolean ifScale, boolean preserve_ratio, int width_or_height_choice, double width, double height){

		ip.setInterpolate(true);
		if (ifScale){
		double current_width=ip.getWidth();
		double current_height=ip.getHeight();
		double ratio=1;
		if (preserve_ratio){
			switch (width_or_height_choice) {
				case 0: ratio=width/current_width; break;
				case 1: ratio=height/current_height; break;
			}
		width=current_width*ratio;
		height=current_height*ratio;
		}
		ip=ip.resize((int) width, (int) height);
		//ip.smooth();
		
		}
		return ip;
	}	

	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = ImageJ_Latex.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		//ImagePlus image = IJ.openImage("http://imagej.net/images/clown.jpg");
		//image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
