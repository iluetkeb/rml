/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.image.log;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.*;
import de.unibi.agai.cis.ImageDecoder;
import de.unibi.agai.cis.ImageProvider;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.xcf.InitializeException;
import net.sf.xcf.Subscriber;
import net.sf.xcf.XcfManager;
import net.sf.xcf.event.PublishEvent;
import net.sf.xcf.event.PublishEventAdapter;
import net.sf.xcf.event.PublishEventListener;
import net.sf.xcf.naming.NameNotFoundException;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 *
 * @author iluetkeb
 */
public class StreamLogger implements Runnable {

    private static final Logger logger = Logger.getLogger(StreamLogger.class.
            getName());
    private final PriorityBlockingQueue<IVideoPicture> queue;
    private final ExecutorService inputProcessing;
    private final XcfManager xm;
    private final Subscriber s;
    private final StreamWriter sw;
    private final ImageDecoder decoder = new ImageDecoder();

    public StreamLogger(String publisherName, String filename) throws
            InitializeException, NameNotFoundException, IOException {
        queue =
                new PriorityBlockingQueue<IVideoPicture>(100,
                new Comparator<IVideoPicture>() {

                    public int compare(IVideoPicture o1, IVideoPicture o2) {
                        return (int) Math.signum(o1.getTimeStamp() - o2.
                                getTimeStamp());
                    }
                });
        inputProcessing = Executors.newFixedThreadPool(4);

        xm = XcfManager.createXcfManager();
        s = xm.createSubscriber(publisherName);

        sw = new StreamWriter(queue, filename, 20000);

    }

    public void run() {
        PublishEventListener l = new PublishEventAdapter() {

            @Override
            public void handleEvent(PublishEvent event) {
                new ImageConverter(queue, event).run();
            }
        };
        s.addListener(l);

        final Thread t = new Thread(sw);
        t.start();

        try {
            t.join();
        } catch (InterruptedException ex) {
            logger.log(Level.INFO, "Shutting down orderly.");
            s.removeListener(l);
            t.interrupt();
            try {
                t.join();
            } catch (InterruptedException ex2) {
                logger.log(Level.WARNING,
                        "Regular shutdown interrupted, terminating prematurely");
            }
        }
    }

    protected void shutdown(Thread t) {
        logger.log(Level.INFO, "Received SIGTERM, shutting down.");
        t.interrupt();
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 2) {
            System.err.println(
                    "Syntax: StreamLogger <publisher-name> <stream-name> [bitrate]");
            System.exit(-1);
        }


        try {
            final StreamLogger sl = new StreamLogger(args[0], args[1]);

            System.out.println("Registering image listener on " + args[0]);

            final Thread t = new Thread(sl);

            SignalHandler h = new SignalHandler() {
                public void handle(Signal sig) {
                    logger.log(Level.INFO, "Signal {0}", sig.getName());
                    sl.shutdown(t);
                }
            };
            Signal.handle(new Signal("INT"), h);
            Signal.handle(new Signal("TERM"), h);


            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    sl.shutdown(t);
                }
            });

            t.start();
            Thread.yield();

            System.out.println("Waiting for termination signal.");
            t.join();
            System.out.println("Stream coding done.");
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

    private class StreamWriter implements Runnable {

        private final BlockingQueue<IVideoPicture> picQueue;
        private final IMediaWriter writer;
        private boolean streamSetupDone = false;
        private long startTime = 0, lastSeen = 0;

        public StreamWriter(BlockingQueue<IVideoPicture> evQueue,
                String filename,
                int bitrate) throws IOException {
            this.picQueue = evQueue;

            writer = ToolFactory.makeWriter(filename);
        }

        public void run() {
            try {
                picloop:
                while (true) {
                    // let the priority queue do its sorting by keeping something
                    // in it at all times
                    if (picQueue.size() < 30) {
                        Thread.sleep(100);
                        continue picloop;
                    }
                    encodeNext();
                }
            } catch (InterruptedException ex) {
                logger.log(Level.INFO, "Shutting down 1: Consuming backlog");
                try {
                    while (!picQueue.isEmpty()) {
                        encodeNext();
                    }
                } catch (InterruptedException ex1) {
                    logger.log(Level.WARNING, "Backlog encoding has been " +
                            "interrupted. Last few images from the stream " +
                            "will be missing");
                }
                logger.log(Level.INFO, "Shutting down 2: Closing container");
                writer.close();
            }
        }

        protected void encodeNext() throws InterruptedException {
            IVideoPicture frame = picQueue.take();
            setupCoder(frame);
            // move original millisecond timestamp by start offset and make it
            // microseconds then
            long timestamp = (frame.getTimeStamp() - startTime) * 1000;
            logger.log(Level.FINER, "Timestamp: {0}", timestamp);
            checkLastSeen(timestamp);
            frame.setTimeStamp(timestamp);
            frame.setQuality(0);
            writer.encodeVideo(0, frame);

            // attempt to provide this frame for reuse
            availablePics.offer(frame);
        }

        protected final void checkLastSeen(long timestamp) {
            if (timestamp < lastSeen) {
                logger.log(Level.WARNING, "Out of order timestamp: {0} < {1}",
                        new Object[]{timestamp, lastSeen});
            } else {
                lastSeen = timestamp;
            }
        }

        protected void setupCoder(IVideoPicture img) {
            if (streamSetupDone) {
                return;
            }
            writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, null, img.
                    getWidth(), img.getHeight());
            IStreamCoder coder = writer.getContainer().getStream(0).
                    getStreamCoder();
            coder.setPixelType(img.getPixelType());

            startTime = img.getTimeStamp();
            IMetaData md = writer.getContainer().getMetaData();
            md.setValue("CREATION_TIME", new SimpleDateFormat("yyyy-mm-dd HH:mm:ss").format(new Date(startTime)));
            md.setValue("STREAM_START_TIME", Long.toString(startTime));
            writer.getContainer().setMetaData(md);

            streamSetupDone = true;
        }
    }
    private final BlockingQueue<IVideoPicture> availablePics =
            new ArrayBlockingQueue<IVideoPicture>(10);

    private class ImageConverter implements Runnable {

        private final BlockingQueue<IVideoPicture> outQueue;
        private final PublishEvent evt;

        public ImageConverter(BlockingQueue<IVideoPicture> outQueue,
                PublishEvent evt) {
            this.outQueue = outQueue;
            this.evt = evt;
        }

        public void run() {
            try {
                logger.log(Level.FINE, "Received event {0} for conversion", evt);
                ImageProvider spec = decoder.decode(evt.getData());
                IVideoPicture pic = spec.createPicture(availablePics.poll());
                // make spec available for reclaim immediately
                spec = null;
                outQueue.put(pic);
                logger.log(Level.FINE, "Added pic {0} to encoder queue", pic);
            } catch (InterruptedException ex) {
                Logger.getLogger(StreamLogger.class.getName()).
                        log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(StreamLogger.class.getName()).
                        log(Level.SEVERE, null, ex);
            }
        }
    }
}
