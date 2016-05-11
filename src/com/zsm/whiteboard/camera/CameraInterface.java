package com.zsm.whiteboard.camera;

import java.io.IOException;
import java.util.List;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.view.Surface;

import com.zsm.whiteboard.camera.CameraController.PREVIEW_FORMAT;
import com.zsm.whiteboard.util.Size;

public interface CameraInterface {
	
	void open( int id, CameraControllerCallback callback );

	void close();
	
	List<Size> getSupportedCaptureSize();

	List<Size> getSupportedPreviewSize();

	int getSensorOrientation();

	/**
	 * Set where to display the preview. If neither Surface nor SurfaceTexture set,
	 * the preview callback MUST be set when {@link startPreview} invoked, and in
	 * the callback to make the preview displayed. Otherwise, there will be nowhere
	 * to display the preview.
	 * 
	 * @param s surface to display the preview frames.
	 * 
	 * @see setPreviewTexture, startPreview
	 */
	void setPreviewSurface(Surface s);

	/**
	 * Set where to display the preview. If neither Surface nor SurfaceTexture set,
	 * the preview callback MUST be set when {@link startPreview} invoked, and in
	 * the callback to make the preview displayed. Otherwise, there will be nowhere
	 * to display the preview.
	 * 
	 * @param texture texture to display the preview frames
	 * @param previewViewSize size of the view to display the prieview
	 * @throws IOException
	 * 
	 * @see setPreviewSurface, startPreview
	 */
	void setPreviewTexture(SurfaceTexture texture, Size previewViewSize) throws IOException;

	/**
	 * Start previewing, and set a callback to process the preview frames. If neither
	 * {@link  setPreviewSurface} nor {@link setPreviewTexture} invoked to set where to
	 * display the preview frame, the callback MUST display the frames by itself.
	 * 
	 * @param format the format of the preview frame.
	 * @param cb callback to process the preview frames. When it is null, the callback will
	 * 			be uninstalled.
	 */
	void startPreview(PREVIEW_FORMAT format, PreviewCallback cb);

	void stopPreview();

	public abstract boolean focusIsVideo();

	public abstract boolean supportsAutoFocus();

	public abstract List<CameraController.Area> getMeteringAreas();

	public abstract List<CameraController.Area> getFocusAreas();

	public abstract void clearFocusAndMetering();

	public abstract boolean setFocusAndMeteringArea(List<CameraController.Area> areas);

	public abstract void cancelAutoFocus();

	public abstract void autoFocus();

	public abstract void startAutoFocusAt(Rect displayRect);

	public abstract void takePicture(final CameraController.PICTURE_TYPE imageType);

	public abstract void soundShutter(boolean sound);

	public abstract void setFocusMode(CameraController.FOCUS_MODE mode);

	public abstract void setPreviewCallback(final CameraController.PREVIEW_FORMAT type, final PreviewCallback cb);

	void setPreviewFrameProcessing(boolean processing);

}
