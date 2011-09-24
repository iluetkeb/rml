/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.events;

/**
 *
 * @author iluetkeb
 */
public class Measure {
    public final long timestampMillis;
    public final String value;
    
    public Measure(String value, long timestampMillis) {
        this.value = value;
        this.timestampMillis = timestampMillis;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
