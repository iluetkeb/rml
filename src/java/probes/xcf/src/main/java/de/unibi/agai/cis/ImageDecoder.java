/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.unibi.agai.cis;

import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.xcf.transport.Attachment;
import net.sf.xcf.transport.XOPData;
import nu.xom.Document;
import nu.xom.Element;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.xom.XOMXPath;

/**
 * Decoder class for the image metadata format used by iceWings XCFImageExport.
 * Understands XCF_CONTAINER format (with 5-byte header) and STD format (planar,
 * no header) so far.  It is only tested with 3-plane RGB and YUV images. In
 * 4-plane images, the alpha plane is ignored.
 * 
 * @author Ingo Luetkebohle <iluetkeb@techfak.uni-bielefeld.de>
 */
public class ImageDecoder {
    private final String imageURIAttrName = "uri";
    private final String imageElementName = "IMAGE";
    private final String propElementName = "PROPERTIES";
    private final String widthAttrName = "width";
    private final String heightAttrName = "height";
    private final String colorspaceAttrName = "colorspace";
    private final String formatAttrName = "format";
    
    private final String CS_RGB = "rgb";
    private final String CS_YUV = "yuv";
    private final String CS_GRAY = "gray";

    private static XPath imageXPath;
    static {
        try {
            imageXPath = new XOMXPath("//IMAGE");
        } catch (JaxenException ex) {
            Logger.getLogger(ImageDecoder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /** Create Image Decoder. */
    public ImageDecoder() {
    }
    
    /**
     * Create an ImageProvider from ImageData in the XOP document.
     * 
     * @param xd
     * @return ImageProvider containing the image.
     * @throws IllegalArgumentException When the data contained did not meet 
     *   expectations, e.g. when it does not contain an image.
     */
    public ImageProvider decode(XOPData xd) {
        // find metadata
        try {
            Document d = xd.getDocument();
            Element imageElement = (Element) imageXPath.selectSingleNode(d);
            String uri = imageElement.getAttributeValue(imageURIAttrName);

            // parse rest of metadata
            Element props = imageElement.getFirstChildElement(propElementName);
            final int width     = Integer.parseInt(props.getAttributeValue(widthAttrName));
            final int height    = Integer.parseInt(props.getAttributeValue(heightAttrName));
            int nplanes = 3;
            String colorspace = props.getAttributeValue(colorspaceAttrName);
            if(colorspace == null){
                colorspace = props.getAttributeValue(formatAttrName);
            }
            colorspace = colorspace.trim();

            PlaneType ptype = PlaneType.UNKNOWN_RAW8;
            if(CS_RGB.equals(colorspace)) {
                ptype = PlaneType.PLANAR_RGB8;
            } else if(CS_YUV.equals(colorspace)) {
                ptype = PlaneType.PLANAR_YUV8;
            } else if(CS_GRAY.equals(colorspace)) {
                ptype = PlaneType.GRAY8;
                nplanes = 1;
            }
            if(!(nplanes == 1 || nplanes == 3)) 
                throw new IllegalArgumentException("Expected 1 or 3 planes, not " + nplanes);

            // get attachment
            Attachment imagePacket = xd.getAttachment(uri);
            
            final int expectedSize = width * height * nplanes;
            if(imagePacket.getValue().length == expectedSize) {
                return new ImageProvider(width, height, uri, 
                        imagePacket.getValue(), 0, ptype);
            } else {
                // old-school packet with meta-data header
                return new ImageProvider(width, height, uri, 
                        imagePacket.getValue(), 
                        imagePacket.getValue().length - expectedSize, ptype);
            }
        } catch(Exception ex) {
            // this happens when an element or attribute that is required is 
            // not present
            // TODO: provide information on the name and type
            throw new IllegalArgumentException("Missing element or attribute " +
                    "in document.", ex);
        }
    }
}
