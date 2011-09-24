/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.events.log;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 *
 * @author iluetkeb
 */
public class EmbeddedXMLFormatter extends Formatter {
    private String encoding = "utf-8";
    
    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("<xcflog:record>");
        sb.append("<xcflog:millis>").append(record.getMillis()).append("</xcflog:millis>\n");
        sb.append("<xcflog:sequence>").append(record.getSequenceNumber()).append("</xcflog:sequence>\n");
        sb.append("<xcflog:logger>").append(record.getLoggerName()).append("</xcflog:logger>\n");
        sb.append("<xcflog:level>").append(record.getLevel()).append("</xcflog:level>\n");
        sb.append("<xcflog:class>").append(record.getSourceClassName()).append("</xcflog:class>\n");
        sb.append("<xcflog:method>").append(record.getSourceMethodName()).append("</xcflog:method>\n");
        sb.append("<xcflog:thread>").append(record.getThreadID()).append("</xcflog:thread>\n");
        sb.append("<xcflog:message>").append(record.getMessage()).append("</xcflog:message>\n");
        int count = 0;
        for(Object o : record.getParameters()) {
            sb.append("<xcflog:param num=\"").append(count++).append("\">");
            String value = o.toString();
            // check for xml text parameters and remove the xml header, if any
            if(value.startsWith("<?xml")) {
                String parts[] = value.split("\\?>", 2);
                if(parts.length == 2) {
                    // check encoding
                    if(parts[0].contains(encoding))
                        sb.append(parts[1]);
                    else {
                        sb.append("<![CDATA[").append(value).append("]]>");
                    }
                } else
                    sb.append(parts[0]);
            }
            sb.append("</xcflog:param>");
        }
        
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public String getHead(Handler h) {
        encoding = h.getEncoding();
        return "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n" +
                "<xcflog:log xmlns:xcflog=\"http://opensource.cit-ec.de/xcflog.dtd\">";
    }
    
    @Override 
    public String getTail(Handler h) {
        return "</xcflog:log>";
    }
    
}
