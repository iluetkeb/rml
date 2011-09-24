/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.events;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import org.jaxen.JaxenException;
import org.jaxen.xom.XOMXPath;

/**
 *
 * @author iluetkeb
 */
public class MetricsHelper {
    private final DefaultComboBoxModel valueXPaths, timeXPaths;
    
    public MetricsHelper() {
        valueXPaths = new DefaultComboBoxModel();
        timeXPaths = new DefaultComboBoxModel();
    }
    
    public ComboBoxModel getValueXPaths() {
        return valueXPaths;
    }
    
    public ComboBoxModel getTimeXPaths() {
        return timeXPaths;
    }
    
    protected boolean checkAndShow(String xpathStr) {
        try {
            new XOMXPath(xpathStr);
            return true;
        } catch(JaxenException ex) {
            JOptionPane.showMessageDialog(null, "XPath " + xpathStr + 
                    " not valid: " + ex.getMessage());
            return false;
        }
    }
    
    public Action createAddAction(final MetricsLogger logger, final JTextField nameField,
            final JComboBox valueBox, final JComboBox timeBox) {
        return new AbstractAction("Add") {

            @Override
            public void actionPerformed(ActionEvent e) {
                String valueXPathStr = (String) valueBox.getSelectedItem();
                String timeXPathStr = (String)timeBox.getSelectedItem();
                
                // check validity
                boolean valid = true;
                valid &= checkAndShow(valueXPathStr);
                valid &= checkAndShow(timeXPathStr);
                if(!valid)
                    return;
                
                // check for new items
                if(valueBox.getSelectedIndex() == -1) {
                    valueXPaths.addElement(valueXPathStr);
                }
                if(timeBox.getSelectedIndex() == -1) {
                    timeXPaths.addElement(timeXPathStr);
                }
                
                logger.addMetric(nameField.getText(), valueXPathStr, timeXPathStr);
            }
            
        };
    }
    
    public void add(Metric m) {
        
    }
}
