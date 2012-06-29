package de.unibi.agai.cis;

import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;

/**
 * Generic description of an image: width, height, pixel organization, 
 * and so on plus helper functions to convert it to various representations.
 * This is basically used to give the user the more-or-less raw image data
 * and provide a means to get it in the desired container.
 * 
 * @author Ingo Luetkebohle <iluetkeb@techfak.uni-bielefeld.de>
 */
public class ImageProvider {
    private final int width, height, nplanes;
    private final int numPixels;
    private final String uri;
    private final byte[] pixelData;
    private final int offset;
    private final PlaneType ptype;
    private final long timestamp;
    
    private static final ColorModel cm;
    
    static {
        cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), 
                false, false, Transparency.OPAQUE, DataBufferByte.TYPE_BYTE);
    }
    
    /** Create a new spec for an image where all planes have the given
     * width and height. 
     * @param width Width of a plane.
     * @param height Height of a plane.
     * @param uri pseudo-unique identifier of the image.
     * @param pixelData Data for the image.
     * @param offset Offset where the data starts in pixelData.
     * @param ptype How pixels are organized in pixelData. 
     */
    public ImageProvider(int width, int height, String uri, 
            byte[] pixelData, int offset, PlaneType ptype, long timestamp) {
        this.width = width;
        this.height = height;
        this.nplanes = PlaneType.getNumPlanes(ptype);
        this.uri = uri;
        this.pixelData = pixelData;
        this.offset = offset;
        this.ptype = ptype;
        this.timestamp = timestamp;
        numPixels = width * height * nplanes;
    }
    
    /** Return (not really) unique identifier of image. */
    public final String getURI() {
        return uri;
    }
    
    /** Return the number of bytes used for the described image.  */
    public final int getTotalBytes() {
        return numPixels;
    }
    /** Return the number of bytes in plane n.*/
    public final int getPlaneNumBytes(int planeNo) {
        return getWidth(planeNo) * getHeight(planeNo);
    }
    /** Returns width of plane n */
    public final int getWidth(int planeNo) {
        return width;
    }
    /** Returns height of plane n */
    public final int getHeight(int planeNo) { 
        return height;
    }
    /** Returns number of planes. */
    public final int getNumPlanes() {
        return nplanes;
    }
    
    public long getCaptureTimeMillis() {
        return timestamp;
    }
    
    public IVideoPicture createPicture(IVideoPicture pic) {
        switch(ptype) {
            case GRAY8:
                return createPictureFromGray(pic);
            case INTERLEAVED_RGB8:
                return createPictureFromRGB(pic);
            case PLANAR_RGB8:
                return createPictureFromPlanarRGB(pic);
            case PLANAR_YUV8:
                return createPictureFromYUV(pic);
            default:
                throw new IllegalStateException("Unsupported plane type " + ptype);
                    
        }
    }
    
    public BufferedImage createBufferedImage(BufferedImage supplied) {
        if(numPixels + offset != pixelData.length)
            throw new IllegalArgumentException("Expected " + (numPixels + offset) + 
                    " bytes but got " + pixelData.length);
                     
        DataBufferByte db = new DataBufferByte(pixelData, numPixels, offset);
        int[] bankIndices = new int[nplanes];
        int[] bandOffsets = new int[nplanes];
        for(int i = 0; i < nplanes; i++) {
            bankIndices[i] = 0;
            bandOffsets[i] = i * width * height;
        }
        return new BufferedImage(cm, WritableRaster.createBandedRaster(db, width, height, width, 
                bankIndices, bandOffsets, null), false, null);
    }

    /** Return one buffer per plane. Re-uses the supplied
     * buffer array, if possible. Note: The array /will/ contain
     * new buffers after this method returns, even if the 
     * the array itself is still the same. */
    public ByteBuffer[] createBuffer(ByteBuffer[] suppliedBuf) {
        ByteBuffer[] result;
        int numPP = getPlaneNumBytes(0);
        
        if(suppliedBuf == null || suppliedBuf.length != nplanes || 
                suppliedBuf[0] == null || 
                suppliedBuf[0].capacity() < numPP) {
            result = new ByteBuffer[nplanes];
        } else {
            result = suppliedBuf;
        }
        
        ByteBuffer fullBuf = ByteBuffer.wrap(pixelData);
        int pos = offset;
        for(int i = 0; i < nplanes; ++i) {
            fullBuf.position(pos);
            pos+=getPlaneNumBytes(i);
            result[i] = fullBuf.slice();                    
        }
        
        return result;
    }

    /** Returns layout of the image. */
    public PlaneType getImageLayout() {
        return ptype;
    }
    
    public boolean isCompatible(ImageProvider spec) {
        return width == spec.width &&
                height == spec.height &&
                ptype == spec.ptype &&
                nplanes == spec.nplanes;
                
    }
    
    protected IVideoPicture configurePic(IVideoPicture pic, IPixelFormat.Type type) {
        IVideoPicture result = pic;
        if(pic == null || pic.getPixelType() != type || 
                pic.getWidth() != width || pic.getHeight() != height)
            // NOTE: we could do this much more efficiently (by passing a
            // reference) if the underlying data would already be in a DIRECT
            // byte buffer. this would require a middleware change, though
            result = IVideoPicture.make(type, width, height);
        
        result.setTimeStamp(getCaptureTimeMillis());
        
        return result;
    }

    private IVideoPicture createPictureFromGray(IVideoPicture pic) {
        pic = configurePic(pic, IPixelFormat.Type.GRAY8);
        ByteBuffer buf = pic.getByteBuffer();
        buf.rewind();
        buf.put(pixelData);
        return pic;
    }

    private IVideoPicture createPictureFromRGB(IVideoPicture pic) {
        pic = configurePic(pic, IPixelFormat.Type.RGB24);
        ByteBuffer buf = pic.getByteBuffer();
        buf.rewind();
        buf.put(pixelData);
        return pic;
    }

    private IVideoPicture createPictureFromPlanarRGB(IVideoPicture pic) {
        // TODO: as xuggler does not support planar RGB natively,
        // this would require us to convert internally
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private IVideoPicture createPictureFromYUV(IVideoPicture pic) {
        pic = configurePic(pic, IPixelFormat.Type.YUV444P);
        ByteBuffer buf = pic.getByteBuffer();
        buf.rewind();
        buf.put(pixelData);
        return pic;
    }
}
