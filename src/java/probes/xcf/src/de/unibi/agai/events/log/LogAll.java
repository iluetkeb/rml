/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.events.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.xcf.ActiveMemory;
import net.sf.xcf.InitializeException;
import net.sf.xcf.Subscriber;
import net.sf.xcf.XcfManager;
import net.sf.xcf.event.system.AdditionEvent;
import net.sf.xcf.event.system.DeletionEvent;
import net.sf.xcf.event.system.SystemEvent;
import net.sf.xcf.event.system.SystemEventAdapter;
import net.sf.xcf.ice.XCF.Remote.ServerPrx;
import net.sf.xcf.memory.MemoryException;
import net.sf.xcf.naming.EndpointType;
import net.sf.xcf.naming.Finder;
import net.sf.xcf.naming.NameNotFoundException;
import nu.xom.Document;
import nu.xom.Element;

/**
 *
 * @author iluetkeb
 */
public class LogAll extends SystemEventAdapter {

    private static final Logger logger =
            Logger.getLogger(LogAll.class.getName());
    private static final List<String> memoryMethods = Arrays.asList(
            new String[]{"add_index", "attachments", "list_indexes", "modify",
                "query", "status"});
    private final XcfManager xm;
    private final Finder reg;
    private final List<XCFLogger> loggers =
            new CopyOnWriteArrayList<XCFLogger>();
    private final LogWriter lw;

    public LogAll(OutputStream out, XcfManager xm) {
        this.xm = xm;
        reg = xm.getRegistry();
        lw = new LogWriter(out);

        for (Object o : reg.serverList().keySet()) {
            String s = (String) o;
            if (isMemory(s)) {
                addMemory(s);
            } else {
                addServer(s);
            }
        }
        for (Object o : reg.publisherList().keySet()) {
            addPublisher((String)o);
        }
    }
    
    public static void main(String[] args) {
        Document root = new Document(new Element("root"));

        File logFile = null;
        if (args.length == 0) {
            logFile = new File(System.getProperty("user.home"), "xcflog.xml");
        } else {
            logFile = new File(args[0]);
        }
        System.out.println("Logging to " + logFile);

        // TODO: add rotation

        // register listeners
        try {
            XcfManager xm = XcfManager.createXcfManager();
            LogAll logAll = new LogAll(new FileOutputStream(logFile), xm);
            //logAll.checkLogConfig();
            xm.addListener(logAll);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LogAll.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InitializeException ex) {
            Logger.getLogger(LogAll.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    protected final boolean isMemory(String uri) {
        try {
            Thread.sleep(500); // wait for memory to register its methods
        } catch (InterruptedException ex) {
            Logger.getLogger(LogAll.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            ServerPrx proxy = reg.getServer(uri);
            return proxy.getMethods().keySet().containsAll(memoryMethods);
        } catch (NameNotFoundException ex) {
            return false;
        }

    }

    protected final void addMemory(String uri) {
        logger.log(Level.INFO, "Observing new memory {0}", uri);
        try {
            ActiveMemory am = xm.createActiveMemory(uri);
            MemoryLogger ml = new MemoryLogger(lw, uri);
            am.addListener(ml);
            loggers.add(ml);
        } catch (MemoryException ex) {
            Logger.getLogger(LogAll.class.getName()).log(Level.SEVERE, null,
                    ex);
        } catch (InitializeException ex) {
            Logger.getLogger(LogAll.class.getName()).log(Level.SEVERE, null,
                    ex);
        } catch (NameNotFoundException ex) {
            Logger.getLogger(LogAll.class.getName()).log(Level.SEVERE, null,
                    ex);
        }
    }
    
    protected final void addPublisher(String uri) {
        logger.log(Level.INFO, "Observing new publisher {0}", uri);
        try {
            Subscriber s = xm.createSubscriber(uri);
            PublisherLogger pl = new PublisherLogger(lw, uri);
            s.addListener(pl);
            loggers.add(pl);
        } catch (InitializeException ex) {
            Logger.getLogger(LogAll.class.getName()).log(Level.SEVERE, null,
                    ex);
        } catch (NameNotFoundException ex) {
            Logger.getLogger(LogAll.class.getName()).log(Level.SEVERE, null,
                    ex);
        }
    }
    
    protected final void addServer(String uri) {
        logger.log(Level.WARNING, "Observing servers not supported, ignoring server {0}", uri);
    }

    protected void removeLogger(String uri) {
        Iterator<XCFLogger> iter = loggers.iterator();
        while (iter.hasNext()) {
            XCFLogger ml = iter.next();
            if (ml.getURI().equals(uri)) {
                iter.remove();
            }
        }
    }

    @Override
    public void handleEvent(AdditionEvent e) {
        final String uri = e.getUri();
        switch(e.getEndpointType()) {
            case SERVER:
                if (isMemory(uri)) {
                    addMemory(uri);
                } else {
                    addServer(uri);
                }
                break;
            case PUBLISHER:
                addPublisher(uri);
                break;
        }      
    }

    @Override
    public void handleEvent(DeletionEvent e) {
        //logger.log(Level.INFO, "deletion: type={0}, {1}", new Object[]{ e.getEndpointType(), e.toString() });
        final String uri = e.getUri();
        if (e.getEndpointType() == EndpointType.SERVER) {
            if (isMemory(uri)) {
                removeLogger(e.getUri());
            }
        }
    }

    @Override
    public void handleEvent(SystemEvent e) {
        logger.log(Level.INFO, "general: type={0}, name={1}",
                new Object[]{e.getType(), e.getName()});
        if (e instanceof AdditionEvent) {
            handleEvent((AdditionEvent) e);
        } else if (e instanceof DeletionEvent) {
            handleEvent((DeletionEvent) e);
        }
    }
}
