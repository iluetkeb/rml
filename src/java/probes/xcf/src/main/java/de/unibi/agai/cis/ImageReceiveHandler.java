package de.unibi.agai.cis;

import java.awt.Component;
import java.awt.Dimension;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.xcf.event.PublishEvent;
import net.sf.xcf.event.PublishEventAdapter;
import nu.xom.Document;
import nu.xom.Nodes;

/**
 * Listen to events containing images, decode, render, ask for repaint.
 *
 * @author Ingo Luetkebohle <iluetkeb@techfak.uni-bielefeld.de>
 */
public class ImageReceiveHandler extends PublishEventAdapter {

    private static final int GUESSED_IMAGE_DELAY = 10;
    private static final Logger LOGGER =
            Logger.getLogger(ImageReceiveHandler.class.getName());
    /**
     * Number of the plane to get the size from. Will be ignored by
     * {@link ImageProvider}.
     */
    private static final int PLANE_NUMBER = 0;
    private ImageDecoder decoder = new ImageDecoder();
    private Component display;
    private ImageRenderer renderer;
    private Dimension imageSize = new Dimension();
    private String timestampCreated = "0";

    public ImageReceiveHandler(Component display, ImageRenderer renderer) {
        this.renderer = renderer;
        this.display = display;
    }

    @Override
    public void handleEvent(final PublishEvent param) {
        try {
            grabTime(param.getData().getDocument());
            final ImageProvider spec = decoder.decode(param.getData());
            imageSize.height = spec.getHeight(PLANE_NUMBER);
            imageSize.width = spec.getWidth(PLANE_NUMBER);
            renderer.setImageFromSpec(spec);
            display.repaint();
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.SEVERE, "Image decoding failed.", ex);
        }
    }

    /**
     * Provides the original size of the last received image.
     * @return image size or (0, 0) if no image has been received
     */
    public Dimension getImageSize() {
        return this.imageSize;
    }

    /**
     * Provides the creation time of the last received image.
     * @return e.g. "1968550054" or "0", if no image has been received
     */
    public String getCreated() {
        return this.timestampCreated;
    }

    private void grabTime(Document document) {
        Nodes createdNodes = document.query("//CREATED/@value");
        if (createdNodes.size() < 1) {
            long guessed = System.currentTimeMillis() - GUESSED_IMAGE_DELAY;
            this.timestampCreated = String.valueOf(guessed);
            LOGGER.log(Level.WARNING, "Image has no creation time."
                    + " Setting to (now-{0}) ms: {1}",
                    new Object[]{timestampCreated, GUESSED_IMAGE_DELAY});
            return;
        }
        this.timestampCreated = createdNodes.get(0).getValue();
    }
}
