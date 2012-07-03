/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.events.log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.xcf.InitializeException;
import net.sf.xcf.XcfManager;
import org.junit.Test;

/**
 *
 * @author iluetkeb
 */
public class TestTools {

    private static final Logger logger = Logger.getLogger(TestTools.class.
            getName());
    private static boolean startedXCF = false;
    private static Process dispatcher;
    private static DispatcherLogThread dispatcherLogThread;

    public synchronized static void ensureMiddlewareRunning() {
        try {
            XcfManager testXM = XcfManager.createXcfManager();
        } catch (InitializeException ex) {
            // couldn't create XCF connection, starting own one
            try {
                logger.log(Level.INFO, "No dispatcher found, starting up a new one");
                dispatcher = Runtime.getRuntime().exec(
                        "/vol/ai/releases/lucid/bin/dispatcher-java.sh");
                dispatcherLogThread = new DispatcherLogThread(dispatcher);
                dispatcherLogThread.start();
                startedXCF = true;
            } catch (IOException ex1) {
                Logger.getLogger(PublisherLoggerTest.class.getName()).
                        log(Level.SEVERE, null, ex1);
            }
        }
    }

    public synchronized static void stopMiddlewareIfStarted() {
        if (startedXCF && dispatcher != null) {
            logger.log(Level.INFO, "Stopping started dispatcher");
            dispatcher.destroy();
            dispatcher = null;
            startedXCF = false;
            dispatcherLogThread.interrupt();
        }
    }

    @Test
    public void doNothing() {
        
    }
    
    private static class DispatcherLogThread extends Thread {

        private final BufferedReader reader;
        private final BufferedReader errorReader;

        public DispatcherLogThread(Process p) throws IOException {
            reader = new BufferedReader(new InputStreamReader(dispatcher.
                    getInputStream()));
            errorReader = new BufferedReader(new InputStreamReader(dispatcher.
                    getErrorStream()));
            // read initial start-up message
            boolean started = false;
            while (!started) {
                logErrors();
                if (reader.ready()) {
                    String line = logOutput();
                    if (line != null && line.contains("nameservice started, accepting requests")) {
                        started = true;
                    }
                }
            }
        }

        protected final void logErrors() throws IOException {
            if (errorReader.ready()) {
                logger.log(Level.WARNING, "Dispatcher: {0}", errorReader.readLine());
            }
        }

        protected final String logOutput() throws IOException {
            if (reader.ready()) {
                String line = reader.readLine();
                logger.log(Level.INFO, "Dispatcher: {0}", line);
                return line;
            }
            return null;
        }

        @Override
        public void run() {
            while (!interrupted()) {
                try {
                    logErrors();
                    logOutput();
                } catch(IOException ex) {
                    logger.log(Level.INFO, "Dispatcher: {0}", ex.getMessage());
                    return;
                }                
            }
        }
    };
}
