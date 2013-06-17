/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.rml.maryspeech;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import marytts.MaryInterface;
import marytts.client.RemoteMaryInterface;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.converters.DOMConverter;
import org.w3c.dom.DOMImplementation;

/**
 *
 * @author mgoerlic
 */
public class ConvertTask implements Runnable {
    private static final Logger logger = Logger.getLogger(ConvertTask.class.getName());
    private static DOMImplementation impl;
    private static ThreadLocal maryInterfaceVar;
    
    private Node maryNode;
    private File destinationFolder;
    private Node timestampNode;
    private MaryInterface maryInterface;
    private SimpleDateFormat formatDate;
    
    public ConvertTask(Node maryNode, Node timestampNode, File destinationFolder) {
        this.maryNode = maryNode;
        this.timestampNode = timestampNode;
        this.destinationFolder = destinationFolder;
        
        this.formatDate = new SimpleDateFormat("YYYY-MM-dd-HH-mm-ss-SSS");
    }
    
    public final MaryInterface getMaryInterface() throws IOException {
        MaryInterface mi;
        
        mi = (MaryInterface) maryInterfaceVar.get();
        
        if(mi == null) {
            logger.log(Level.INFO, "Creating new Mary Interface.");
            mi = new RemoteMaryInterface();

            // TODO: configure as in the scenario
            //mi.setVoice("...");
        
            mi.setLocale(Locale.GERMAN);
            mi.setVoice("bits1-hsmm");
            mi.setInputType("RAWMARYXML");
            
            maryInterfaceVar.set(mi);
        }
        
        return (MaryInterface) maryInterfaceVar.get();
    }
    
    public static void initEnvironment() throws ParserConfigurationException {
        if(ConvertTask.impl == null) {
            ConvertTask.impl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
            maryInterfaceVar = new ThreadLocal();
        }
    }

    public void run() {
        Document maryxml = null;
        
        try {
            maryInterface = getMaryInterface();
            //logger.log(Level.INFO, "Processing mary node.");
            // parse timestamp...
            String timestampString = timestampNode.getValue().trim();
            logger.log(Level.FINE, "Read timestamp: {0}", timestampString);
            Long timestamp = null;
            try {
                timestamp = Long.parseLong(timestampString);
            } catch(NumberFormatException ex) {
                logger.log(Level.SEVERE, "Could not parse timestamp!");
                System.exit(-1);
            }
            Date date = new Date(timestamp);
            String filename = formatDate.format(date) + "-" + timestamp + ".wav";
            String filenameXML = formatDate.format(date) + "-" + timestamp + ".xml";
            File destinationFile = new File(destinationFolder, filename);
            File destinationFileXML = new File(destinationFolder, filenameXML);
            
            ((Element) maryNode).addAttribute(new Attribute("xml:lang", "http://www.w3.org/XML/1998/namespace", "de"));
            
            // process elements
            maryxml = new Document((Element) maryNode.copy());
            
            org.w3c.dom.Document doc = DOMConverter.convert(maryxml, impl);
            
            FileWriter writer = null;
            try {
                writer = new FileWriter(destinationFileXML);
                
                writer.write(maryxml.toXML());
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
            } finally {
                if(writer != null) {
                    try {
                        writer.close();
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            }
            
            logger.log(Level.FINE, "Generating audio file.");
            AudioInputStream in = maryInterface.generateAudio(doc);
            logger.log(Level.FINE, "Writing audio to {0}.", destinationFile.getAbsolutePath());
            OutputStream out = null;
            try {
                out = new FileOutputStream(destinationFile);
                
                AudioSystem.write(in, AudioFileFormat.Type.WAVE, out);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
            } finally {
                if(out != null) {
                    try {
                        out.close();
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            
            if(maryxml != null) {
                System.out.println(maryxml.toXML());
            }
        }
    }
}
