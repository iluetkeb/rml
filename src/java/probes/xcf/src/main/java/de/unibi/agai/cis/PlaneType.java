/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.unibi.agai.cis;

import javax.media.opengl.GL;

/**
 *
 * @author ingo
 */
public enum PlaneType {
    UNKNOWN_RAW8, GRAY8, PLANAR_RGB8, PLANAR_YUV8, INTERLEAVED_RGB8;
            
    protected static int[] GL_1L = new int[] { GL.GL_LUMINANCE };
    protected static int[] GL_3L = new int[] { GL.GL_LUMINANCE, GL.GL_LUMINANCE, GL.GL_LUMINANCE };
    protected static int[] GL_1RGB = new int[] { GL.GL_RGB };
    
    public static int[] getGLPlaneTypes(PlaneType type) {
        switch(type) {
            case UNKNOWN_RAW8:
            case GRAY8:
                return GL_1L;
            case PLANAR_RGB8:
            case PLANAR_YUV8:
                return GL_3L;
            case INTERLEAVED_RGB8:
                return GL_1RGB;
            default:
                return GL_1L;
        }
    }

    public static final int getNumPlanes(PlaneType type) {
        return getGLPlaneTypes(type).length;
    }
}
