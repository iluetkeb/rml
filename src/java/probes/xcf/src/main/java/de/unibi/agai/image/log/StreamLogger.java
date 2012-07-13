/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.image.log;

import com.xuggle.xuggler.*;
import de.unibi.agai.cis.ImageDecoder;
import de.unibi.agai.cis.ImageProvider;
import java.io.IOException;
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

    private static final Logger logger = Logger.getLogger(StreamLogger.class.getName());
    private final BlockingQueue<IVideoPicture> queue;
    private final ExecutorService inputProcessing;
    private final XcfManager xm;
    private final Subscriber s;
    private final StreamWriter sw;
    private final ImageDecoder decoder = new ImageDecoder();

    public StreamLogger(String publisherName, String filename, int codec) throws
            InitializeException, NameNotFoundException, IOException {
        inputProcessing = Executors.newFixedThreadPool(4);

        xm = XcfManager.createXcfManager();
        s = xm.createSubscriber(publisherName);

        sw = new StreamWriter(availablePics, filename, codec, 20000);
        queue = sw.getInQueue();
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
                    "Syntax: StreamLogger <publisher-name> <stream-name> [codec] [bitrate]");
            System.exit(-1);
        }


        try {
            int codec = 1;
            if (args.length >= 3) {
                if (args[2].equalsIgnoreCase("H264")) {
                    codec = 1;
                } else if (args[2].equalsIgnoreCase("FFV1")) {
                    codec = 2;
                } else {
                    System.err.println(
                            "Unknown Codec. Using default codec: H264");
                }
            }
            final StreamLogger sl = new StreamLogger(args[0], args[1], codec);

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
