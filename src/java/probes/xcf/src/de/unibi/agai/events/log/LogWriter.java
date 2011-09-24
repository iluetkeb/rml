/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.events.log;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import nu.xom.Document;

/**
 *
 * @author iluetkeb
 */
class LogWriter {

    private final AtomicLong sequence = new AtomicLong();
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private final PrintWriter w;
    private Future flast;
    
    public LogWriter(OutputStream out) {
        w = new PrintWriter(new OutputStreamWriter(out));

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    writer.shutdown();
                    writer.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    // ignore
                }
                w.println("</xcflog:log>");
                w.close();
            }
        });

        // we use the default charset, because that's what XOM's toXML() method uses.
        w.println("<?xml version=\"1.0\" encoding=\"" + Charset.defaultCharset().
                displayName() + "\"?>");
        // start element
        w.println(
                "<xcflog:log xmlns:xcflog=\"http://opensource.cit-ec.de/xcflogger.dtd\">");
        w.flush();
    }


    public Future log(long millis, long nanos, String name, Document d) {
        Future f = writer.submit(new FormatCallable(millis, nanos, name, d));
        flast = f; // this is not safe in general, but useful for testing
        return f;
    }    
    
    Future getLast() {
        return flast;
    }

    private final class FormatCallable implements Runnable {

        private final long millis, nanos;
        private final Document doc;
        private final String name;

        public FormatCallable(long millis, long nanos, String name, Document doc) {
            this.millis = millis;
            this.nanos = nanos;
            this.name = name;
            this.doc = doc;
        }

        @Override
        public void run() {
            w.println("<xcflog:record>");
            w.print("<xcflog:millis>");
            w.print(millis);
            w.println("</xcflog:millis>");
            w.print("<xcflog:sequence>");
            w.print(sequence.getAndIncrement());
            w.println("</xcflog:sequence>");
            w.print("<xcflog:name>");
            w.print(name);
            w.println("</xcflog:name>");
            w.println("<xcflog:data>");
            w.print(doc.getRootElement().toXML());
            w.println("</xcflog:data>");
            w.println("</xcflog:record>");
            w.flush();
        }
    }
}
