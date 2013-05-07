package de.citec.rml.maryspeech;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.ParserConfigurationException;
import marytts.MaryInterface;
import nu.xom.Builder;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nux.xom.xquery.StreamingPathFilter;
import nux.xom.xquery.StreamingTransform;
import org.jaxen.JaxenException;

/**
 * Hello world!
 *
 */
public class App {
    private static final Logger logger = Logger.getLogger(App.class.getName());
    static final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    //static final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    
    private static Element findElementByName(Element elmnt, String name, int maxdepth, int currentDepth) {
        Element ret = null;
        
        if(currentDepth > maxdepth) {
            return null;
        }
        
        Elements children = elmnt.getChildElements();
        for(int i=0; i<children.size(); i++) {
            if(children.get(i).getLocalName().equals(name)) {
                return children.get(i);
            } else {
                if((ret = findElementByName(children.get(i), name, maxdepth, currentDepth + 1))!=null) {
                    break;
                }
            }
        }
        
        return ret;
    }
    
    public static void processFile(String filename) {
        try {
            Map prefixes = new HashMap();
            StreamingPathFilter filter = new StreamingPathFilter(
                    "*:log/*:record", prefixes);
            
            File inFile = new File(filename);
            logger.log(Level.INFO, "Opened file {0}.", inFile.getAbsolutePath());

            if (inFile.exists()) {
                final File destinationFolder = inFile.getParentFile();

                Builder b;
                b = new Builder(filter.createNodeFactory(null,
                new StreamingTransform() {

                    @Override
                    public Nodes transform(Element elmnt) {
                        Node maryNode = findElementByName(elmnt, "maryxml", Integer.MAX_VALUE, 0);
                        Node timestampNode = findElementByName(elmnt, "millis", 1, 0);


                        if(maryNode != null && timestampNode != null) {
                            ConvertTask task = new ConvertTask(maryNode, timestampNode, destinationFolder);
                            threadPool.submit(task);
                            //task.run();
                        } else {
                            logger.log(Level.FINE, "Could not find mary or timestamp tag in element.");
                        }

                        return new Nodes();
                    }
                }));

                GZIPInputStream is = new GZIPInputStream(new FileInputStream(inFile));
                try {
                    logger.log(Level.INFO, "Starting parsing of file {0}.", inFile.getAbsolutePath());
                    b.build(is);
                    logger.log(Level.INFO, "Finished of file {0}, waiting for tasks to finish.", inFile.getAbsolutePath());
                } catch (ParsingException ex) {
                    logger.log(Level.SEVERE, null, ex);
                } finally {
                    is.close();
                }
            } else {
                logger.log(Level.SEVERE, "File {0} does not exist!", inFile.getAbsolutePath());
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) throws JaxenException, ParserConfigurationException, InterruptedException {
        threadPool.prestartAllCoreThreads();
        ConvertTask.initEnvironment();
        
        logger.log(Level.INFO, "Starting the process...");
        
        for(int i=0; i<args.length; i++) {
            processFile(args[i]);
        }
        
        threadPool.shutdown();
        while(!threadPool.awaitTermination(1, TimeUnit.SECONDS)) {
            logger.log(Level.INFO, "Still waiting, Tasks left: {0}", threadPool.getTaskCount() - threadPool.getCompletedTaskCount());
        }
        logger.log(Level.INFO, "Process finished!");
    }
}
