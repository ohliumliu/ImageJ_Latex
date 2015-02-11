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
import java.awt.Image;
import javax.imageio.ImageIO;

/* Acknowledgement
1. www.texify.com
2. mimeTeX
3. shuiguan@mitbbs.com
4. http://www.codecogs.com/latex/eqneditor.php
*/

/* To do list
1. More options: background, etc
*/

/* Known problem
1. a_\textrm{b} is wrong, use a_{\textrm{b}}. Need to be like this on www.texify.com itself.
   it is fixed after switching to codecogs engine
2. To insert into indexed color images, need to convert it to RGB first. This shouldn't lose any information.
*/


public class ImageJ_Latex implements PlugIn {

	public void run(String arg) {


		String inputconvert="";
		//Major GUI window
		GenericDialog gd=new GenericDialog("ImageJ_Latex");
		
		//Help message
		gd.addMessage("Input latex math expression below.\n" +
			"The rendered equation will be displayed as a new image.\n" +
			"You will have the choice of copy it to any existing windows.\n" +
			"It is recommended to adjust the size of the rendered image by DPI rather than scaling.");
		//Input field for latex expression. Default is Boltzmann-Einstein relation
		gd.addStringField("latex math expression:", "D=\\frac{k_{\\textrm{B}}T}{6\\pi\\eta r}",20);
		
		

		/* user input about scaling */
		String[] dpiArray = {"50", "80", "100", "110", "120", "150", "200", "300"};
		gd.addChoice("Rendering resolution (DPI)", dpiArray, dpiArray[dpiArray.length-1]);
		
		gd.addCheckbox("Scale the image using the following dimension?",false);
		gd.addNumericField("Width:",300,3);
		gd.addNumericField("Height:",200,3);
		//gd.addStringField("Resoltuion (DPI)", "300", 3);
		gd.addCheckbox("Preserv aspect ratio", true);
		String[] width_or_height={"width","height"};
		gd.addChoice("Preserve aspect ratio according to:", width_or_height,"width");
		/* end of user input about scaling */
		

		
                //TODO
		//gd.addCheckbox("Use current background and foreground colors?",false);

		gd.showDialog();
		//deal with cancel button
		if (gd.wasCanceled()){
			IJ.error("Bye bye!");
			return;
		}
		/* end of setting up GUI*/
		
		/* read user input*/
		String input=gd.getNextString();
		boolean ifScale=gd.getNextBoolean();
		double width=gd.getNextNumber();
		double height=gd.getNextNumber();
		String dpi=dpiArray[gd.getNextChoiceIndex()];
		boolean preserve_ratio=gd.getNextBoolean();
		int width_or_height_choice=gd.getNextChoiceIndex();
		/* end of getting user input*/
		
                /*  Why is it required to convert Latex input to UTF
		 *   1. For version 1-5 of this plugin, www.texify.com was used to render equation.
		 *      The output of this website is an image whose URL contains the UTF-8 encoding of input Latex string.
		 *   2. For version 6 of this plugin,  http://www.codecogs.com/latex/eqneditor.php is used to render equation.    
		 *      Encoding doesn't seem to be a requirement, but it doesn't seem to hurt either.
		 */
		try{
		inputconvert=URLEncoder.encode(input,"UTF-8");
		//IJ.showMessage(inputconvert);
		} catch (UnsupportedEncodingException e1)
		{IJ.error("Your input is not compatible with my understanding of URL encoding.");
		}
		
		// For codecogs, this conversion is not required, but doesn't hurt to have it.
		/*replace + by space, then replace by space by %20*/
		/*replace works with characters, replaceAll is for strings, but replaceAll("+","%20") doesn't work. Probably because
		+ means something special in the context of string or regular expression*/
		inputconvert=inputconvert.replace("+"," ");
		inputconvert=inputconvert.replaceAll(" ","%20");
		/**/
		
		/*replace %2F by /  */
		/* For some reason, %2F is not treated as / by texify.com, but a plain / works
		*  This is not an issue with codecogs
		*/
		inputconvert=inputconvert.replaceAll("%2F","/");

		//String texifybaseurl="http://www.texify.com/img/%5CLARGE%5C%21";
		//String codecogsBase="http://latex.codecogs.com/png.latex?";
		
		String codecogsBase="http://latex.codecogs.com/png.latex?%5Cdpi%7B"+dpi+"%7D%20";
		
		//String input_url=texifybaseurl+inputconvert+".gif";
		String input_url = codecogsBase + inputconvert;


		/* Texify stops working now, switching to latex.codecogs.com
		 * The resulting image from codecogs doesn't have its file name as part of the URL to the image.
		 * ImageJ's import function thus not working. Use Javax imageIO module.
		 * TODO: Check ImageJ's source to see if it can be improved. 
		*/
		Image image = null;
		try {
  		  URL url = new URL(input_url);
   		  image = ImageIO.read(url);
		} catch (IOException e) {
		   IJ.error("I am not able to get output from http://latex.codecogs.com/latex/eqneditor.php.\nPlease check if the site is working.");
		}
		/* The following is not working after texify is down
		ImagePlus good_image=IJ.openImage(input_url);
		*/	
                
                
                /* Process the image
		 *  ImageJ images are ImagePlus objects.
		 *  ImagesPlus has two parts: Image and ImageProcessor.
		 *  Image is the actual data and ImageProcessor is the operation to be applied.
		 */		
		ImagePlus good_image = new ImagePlus();
		good_image.setImage(image);
		//good_image.show();
		
		/* Modify the image*/
		/* Convert to RGB and then back to Byte in order to force the scaling
		 * This part is still not 100% clear to me. Concatenated commands not working the same
		 * way as separate ones.
		 */		
		ImageProcessor good_image_p=good_image.getProcessor();
		//good_image_p.convertToRGB().convertToByte(true);
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



		/* handle user input about copy/pastee */
		int[] windowList= WindowManager.getIDList();
		if (windowList!=null){
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
			
		/* copy/paste */
			int copyTarget=copy_gd.getNextChoiceIndex();
			
			ImagePlus target_ip=WindowManager.getImage(windowList[copyTarget]);
			if (target_ip.isInvertedLut()) IJ.showMessage("target image is using inverted LUT!");
			ImageWindow targetWindow=target_ip.getWindow();
			WindowManager.setCurrentWindow(targetWindow);
			
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
			targetWindow.paste();

		/* end of copy/paste */

		}
		
		



		/**/

	}
	
	/* method to scale */
	ImageProcessor scale(ImageProcessor imgProc, boolean ifScale, boolean preserve_ratio, int width_or_height_choice, double width, double height){

		imgProc.setInterpolate(true);
		if (ifScale){
		double current_width=imgProc.getWidth();
		double current_height=imgProc.getHeight();
		double ratio=1;
		if (preserve_ratio){
			switch (width_or_height_choice) {
				case 0: ratio=width/current_width; break;
				case 1: ratio=height/current_height; break;
			}
		width=current_width*ratio;
		height=current_height*ratio;
		}
		imgProc=imgProc.resize((int) width, (int) height);
		}
		return imgProc;
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
