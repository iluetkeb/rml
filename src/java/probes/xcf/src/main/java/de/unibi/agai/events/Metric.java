/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.events;

import org.jaxen.xom.XOMXPath;

/**
 *
 * @author iluetkeb
 */
public class Metric {
    public final XOMXPath valX;
    public final XOMXPath timeX;
    public final int column;

    public Metric(XOMXPath value, XOMXPath time, int column) {
        this.valX = value;
        this.timeX = time;
        this.column = column;
    }
    
}
