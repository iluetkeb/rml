/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.events.log;

import com.sun.media.imageio.plugins.tiff.TIFFCompressor;
import com.sun.media.imageio.plugins.tiff.TIFFImageWriteParam;
import java.util.Arrays;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author iluetkeb
 */
public class ImageIOTest {
    
    public ImageIOTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    
    @Test
    public void list() {
        String[] mimeTypes = ImageIO.getWriterMIMETypes();
        System.out.println(Arrays.asList(mimeTypes));
        for(String mime : mimeTypes) {
            Iterator<ImageWriter> iter = ImageIO.getImageWritersByMIMEType(mime);
            while(iter.hasNext()) {
                ImageWriter writer = iter.next();
                if(writer.canWriteSequence()) {
                    ImageWriteParam iwp = writer.getDefaultWriteParam();
                    System.out.println(mime + " " + writer + " " + iwp);
                }
                TIFFImageWriteParam tiwp;                
                TIFFCompressor tc;
                
            }
        }
        
    }
}
