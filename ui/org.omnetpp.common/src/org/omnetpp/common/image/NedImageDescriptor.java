package org.omnetpp.common.image;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;


/**
 * @author rhornig
 * An ImageDescriptor used in NED editor. Supports coloring and preferred size. 
 *
 */
public class NedImageDescriptor extends ImageDescriptor {

    /**
     * The class whose resource directory contain the file, 
     * or <code>null</code> if none.
     */
    private Class location;

    /**
     * The name of the file.
     */
    private String name;

    private int preferredScale = 100;
    
    private RGB colorization = null;
    
    private int colorizationWeight = 0;
    
    /**
     * Creates a new file image descriptor.
     * The file has the given file name and is located
     * in the given class's resource directory. If the given
     * class is <code>null</code>, the file name must be absolute.
     * <p>
     * Note that the file is not accessed until its
     * <code>getImageData</code> method is called.
     * Specific colorization effects can be added before calling 
     * <code>getImageData</code> calling <code>setColorization</code> and
     * <code>setColorizationWeight</code>. If the image is vector based a 
     * preferred width can be set too with <code>setPreferredWidth</code>
     *</p> 
     *
     * @param clazz class for resource directory, or
     *   <code>null</code>
     * @param filename the name of the file
     */
    NedImageDescriptor(Class clazz, String filename) {
        this.location = clazz;
        this.name = filename;
    }

    NedImageDescriptor(Class clazz, String filename, int preferredScale) {
        this(clazz, filename);
        this.preferredScale = preferredScale;
    }

    public void setColorization(RGB colorization) {
        this.colorization = colorization;
    }

    public void setColorizationWeight(int colorizationWeight) {
        this.colorizationWeight = colorizationWeight;
    }

    /**
     * Sets the preferred scaling factor. If preferredScale < 0 it is treated 
     * as an absolute width parameter
     * @param preferredScale
     */
//    public void setPreferredScale(int preferredScale) {
//        this.preferredScale = preferredScale;
//    }

    /* (non-Javadoc)
     * Method declared on Object.
     */
    public boolean equals(Object o) {
        if (!(o instanceof NedImageDescriptor)) {
            return false;
        }
        NedImageDescriptor other = (NedImageDescriptor) o;
        if (location != null) {
            if (!location.equals(other.location)) {
                return false;
            }
        } else {
            if (other.location != null) {
                return false;
            }
        }
        if (colorization != null) {
            if (!colorization.equals(other.colorization)) {
                return false;
            }
        } else {
            if (other.colorization != null) {
                return false;
            }
        }
        return name.equals(other.name) 
                &&preferredScale == other.preferredScale 
                && colorizationWeight == other.colorizationWeight;
    }

