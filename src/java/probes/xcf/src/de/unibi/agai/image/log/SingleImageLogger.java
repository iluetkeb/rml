/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.image.log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    
    
    public static void main(String args[]) {
        try {
            if(args.length < 2) {
                System.err.println("Syntax: SingleImageLogger <publisher-name> <image-baseName>");
                System.exit(-1);
            }
            
            final ExecutorService exec = Executors.newFixedThreadPool(10);
            final XcfManager xm = XcfManager.createXcfManager();
            final Subscriber s = xm.createSubscriber(args[0]);
            final String baseName = args[1];
            System.out.println("Registering image listener on " + args[0]);
            s.addListener(new PublishEventAdapter() {

                @Override
                public void handleEvent(PublishEvent event) {
                    exec.submit(new ConvertRunnable(baseName, event));
                }
                
            });
        } catch (NameNotFoundException ex) {
            Logger.getLogger(SingleImageLogger.class.getName()).
                    log(Level.SEVERE, null, ex);
        } catch (InitializeException ex) {
            Logger.getLogger(SingleImageLogger.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }
}
