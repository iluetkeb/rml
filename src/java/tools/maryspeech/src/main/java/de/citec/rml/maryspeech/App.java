package de.citec.rml.maryspeech;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import marytts.MaryInterface;
import marytts.client.RemoteMaryInterface;
import marytts.exceptions.SynthesisException;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.converters.DOMConverter;
import nux.xom.xquery.StreamingPathFilter;
import nux.xom.xquery.StreamingTransform;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.xom.XOMXPath;
import org.w3c.dom.DOMImplementation;

/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) throws JaxenException, ParserConfigurationException {
        try {
            final DOMImplementation impl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
            final MaryInterface mi = new RemoteMaryInterface();

            // configure as in the scenario
            //mi.setVoice("...");

            Map prefixes = new HashMap();
            StreamingPathFilter filter = new StreamingPathFilter(
                    "*:log/*:record", prefixes);

            final XPath maryXPath = new XOMXPath("//maryxml");
            Builder b = new Builder(filter.createNodeFactory(null,
                    new StreamingTransform() {
                @Override
                public Nodes transform(Element elmnt) {
                    try {
                        Node node = (Node) maryXPath.selectSingleNode(elmnt);
                        if(node != null) {
                            // parse timestamp...

                            // process elements
                            Document maryxml = new Document((Element)node.copy());
                            org.w3c.dom.Document doc = DOMConverter.convert(maryxml, impl);
                            AudioInputStream in = mi.generateAudio(doc);
                            // write out audio, in timestamped wav.file
                        }
                    } catch (JaxenException ex) {
                        Logger.getLogger(App.class.getName()).
                                log(Level.SEVERE, null, ex);
                    } catch (SynthesisException ex) {
                        Logger.getLogger(App.class.getName()).
                                log(Level.SEVERE, null, ex);
                    }
                    return new Nodes();
                }
            }));
        } catch (IOException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
