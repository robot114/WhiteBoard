package com.zsm.whiteboard.camera;


public interface CameraUserInterface {

	void cameraOpened( int id );

	void cameraPermissionDenied(int id);

	void cameraOpenFailed(int id, Exception e);
	
	void onAutoFocus( boolean success );
	
	void savePicture( CameraController.PICTURE_TYPE imageType, byte[] data );
}
