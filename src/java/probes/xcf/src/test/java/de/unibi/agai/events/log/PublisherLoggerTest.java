/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.events.log;

import net.sf.xcf.event.PublishEventAdapter;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import net.sf.xcf.Subscriber;
import net.sf.xcf.Publisher;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.io.ByteArrayOutputStream;
import net.sf.xcf.InitializeException;
import net.sf.xcf.XcfManager;
import net.sf.xcf.event.PublishEvent;
import net.sf.xcf.naming.EndpointType;
import net.sf.xcf.naming.NameExistsException;
import net.sf.xcf.naming.NameNotFoundException;
import net.sf.xcf.transport.XOPData;
import net.sf.xcf.xml.SyntaxException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author iluetkeb
 */
public class PublisherLoggerTest {

    private XcfManager xm;

    public PublisherLoggerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        TestTools.ensureMiddlewareRunning();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        TestTools.stopMiddlewareIfStarted();
    }

    @Before
    public void setUp() throws InitializeException {
        xm = XcfManager.createXcfManager();
    }

    @After
    public void tearDown() {
        if (xm != null) {
            xm.deactivate();
            xm = null;
        }
    }

    /**
     * Test of handleEvent method, of class PublisherLogger.
     */
    @Test
    public void testHandleEvent() throws SyntaxException, InterruptedException,
            ExecutionException {
        System.out.println("handleEvent");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        LogWriter lw = new LogWriter(out);
        int size = out.size();
        assertTrue(size > 0);
        PublishEvent param = new PublishEvent("foo", false);
        param.setData(XOPData.fromString("<?xml version=\"1.0\"?><foo/>"));
        PublisherLogger instance = new PublisherLogger(lw, "foo");
        instance.handleEvent(param);
        Future f = lw.getLast();
        f.get();
        int newSize = out.size();
        assertTrue(newSize > size);
    }

    /**
     * Test of getType method, of class PublisherLogger.
     */
    @Test
    public void testGetType() {
        System.out.println("getType");
        PublisherLogger instance = new PublisherLogger(null, "foo");
        assertEquals(EndpointType.PUBLISHER, instance.getType());
    }

    /**
     * Test of getURI method, of class PublisherLogger.
     */
    @Test
    public void testGetURI() {
        System.out.println("getURI");
        PublisherLogger instance = new PublisherLogger(null, "foo");
        assertEquals("foo", instance.getURI());
    }

    @Test(timeout = 750)
    public void testRoundtrip() throws NameExistsException, InitializeException,
            SyntaxException, NameNotFoundException, InterruptedException,
            ExecutionException {
        System.out.println("roundtrip");
        final String name = "foo_pub";
        Publisher p = xm.createPublisher(name);
        
        // for some reason, subscriptions don't seem to work when using the same XcfManager
        XcfManager xm2 = XcfManager.createXcfManager();
        Subscriber s = xm2.createSubscriber(name);
        assertEquals(1, p.getSubscriberList().length);
                
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        LogWriter lw = new LogWriter(out);
        int size = out.size();
        assertTrue(size > 0);

        PublisherLogger instance = new PublisherLogger(lw, name);
        s.addListener(instance);
        final Semaphore sema = new Semaphore(1);
        sema.acquire();
        
        s.addListener(new PublishEventAdapter() {

            @Override
            public void handleEvent(PublishEvent param) {
                System.out.println("\tgot event");
                sema.release();
            }

        });
        p.send(XOPData.fromString("<?xml version=\"1.0\"?><foo/>"));

        // wait for result
        System.out.println("\twaiting for permit");
        sema.acquire();
        lw.getLast().get();
        
        assertTrue(out.size() > size);
        xm2.deactivate();
    }
}
