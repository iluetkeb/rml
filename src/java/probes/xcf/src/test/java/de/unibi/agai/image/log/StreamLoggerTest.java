/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.image.log;

import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author iluetkeb
 */
public class StreamLoggerTest {

    public StreamLoggerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of run method, of class StreamLogger.
     */
    @Ignore
    @Test
    public void testRun() {
        System.out.println("run");
        StreamLogger instance = null;
        instance.run();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of main method, of class StreamLogger.
     */
    @Ignore
    @Test
    public void testMain() throws Exception {
        System.out.println("main");
        String[] args = null;
        StreamLogger.main(args);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    @Test
    public void testTimeOrdering() throws InterruptedException {
        PriorityBlockingQueue<IVideoPicture> queue =
                new PriorityBlockingQueue<IVideoPicture>(100,
                new Comparator<IVideoPicture>() {

                    public int compare(IVideoPicture o1, IVideoPicture o2) {
                        return (int) Math.signum(o1.getTimeStamp() - o2.
                                getTimeStamp());
                    }
                });
        IVideoPicture pic1 = IVideoPicture.make(IPixelFormat.Type.NONE,
                640,480 ), pic2 = IVideoPicture.make(IPixelFormat.Type.NONE, 640,
                480);
        pic1.setTimeStamp(0);
        pic2.setTimeStamp(1);
        queue.put(pic2);
        queue.put(pic1);
        
        assertSame(pic1, queue.take());
    }
}
