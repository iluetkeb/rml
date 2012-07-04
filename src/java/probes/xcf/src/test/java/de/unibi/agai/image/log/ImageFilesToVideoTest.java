/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.image.log;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author iluetkeb
 */
public class ImageFilesToVideoTest {
    
    public ImageFilesToVideoTest() {
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
     * Test of run method, of class ImageFilesToVideo.
     */
    @Test
    public void testRun() throws IOException {
        System.out.println("run");
        ImageFilesToVideo instance = new ImageFilesToVideo("test.mkv", "src/test/resources/images/image-");
        instance.run();
        assertTrue(new File("test.mkv").exists());
    }
    
    @Test
    public void testGetTimestamp() throws IOException {
        System.out.println("getTimestamp");
        ImageFilesToVideo instance = new ImageFilesToVideo("test.mkv", "src/test/resources/images/image-");
        assertEquals(1321884013645L, instance.getTimestamp(new File("src/test/resources/images/image-1321884013645.jpg")));
        
        Date d = new Date(1321884013679L);
        System.out.println(d);
        
        // side test
    }

    /**
     * Test of main method, of class ImageFilesToVideo.
     */
    @Ignore @Test
    public void testMain() {
        System.out.println("main");
        String[] args = null;
        ImageFilesToVideo.main(args);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}
