/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.events.log;

import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.xcf.event.MemoryEvent;
import net.sf.xcf.event.MemoryEventAdapter;
import net.sf.xcf.memory.MemoryAction;
import net.sf.xcf.naming.EndpointType;
import net.sf.xcf.xml.XPath;

/**
 *
 * @author iluetkeb
 */
class MemoryLogger extends MemoryEventAdapter implements XCFLogger {
    private final LogWriter lw;
    private final String uri;
    
    public MemoryLogger(LogWriter lw, String uri) {
        super(MemoryAction.ALL, new XPath("/*"));
        this.lw = lw;
        this.uri = uri;
    }
    
    @Override
    public void handleEvent(MemoryEvent e) {        
        lw.log(System.currentTimeMillis(), System.nanoTime(), uri, e.getData().getDocument());
    }
    
    @Override
    public String getURI() {
        return uri;
    }

    @Override
    public EndpointType getType() {
        return EndpointType.SERVER;
    }    
}
