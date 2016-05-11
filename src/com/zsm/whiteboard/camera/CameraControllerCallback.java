package com.zsm.whiteboard.camera;

public interface CameraControllerCallback {

	void onOpened( int id );

	void onPermissionDenied(int id);

	void onOpenFailed(int id, Exception e);
	
	void onAutoFocus( boolean success );
	
	void onPictureReady( CameraController.PICTURE_TYPE imageType, byte[] pictureData );
}