    /* (non-Javadoc)
     * Method declared on ImageDesciptor.
     * Returns null if the image data cannot be read.
     */
    public ImageData getImageData() {
        InputStream in = getStream();
        ImageData result = null;
        if (in != null) {
            try {
                result = new ImageData(in);
            } catch (SWTException e) {
                if (e.code != SWT.ERROR_INVALID_IMAGE)
                    throw e;
                // fall through otherwise
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        // add colorization effect to the image data
        shadeImageData(result, colorization, colorizationWeight);
        result = rescaleImageData(result);
        return result;
    }

    /**
     * Checks whether the specified resource or file behind the descriptor exists
     * @return <code>ture</code> if respurce is present <code>false</code> otherwise
     */
    public boolean canCreate() {
        if(location != null && location.getResourceAsStream(name) == null)
            return false;
        // check if the file exists
        if(location == null && !(new File(name)).exists())
            return false;
        return true;
    }
    
    /**
     * Returns a stream on the image contents.  Returns
     * null if a stream could not be opened.
     * 
     * @return the buffered stream on the file or <code>null</code>
     * if the file cannot be found
     */
    private InputStream getStream() {
        InputStream is = null;

        if (location != null) {
            is = location.getResourceAsStream(name);

        } else {
            try {
                is = new FileInputStream(name);
            } catch (FileNotFoundException e) {
                return null;
            }
        }
        if (is == null)
            return null;
        else
            return new BufferedInputStream(is);
    }


    public int hashCode() {
        int code = name.hashCode();
        if (location != null) {
            code += location.hashCode();
        }
        if (colorization != null) {
            code += colorization.hashCode();
        }
        code += colorizationWeight * 10000 + preferredScale;
        return code;
    }

    /**
     * The <code>NedImageDescriptor</code> implementation of this <code>Object</code> method 
     * returns a string representation of this object which is suitable only for debugging.
     */
    public String toString() {
        return "NedImageDescriptor(location=" + location + ", name=" + name + 
            ", prefScale=" + preferredScale + ", colorize="+colorization+", colorWight="+colorizationWeight+")";
    }

    /**
     * Utility method to shade image data with a wighted color
     * @param data The in-place converted image data
     * @param shade Shading color
     * @param weight The amount of shading 0 - 100
     */
    private static void shadeImageData(ImageData data, RGB shade, int weight) {
        
        if(data == null || shade == null || weight <= 0)
            return;

        int width = data.width;
        int height = data.height;
        PaletteData palette = data.palette;
        if (!palette.isDirect) {
            /* Convert the palette entries */
            RGB [] rgbs = palette.getRGBs();
            for (int i = 0; i < rgbs.length; i++) {
                if (data.transparentPixel != i) {
                    RGB color = rgbs [i];
                    int lum = (2126*color.red + 7152*color.green + 722*color.blue)/10000;
                    color.red = determineShading(color.red, shade.red, weight, lum);
                    color.green = determineShading(color.green, shade.green, weight, lum);
                    color.blue = determineShading(color.blue, shade.blue, weight, lum);
                }
            }
            data.palette = new PaletteData(rgbs);
        } else {
            /* Convert the pixels. */
            int[] scanline = new int[width];
            int redMask = palette.redMask;
            int greenMask = palette.greenMask;
            int blueMask = palette.blueMask;
            int redShift = palette.redShift;
            int greenShift = palette.greenShift;
            int blueShift = palette.blueShift;
            for (int y = 0; y < height; y++) {
                data.getPixels(0, y, width, scanline, 0);
                for (int x = 0; x < width; x++) {
                    int pixel = scanline[x];
                    int red = pixel & redMask;
                    red = (redShift < 0) ? red >>> -redShift : red << redShift;
                    int green = pixel & greenMask;
                    green = (greenShift < 0) ? green >>> -greenShift : green << greenShift;
                    int blue = pixel & blueMask;
                    blue = (blueShift < 0) ? blue >>> -blueShift : blue << blueShift;
                    
                    int lum = (2126*red + 7152*green + 722*blue)/10000;
                    red = determineShading(red, shade.red, weight, lum);
                    green = determineShading(green, shade.green, weight, lum);
                    blue = determineShading(blue, shade.blue, weight, lum);
                    
                    red = (redShift < 0) ? red << -redShift : red >> redShift;
                    red &= redMask;
                    green = (greenShift < 0) ? green << -greenShift : green >> greenShift;
                    green &= greenMask;
                    blue = (blueShift < 0) ? blue << -blueShift : blue >> blueShift;
                    blue &= blueMask;
                    scanline[x] = red | blue | green;
                }
                data.setPixels(0, y, width, scanline, 0);
            }
        }
    }

    // calculate the colorization of a single point
    private static int determineShading(int origColor, int shadeColor, int weight, int lum) {
        int col = (int)((100-weight)*origColor + weight*lum*shadeColor/128.0)/100;
        return col<0 ? 0 : (col>255 ? 255 : col);
    }
    
    /**
     * @param result
     * @param scale
     * @return 
     */
    private ImageData rescaleImageData(ImageData imgData) {
        if (preferredScale==100 || imgData.width==0 || imgData.width==-preferredScale)
            return imgData;
        
        double scaleRatio;
        if (preferredScale < 0)
            // treat as absolute size
            scaleRatio = -(double)preferredScale / imgData.width;
        else
            // treat as relatve size in percent
            scaleRatio = (double)preferredScale / 100;
        
        return ImageConverter.getResampledImageData(imgData, (int)(imgData.width*scaleRatio), (int)(imgData.height*scaleRatio), 1, 1, 0, 0);
    }

}
