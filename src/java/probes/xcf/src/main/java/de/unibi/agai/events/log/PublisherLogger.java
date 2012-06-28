/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.events.log;

import net.sf.xcf.event.PublishEvent;
import net.sf.xcf.event.PublishEventAdapter;
import net.sf.xcf.naming.EndpointType;

/**
 *
 * @author iluetkeb
 */
class PublisherLogger extends PublishEventAdapter implements XCFLogger {
    private final LogWriter lw;
    private final String uri;
    
    public PublisherLogger(LogWriter lw, String uri) {
        this.lw = lw;
        this.uri = uri;
    }
    
    @Override
    public void handleEvent(PublishEvent param) {
        lw.log(System.currentTimeMillis(), System.nanoTime(), uri, param.getData().getDocument());
    }

    @Override
    public EndpointType getType() {
        return EndpointType.PUBLISHER;
    }

    @Override
    public String getURI() {
        return uri;
    }
    
}
