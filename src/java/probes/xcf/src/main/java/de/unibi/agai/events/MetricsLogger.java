/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.events;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import net.sf.xcf.ActiveMemory;
import net.sf.xcf.InitializeException;
import net.sf.xcf.XcfManager;
import net.sf.xcf.event.MemoryEvent;
import net.sf.xcf.event.MemoryEventAdapter;
import net.sf.xcf.memory.MemoryAction;
import net.sf.xcf.memory.MemoryException;
import net.sf.xcf.naming.NameNotFoundException;
import net.sf.xcf.xml.XPath;
import nu.xom.Document;
import org.jaxen.JaxenException;
import org.jaxen.xom.XOMXPath;

/**
 *
 * @author iluetkeb
 */
public class MetricsLogger {
    private final ActiveMemory am;
    private final DefaultTableModel model;
    private final List<Metric> metrics = new CopyOnWriteArrayList();
    
    public MetricsLogger(String name) throws InitializeException, NameNotFoundException, MemoryException {
        XcfManager xm = XcfManager.createXcfManager();
        this.am = xm.createActiveMemory(name);
        am.addListener(new Listener());
        
        model = new DefaultTableModel(new String[]{"#"}, 0);
    }
    
    public void addMetric(String name, String valueXPath, String timeXPath) {
        try {
            int col = -1;
            synchronized(model) {
                col = model.getColumnCount();
                model.addColumn(name);
            }
            metrics.add(new Metric(new XOMXPath(valueXPath), new XOMXPath(timeXPath), col));
        } catch (JaxenException ex) {
            throw new IllegalArgumentException("One of XPath '" + valueXPath + 
                    "' or '" + timeXPath + "' not valid: "+ ex.getMessage());
                            
        }
    }
    
    public TableModel getMetricsModel() {
        return model;
    }
    
    private final class Listener extends MemoryEventAdapter {
        private final Logger logger = Logger.getLogger(Listener.class.getName());
        
        public Listener() {
            super(MemoryAction.INSERT, new XPath("/*"));
        }
        
        @Override
        public void handleEvent(MemoryEvent e) {
            synchronized(model) {
                final int count = model.getColumnCount();
                if(count == 1)
                    return; // no metrics, just index
                final Object[] data = new Object[count];
                Arrays.fill(data, null);
                boolean haveMeasures = false;
                
                final Document doc = e.getData().getDocument();
                for(Metric m : metrics) {
                    try {                        
                        String val = m.valX.stringValueOf(doc);
                        if(val == null || val.length() == 0)
                            continue;
                        String time = m.timeX.stringValueOf(doc);
                        if(time == null || time.length() == 0) {
                            logger.log(Level.WARNING, "Entry did contain value but timestamp could not be found",
                                    doc.toXML());
                        }
                        System.err.println("val= " + val + ", time=" + time);
                        if(val == null)
                            continue;
                        long timestamp = Long.parseLong(time);
                        data[m.column] = new Measure(val, timestamp);
                        haveMeasures = true;
                    } catch (JaxenException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
                
                if(haveMeasures) {
                    data[0] = model.getRowCount();
                    model.addRow(data);
                }
            }
                        
        }
    }
}
