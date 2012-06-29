/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.unibi.agai.cis;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * Interface to be implemented by rendering surfaces.
 * 
 * @author Ingo Luetkebohle <iluetkeb@techfak.uni-bielefeld.de>
 */
public interface ImageRenderer {
    /** Set displayed image from ImageProvider. This method is recommended,
     * because it allows the Rendered to choose the most efficient transfer
     * format. 
     */
    void setImageFromSpec(ImageProvider spec);

    /** Set displayed image from buffered image. */
    void setImage(BufferedImage i);
    
    /** 
     * Specify whether rendering should keep the aspect ratio of the image. 
     * If no, it well be stretched to the display surface. If yes, borders
     * may occur, if the aspect ratio of surface and source image are different.
     * The default is false.
     */
    public void setKeepAspectRatio(boolean keepAspect);
    
    /**
     * Setter for property imageLocation.
     * @param imageLocation New value of property imageLocation.
     */
    void setImageLocation(URL imageLocation) throws IOException;

    /**
     * Setter for property imageResource.
     * @param imageResource New value of property imageResource.
     */
    void setImageResource(String imageResource) throws IOException;

}
