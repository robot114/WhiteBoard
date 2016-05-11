package com.zsm.whiteboard.camera;

public interface PreviewCallback {

	/**
	 * Callback when a preview frame is ready and to be processed in this method. 
	 * {@link CameraController.setPreviewFrameProcessing} MUST be invoked just
	 * before the frame to be processed in the same thread of processing.  When
	 * the process finished, {@link CameraController.setPreviewFrameProcessing} MUST
	 * be invoked in the same thread. It is to notify.
	 * 
	 * @param format format of the frame
	 * @param frameData data of the frame
	 * @param width width of the frame
	 * @param height height of the frame
	 */
	public void onPreviewFrame( CameraController.PREVIEW_FORMAT format,
								byte[] frameData, int width, int height );
}
