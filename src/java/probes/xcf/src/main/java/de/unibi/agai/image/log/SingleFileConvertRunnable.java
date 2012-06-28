/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.image.log;

import de.unibi.agai.cis.ImageDecoder;
import de.unibi.agai.cis.ImageProvider;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import net.sf.xcf.event.PublishEvent;
import nu.xom.Document;
import nu.xom.Nodes;

/**
 *
 * @author iluetkeb
 */
public class SingleFileConvertRunnable implements Runnable {
    private static final Logger logger = Logger.getLogger(SingleFileConvertRunnable.class.getName());
    
    private final String baseName;
    private final PublishEvent imageEvent;
    private final long receiveTime = System.currentTimeMillis();
    private final ImageWriter writer;
    private final ImageWriteParam iwp;
    
    public SingleFileConvertRunnable(String baseName, PublishEvent imageEvent, float quality) {
        this.baseName = baseName;
        this.imageEvent = imageEvent;
        writer = ImageIO.getImageWritersByFormatName(
                "jpeg").next();
        iwp = writer.getDefaultWriteParam();
	iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        iwp.setCompressionQuality(quality);
        
    }
    
    @Override
    public void run() {
        try {
            final long timestamp = grabTime(imageEvent.getData().getDocument());
            logger.log(Level.FINER, "Grab time {0}", timestamp);            
            final ImageDecoder decoder = new ImageDecoder();
            final ImageProvider spec = decoder.decode(imageEvent.getData());
            final BufferedImage img = spec.createBufferedImage(null);
            logger.log(Level.FINER, "Decoded image {0}", img);            
            final File imageFile = new File(baseName + "-" + timestamp + ".jpg");
            final ImageOutputStream ios = ImageIO.createImageOutputStream(imageFile);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), iwp);
            ios.close();
            logger.log(Level.FINE, "Wrote image to {0}", imageFile);            
        } catch (Exception ex) {
            Logger.getLogger(SingleFileConvertRunnable.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }
 
     private long grabTime(Document document) {
        Nodes createdNodes = document.query("//CREATED/@value");
        if (createdNodes.size() < 1) {
            return receiveTime;
        }
        final long result = Long.parseLong(createdNodes.get(0).getValue());
	if(result == 0) {
		return receiveTime;
	} else
		return result;
    }
}
