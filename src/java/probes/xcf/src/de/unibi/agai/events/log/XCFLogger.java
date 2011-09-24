/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.events.log;

import net.sf.xcf.naming.EndpointType;

/**
 *
 * @author iluetkeb
 */
interface XCFLogger {
    /** Retrieve the type of object this logger is logging. */
    EndpointType getType();
    /** Retrieve the uri this logger is logging. */
    String getURI();
}
