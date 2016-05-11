package com.zsm.whiteboard.util;

import android.hardware.Camera;

@SuppressWarnings("deprecation")
public class Size {

    /** width of the picture */
    public int width;
    /** height of the picture */
    public int height;
    
    public Size(int w, int h) {
        width = w;
        height = h;
    }
    
    public Size( Camera.Size s ) {
    	width = s.width;
    	height = s.height;
    }

	public int area() {
		return width*height;
	}
	
    /**
     * Compares {@code obj} to this size.
     *
     * @param obj the object to compare this size with.
     * @return {@code true} if the width and height of {@code obj} is the
     *         same as those of this size. {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Size)) {
            return false;
        }
        Size s = (Size) obj;
        return width == s.width && height == s.height;
    }
    
    @Override
    public int hashCode() {
        return width * 32749 + height;
    }
    
	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
}
