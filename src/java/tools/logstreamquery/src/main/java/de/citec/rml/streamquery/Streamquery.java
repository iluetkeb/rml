package de.citec.rml.streamquery;

import gnu.getopt.Getopt;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.saxon.Configuration;
import net.sf.saxon.query.StaticQueryContext;
import nu.xom.Builder;
import nu.xom.Comment;
import nu.xom.DocType;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.Text;
import nux.xom.io.StreamingSerializer;
import nux.xom.io.StreamingSerializerFactory;
import nux.xom.xquery.StreamingPathFilter;
import nux.xom.xquery.StreamingTransform;
import nux.xom.xquery.XQuery;
import nux.xom.xquery.XQueryException;

/**
 * Hello world!
 *
 */
public class Streamquery implements Runnable {

    public static final String XCFLOG_NS =
            "http://opensource.cit-ec.de/xcflogger.dtd";
    private final XQuery query;
    private final InputStream in;
    private final OutputStream out;

    public Streamquery(String query, InputStream in,
            OutputStream out) throws XQueryException {
        StaticQueryContext context = new StaticQueryContext(new Configuration());
        context.declareActiveNamespace("xcflog", XCFLOG_NS);
        this.query = new XQuery(query, null, context, null);
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        try {
            final StreamingSerializerFactory factory =
                    new StreamingSerializerFactory();
            final StreamingSerializer ser = factory.createXMLSerializer(out,
                    "UTF-8");
            final Element root = new Element("xcflog:log", XCFLOG_NS);
            try {
                ser.writeXMLDeclaration();
                ser.writeStartTag(root);
            } catch (IOException ex) {
                Logger.getLogger(Streamquery.class.getName()).
                        log(Level.SEVERE,
                        "Could not write XML declaration, stopping. {0}", ex);
                return;
            }

            StreamingTransform myTransform = new StreamingTransform() {
                @Override
                public Nodes transform(Element elmnt) {
                    try {
                        Nodes results = query.execute(elmnt).toNodes();

                        try {
                            if (results.size() > 0) {
                                // write output
                                for (int i = 0, len = results.size(); i < len;
                                        ++i) {
                                    Node n = results.get(i);
                                    if (n instanceof Element) {
                                        ser.write((Element) n);
                                    } else if (n instanceof Text) {
                                        ser.write((Text) n);
                                    } else if (n instanceof DocType) {
                                        ser.write((DocType) n);
                                    } else if (n instanceof Comment) {
                                        ser.write((Comment) n);
                                    } else {
                                        Logger.
                                                getLogger(Streamquery.class.
                                                getName()).
                                                log(Level.WARNING, "Do not " +
                                                "recognize type of {0}, not written",
                                                n);
                                    }
                                }
                                ser.write(new Text("\n"));
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(Streamquery.class.getName()).
                                    log(Level.SEVERE, "Could not write " +
                                    "{0}: {1}", new Object[]{elmnt, ex});
                        }
                    } catch (XQueryException ex) {
                        Logger.getLogger(Streamquery.class.getName()).
                                log(Level.SEVERE, null, ex);
                    }
                    return new Nodes(); // mark current element as subject to garbage collection
                }
            };

            Map prefixes = new HashMap();
            prefixes.put("xcflog", XCFLOG_NS);
            Builder builder = new Builder(new StreamingPathFilter(
                    "/xcflog:log/xcflog:record", prefixes)
                    .createNodeFactory(null, myTransform));
            builder.build(in);

            ser.writeEndTag();
            ser.writeEndDocument();
        } catch (ParsingException ex) {
            Logger.getLogger(Streamquery.class.getName()).
                    log(Level.SEVERE, "Error parsing input {0}: {1}",
                    new Object[]{in, ex});
        } catch (IOException ex) {
            Logger.getLogger(Streamquery.class.getName()).
                    log(Level.SEVERE, "IO error parsing input {0}: {1}",
                    new Object[]{in, ex});
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(Streamquery.class.getName()).
                        log(Level.SEVERE,
                        "Could not close output, will be incomplete {0}", ex);
            }
            try {
                in.close();
            } catch (IOException ex) {
                Logger.getLogger(Streamquery.class.getName()).
                        log(Level.SEVERE, "Could not close input {0}", ex);
            }
        }
    }

    public static Streamquery createFromArgs(String[] args) throws IOException, XQueryException {
        String query = null;
        InputStream input = System.in;
        OutputStream output = System.out;

        Getopt g = new Getopt("streamquery", args, "q:f:i:p:o:h");
        //
        int c;
        String arg;

        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'h': // help
                    System.out.printf(
                            "Syntax: logstreamquery [-q 'query' | -f " +
                            "queryfile] [-i inputfile] [-o output]%n" +
                            "If both -q and -f are given, the later one takes precedence.%n" +
                            "%nIf no input argument given, uses stdin. If no " +
                            "output argument given, writes to stdout.%n");
                    return null;
                case 'q':
                    query = g.getOptarg();
                    break;
                case 'f':
                    query = new String(Files.readAllBytes(
                            new File(g.getOptarg()).toPath()));
                    break;
                case 'i':
                    input = new BufferedInputStream(new FileInputStream(g.
                            getOptarg()));
                    break;
                case 'o':
                    output = new BufferedOutputStream(new FileOutputStream(g.
                            getOptarg()));
                    break;
                //
                case '?':
                    break; // getopt() already printed an error
                //
                default:
                    System.out.print("getopt() returned " + c + "\n");
            }
        }

        return new Streamquery(query, input, output);
    }

    public static void main(String[] args) {
        try {
            Streamquery sq = createFromArgs(args);
            if (sq == null) {
                System.err.println(
                        "Could not configure Streamquery from given arguments, exiting.");
                System.exit(-1);
            }
            sq.run();

        } catch (IOException | XQueryException ex) {
            ex.printStackTrace(System.err);
            System.exit(-1);
        }
    }
}
