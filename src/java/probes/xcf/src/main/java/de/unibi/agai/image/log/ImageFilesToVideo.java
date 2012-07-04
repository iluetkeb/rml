/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.image.log;

import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author iluetkeb
 */
public class ImageFilesToVideo {
    private static final Logger logger = Logger.getLogger(ImageFilesToVideo.
            class.getName());
    
    private final File imageDir;
    private final String baseName;
    private final StreamWriter sw;
    private final BlockingQueue<IVideoPicture> availablePics =
            new ArrayBlockingQueue<IVideoPicture>(10);
    private final BlockingQueue<IVideoPicture> feedQueue;
    
    public ImageFilesToVideo(String outputfile, String baseName) throws
            IOException {
        imageDir = new File(baseName).getAbsoluteFile().getParentFile();
        this.baseName = new File(baseName).getName();

        sw = new StreamWriter(availablePics, outputfile, 20000, 10);
        feedQueue = sw.getInQueue();
    }

    protected File[] getInputFiles() {
        File[] imageFiles = imageDir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.startsWith(baseName);
            }
        });
        Arrays.sort(imageFiles, new Comparator<File>() {

            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return imageFiles;
    }
    
    private BufferedImage convertBuf;

    public BufferedImage convertToType(BufferedImage sourceImage,
            int targetType) {
        // if the source image is already the target type, return the source image
        if (sourceImage.getType() == targetType) {
            return sourceImage;
        } else {
            if(convertBuf == null || 
                    convertBuf.getWidth() != sourceImage.getWidth() ||
                    convertBuf.getHeight() != sourceImage.getHeight() ||
                    convertBuf.getType() != targetType)
                convertBuf = new BufferedImage(sourceImage.getWidth(), sourceImage.
                    getHeight(),
                    targetType);
            
            convertBuf.getGraphics().drawImage(sourceImage, 0, 0, null);
        }
        
        return convertBuf;
    }

    public void run() {
        try {
            File[] imageFiles = getInputFiles();

            Thread encodeThread = new Thread(sw);
            encodeThread.start();
            
            IConverter converter = null;
            int count = 0;
            for (File f : imageFiles) {
                try {
                    BufferedImage img = convertToType(ImageIO.read(f),
                        BufferedImage.TYPE_3BYTE_BGR);
                    if(converter == null)
                         converter = ConverterFactory.
                                 createConverter(img, IPixelFormat.Type.YUV420P);
                    feedQueue.put(converter.toPicture(img, getTimestamp(f)));
                    
                    // throttle ourselves when feedQueue gets full
                    if(feedQueue.size() > 100)
                        Thread.sleep(100);
                    
                    // provide some progress output when there is a large 
                    // number of files to convert
                    ++count;
                    if((count % 1000) == 0) {
                        logger.log(Level.INFO, "Fed {0} images to conversion, " +
                                "{1}% complete", new Object[]{count, 
                                    (float)count/(float)imageFiles.length});
                    }
                    
                } catch (InterruptedException ex) {
                    logger.log(Level.INFO, "Interrupted while feeding " +
                            "images, aborting");
                    return;
                } catch (IOException ex) {
                    Logger.getLogger(ImageFilesToVideo.class.getName()).
                            log(Level.SEVERE, null, ex);
                }
            }

            logger.log(Level.INFO, "Finished importing {0} source images, " +
                    "waiting for encoder to finish.", imageFiles.length);
            encodeThread.interrupt();
            encodeThread.join();
        } catch (InterruptedException ex) {
            logger.log(Level.INFO, "Interrupted while waiting for encoder to finish");
        }
    }

    protected long getTimestamp(File f) {
        String stamp = f.getName().replace(baseName, "").split("\\.")[0];
        return Long.parseLong(stamp);
    }
    
    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.err.println("files_to_video <outputfile> <image-basename>");
                System.exit(-1);
            }

            ImageFilesToVideo converter = new ImageFilesToVideo(args[0], args[1]);
            converter.run();
        } catch (IOException ex) {
            Logger.getLogger(ImageFilesToVideo.class.getName()).
                    log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
    }
}
