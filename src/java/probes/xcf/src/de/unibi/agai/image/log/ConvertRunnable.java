/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.image.log;

import de.unibi.agai.cis.ImageDecoder;
import de.unibi.agai.cis.ImageProvider;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import net.sf.xcf.event.PublishEvent;
import nu.xom.Document;
import nu.xom.Nodes;

/**
 *
 * @author iluetkeb
 */
public class ConvertRunnable implements Runnable {
    private static final Logger logger = Logger.getLogger(ConvertRunnable.class.getName());
    
    private final String baseName;
    private final PublishEvent imageEvent;
        
    public ConvertRunnable(String baseName, PublishEvent imageEvent) {
        this.baseName = baseName;
        this.imageEvent = imageEvent;
    }
    
    @Override
    public void run() {
        try {
            final long timestamp = grabTime(imageEvent.getData().getDocument());
            final ImageDecoder decoder = new ImageDecoder();
            final ImageProvider spec = decoder.decode(imageEvent.getData());
            final BufferedImage img = spec.createBufferedImage(null);
            final File imageFile = new File(baseName + "-" + timestamp + ".jpg");
            ImageIO.write(img, "JPEG", imageFile);
            logger.log(Level.FINE, "Wrote image to {0}", imageFile);
        } catch (Exception ex) {
            Logger.getLogger(ConvertRunnable.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }
 
     private long grabTime(Document document) {
        Nodes createdNodes = document.query("//CREATED/@value");
        if (createdNodes.size() < 1) {
            return System.currentTimeMillis();
        }
        return Long.parseLong(createdNodes.get(0).getValue());
    }
}
