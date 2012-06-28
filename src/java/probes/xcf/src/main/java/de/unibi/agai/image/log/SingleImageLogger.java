/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.image.log;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
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
public class SingleImageLogger {
    private static final Logger logger = Logger.getLogger(SingleImageLogger.class.getName());
    
    private static final int NUM_THREADS = 50;
    private static final int QUEUE_WARN_SIZE = 3*NUM_THREADS;
    
    public static void main(String args[]) {
        try {
            if(args.length < 2) {
                System.err.println("Syntax: SingleImageLogger <publisher-name> <image-baseName> [quality]");
                System.exit(-1);
            }
            float defaultQuality = 0.8f;
            final float quality = args.length > 2 ? Float.parseFloat(args[2]) : defaultQuality;
            
            // create thread pool and warm it up
            final ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_THREADS);
            exec.prestartAllCoreThreads();
            Thread.yield();
            
            final XcfManager xm = XcfManager.createXcfManager();
            final Subscriber s = xm.createSubscriber(args[0]);
            final String baseName = args[1];
            final AtomicInteger count = new AtomicInteger();
            
            System.out.println("Registering image listener on " + args[0]);
            final long start = System.currentTimeMillis();
            s.addListener(new PublishEventAdapter() {
                
                @Override
                public void handleEvent(PublishEvent event) {
		    try {
                    final int size = exec.getQueue().size();
                    final long duration = System.currentTimeMillis() - start;
                    // allow for some start-up backlog, but then warn when 
                    // queue size starts to grow too much
                    if(duration > 500 && size > QUEUE_WARN_SIZE) {
                        logger.log(Level.INFO, "Queue size growing to {0}", size);
                    }
                    exec.submit(new SingleFileConvertRunnable(baseName, event, quality));
                    int cur = count.incrementAndGet();
                    if((cur % 100) == 0) {
                        logger.log(Level.INFO, "Logged {0} images to {1} in {2}ms ({3}/s)", 
                                new Object[]{cur, baseName, duration, (double)cur/(double)(duration/1000)});
                    }
		    } catch(Exception ex) {
            		Logger.getLogger(SingleImageLogger.class.getName()).
                    		log(Level.SEVERE, null, ex);
		    }
                }
                
            });
        } catch (Exception ex) {
            Logger.getLogger(SingleImageLogger.class.getName()).
                    log(Level.SEVERE, null, ex);
        } 
    }
}
