/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.image.log;

import com.xuggle.xuggler.*;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import de.unibi.agai.cis.ImageDecoder;
import de.unibi.agai.cis.ImageProvider;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.xcf.InitializeException;
import net.sf.xcf.Subscriber;
import net.sf.xcf.XcfManager;
import net.sf.xcf.event.PublishEvent;
import net.sf.xcf.event.PublishEventAdapter;
import net.sf.xcf.naming.NameNotFoundException;

/**
 *
 * @author iluetkeb
 */
public class StreamLogger {
    private static final Logger logger = Logger.getLogger(StreamLogger.class.getName());
    

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 2) {
            System.err.println(
                    "Syntax: StreamLogger <publisher-name> <stream-name> [bitrate]");
            System.exit(-1);
        }

        final PriorityBlockingQueue<IVideoPicture> queue =
                new PriorityBlockingQueue<IVideoPicture>(100,
                new Comparator<IVideoPicture>() {

                    public int compare(IVideoPicture o1, IVideoPicture o2) {
                        return (int) Math.signum(o1.getTimeStamp() - o2.
                                getTimeStamp());
                    }
                });
        final ExecutorService inputProcessing = Executors.newFixedThreadPool(4);

        try {
            final XcfManager xm = XcfManager.createXcfManager();
            final Subscriber s = xm.createSubscriber(args[0]);
            final String baseName = args[1];

            System.out.println("Registering image listener on " + args[0]);
            final long start = System.currentTimeMillis();
            s.addListener(new PublishEventAdapter() {

                @Override
                public void handleEvent(PublishEvent event) {
                    inputProcessing.submit(new ImageConverter(queue, event));
                }
            });

            StreamWriter sw = null;
            try {
                sw = new StreamWriter(queue, args[1], 20000);
                final Thread t = new Thread(sw);
                t.start();

                Runtime.getRuntime().addShutdownHook(new Thread() {

                    @Override
                    public void run() {
                        t.interrupt();
                    }
                });
                t.join();
            } finally {
                if(sw != null)
                    sw.close();
            }

        } catch (IOException ex) {
            Logger.getLogger(StreamLogger.class.getName()).
                    log(Level.SEVERE, null, ex);
            System.exit(-1);
        } catch (NameNotFoundException ex) {
            Logger.getLogger(StreamLogger.class.getName()).
                    log(Level.SEVERE, null, ex);
            System.exit(-1);
        } catch (InitializeException ex) {
            Logger.getLogger(StreamLogger.class.getName()).
                    log(Level.SEVERE, null, ex);
            System.exit(-1);
        }

        System.exit(0);
    }
    private static final ImageDecoder decoder = new ImageDecoder();
    static IConverter converter = null;

    private static synchronized void setupConverter(BufferedImage img) {
        if (converter != null) {
            return;
        }
        converter = ConverterFactory.createConverter(img,
                IPixelFormat.Type.YUV420P);
    }

    private static class StreamWriter implements Runnable, Closeable {

        private final BlockingQueue<IVideoPicture> picQueue;
        private final IContainer container;
        private final IStream stream;
        private IStreamCoder coder;
        private final int bitrate;

        public StreamWriter(BlockingQueue<IVideoPicture> evQueue,
                String filename,
                int bitrate) throws IOException {
            this.picQueue = evQueue;
            this.bitrate = bitrate;

            container = IContainer.make();
            if (container.open(filename, IContainer.Type.WRITE, null) < 0) {
                throw new IOException("Cannot open output file " + filename);
            }
            stream = container.addNewStream(ICodec.ID.CODEC_ID_H264);
        }

        public void run() {
            try {
                IPacket packet = IPacket.make(1048576);
                picloop:
                while (true) {
                    // let the priority queue do its sorting by keeping something
                    // in it at all times
                    if (picQueue.size() < 5) {
                        Thread.sleep(100);
                        continue picloop;
                    }
                    IVideoPicture frame = picQueue.take();
                    setupCoder(frame);
                    frame.setQuality(0);
                    coder.encodeVideo(packet, frame, 0);
                    System.err.print(".");
                    
                    if (packet.isComplete()) {
                        container.writePacket(packet);
                        packet.reset();
                        logger.log(Level.INFO, "Writing packet");
                    } 
                }
            } catch (InterruptedException ex) {
                logger.log(Level.INFO, "Closing container");
                container.writeTrailer();
                container.close();
            }
        }

        protected void setupCoder(IVideoPicture img) {
            if (coder != null) {
                return;
            }
            coder = stream.getStreamCoder();
            coder.setBitRate(bitrate);
            coder.setBitRateTolerance(9000);
            coder.setPixelType(IPixelFormat.Type.YUV420P); 
            coder.setWidth(img.getWidth());
            coder.setHeight(img.getHeight());
            coder.setTimeBase(IRational.make(3,1));
            
            int ret = coder.open();
            if (ret < 0) {
                throw new RuntimeException("Cannot configure coder: " + ret);
            }

            container.writeHeader();
        }

        public void close() throws IOException {
            container.close();
        }
    }

    private static class ImageConverter implements Runnable {

        private final BlockingQueue<IVideoPicture> outQueue;
        private final PublishEvent evt;

        public ImageConverter(BlockingQueue<IVideoPicture> outQueue,
                PublishEvent evt) {
            this.outQueue = outQueue;
            this.evt = evt;
        }

        public void run() {
            try {
                final ImageProvider spec = decoder.decode(evt.getData());
                final BufferedImage img =
                        convertToType(spec.createBufferedImage(null),
                        BufferedImage.TYPE_3BYTE_BGR);

                setupConverter(img);
                // FIXME: set up proper timestamp
                outQueue.put(converter.toPicture(img, System.currentTimeMillis() *
                        1000));
            } catch (InterruptedException ex) {
                Logger.getLogger(StreamLogger.class.getName()).
                        log(Level.SEVERE, null, ex);
            }
        }

        private BufferedImage convertToType(BufferedImage sourceImage,
                int targetType) {
            BufferedImage image;

            // if the source image is already the target type, return the source image

            if (sourceImage.getType() == targetType) {
                image = sourceImage;
            } // otherwise create a new image of the target type and draw the new
            // image 
            else {
                image = new BufferedImage(sourceImage.getWidth(), sourceImage.
                        getHeight(),
                        targetType);
                image.getGraphics().drawImage(sourceImage, 0, 0, null);
            }

            return image;
        }
    }
}
