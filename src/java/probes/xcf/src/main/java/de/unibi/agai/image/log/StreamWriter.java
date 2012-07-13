/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.agai.image.log;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IMetaData;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author iluetkeb
 */
public class StreamWriter implements Runnable {

    private static final Logger logger = Logger.getLogger(StreamWriter.class.getName());
    private final BlockingQueue<IVideoPicture> picQueue;
    private final BlockingQueue<IVideoPicture> availablePics;
    private final IMediaWriter writer;
    private boolean streamSetupDone = false;
    private long startTime = 0;
    private long lastSeen = 0;
    private final int minFill;
    private final int codec;

    public StreamWriter(BlockingQueue<IVideoPicture> availablePics, String filename,
            int codec, int bitrate, int minFill) throws IOException {
        this.availablePics = availablePics;
        picQueue =
                new PriorityBlockingQueue<IVideoPicture>(100,
                new Comparator<IVideoPicture>() {

                    public int compare(IVideoPicture o1, IVideoPicture o2) {
                        return (int) Math.signum(o1.getTimeStamp() - o2.getTimeStamp());
                    }
                });
        this.codec = codec;
        this.minFill = minFill;
        writer = ToolFactory.makeWriter(filename);
    }

    public StreamWriter(BlockingQueue<IVideoPicture> availablePics, String filename, int codec,
            int bitrate) throws IOException {
        this(availablePics, filename, codec, bitrate, 30);
    }

    public BlockingQueue<IVideoPicture> getInQueue() {
        return picQueue;
    }

    public void run() {
        try {
            picloop:
            while (true) {
                // let the priority queue do its sorting by keeping something
                // in it at all times
                if (picQueue.size() < minFill) {
                    Thread.sleep(100);
                    continue picloop;
                }
                encodeNext();
            }
        } catch (InterruptedException ex) {
            logger.log(Level.INFO,
                    "Shutting down 1: Consuming backlog");
            try {
                while (!picQueue.isEmpty()) {
                    encodeNext();
                }
            } catch (InterruptedException ex1) {
                logger.log(Level.WARNING,
                        "Backlog encoding has been "
                        + "interrupted. Last few images from the stream "
                        + "will be missing");
            }
            logger.log(Level.INFO,
                    "Shutting down 2: Closing container");
            writer.close();
        }
    }

    protected void encodeNext() throws InterruptedException {
        IVideoPicture frame = picQueue.take();
        setupCoder(frame);
        // move original millisecond timestamp by start offset and make it
        // microseconds then
        long timestamp = (frame.getTimeStamp() - startTime) * 1000;
        logger.log(Level.FINER, "Timestamp: {0}", timestamp);
        checkLastSeen(timestamp);
        frame.setTimeStamp(timestamp);
        frame.setQuality(0);
        writer.encodeVideo(0, frame);
        // attempt to provide this frame for reuse
        availablePics.offer(frame);
    }

    protected final void checkLastSeen(long timestamp) {
        if (timestamp < lastSeen) {
            logger.log(Level.WARNING,
                    "Out of order timestamp: {0} < {1}",
                    new Object[]{timestamp, lastSeen});
        } else {
            lastSeen = timestamp;
        }
    }

    protected void setupCoder(IVideoPicture img) {
        if (streamSetupDone) {
            return;
        }
        switch (codec) {
            case 1:
                writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, null,
                        img.getWidth(), img.getHeight());
                break;
            case 2:
                writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_FFV1, null,
                        img.getWidth(), img.getHeight());
                break;
            default:
                writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, null,
                        img.getWidth(), img.getHeight());
                break;
        }
        writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, null,
                img.getWidth(), img.getHeight());
        IStreamCoder coder = writer.getContainer().getStream(0).getStreamCoder();
        coder.setPixelType(img.getPixelType());
        startTime = img.getTimeStamp();
        IMetaData md = writer.getContainer().getMetaData();
        md.setValue("CREATION_TIME",
                new SimpleDateFormat("yyyy-mm-dd HH:mm:ss").format(new Date(startTime)));
        md.setValue("STREAM_START_TIME", Long.toString(startTime));
        writer.getContainer().setMetaData(md);
        streamSetupDone = true;
    }
}
