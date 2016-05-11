package com.zsm.whiteboard.camera;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.view.Surface;

import com.zsm.log.Log;
import com.zsm.whiteboard.camera.CameraController.PREVIEW_FORMAT;
import com.zsm.whiteboard.util.Size;

@SuppressWarnings("deprecation")
public class LegacyCamera implements CameraInterface {

	private Camera mCamera;
	private List<Size> mSupportedPreviewSizes;
	private List<Size> mSupportedPictureSizes;

	private int mCameraId;
	private CameraInfo mCameraInfo;
	private CameraControllerCallback mControllerCallback;
	private Camera.AutoFocusCallback mAutoFocusCallback;
	
	private Size mPreviewViewSize;
	private Camera.Size mCameraPreviewSize;
	protected Boolean mPreviewProcessing;
	private PreviewFrameThread mPreviewFrameThread;
	
	private static class TakePictureShutterCallback implements Camera.ShutterCallback {
		// don't do anything here, but we need to implement the callback to get the shutter 
		// sound (at least on Galaxy Nexus and Nexus 7)
		@Override
        public void onShutter() {
			Log.d( "Shutter called to take picture." );
        }
	}
	
	private class PreviewFrameThread extends Thread {
		
		private PreviewCallback mCallback;
		private PREVIEW_FORMAT mFrameFormat;
		private byte[] mFrameData;
		private boolean mFrameDataReady;
		private boolean mKeepRunning;
		
		public PreviewFrameThread( PREVIEW_FORMAT format, int frameByteSize,
								   PreviewCallback cb ) {
			
			mFrameFormat = format;
			mFrameData = new byte[ frameByteSize ];
			mCallback = cb;
			mPreviewProcessing = false;
		}

		@Override
		public void run() {
			mKeepRunning = true;
			while( mKeepRunning ) {
				synchronized( mPreviewProcessing ) {
					if( !mPreviewProcessing && mFrameDataReady ) {
						mCallback.onPreviewFrame(mFrameFormat, mFrameData,
												 mCameraPreviewSize.width,
												 mCameraPreviewSize.height );
						mFrameDataReady = false;
						Log.d( "Preview frame processed." );
					}
				}
				Thread.yield();
			}
		}
		
		public void setFrameData( byte[] data ) {
			synchronized( mPreviewProcessing ) {
				if( !mPreviewProcessing ) {
					System.arraycopy( data, 0, mFrameData, 0, mFrameData.length );
					mFrameDataReady = true;
				}
			}
		}
		
		public void stopPreviewProcess() {
			mKeepRunning = false;
		}
	}

	@Override
	public void open(final int id, final CameraControllerCallback callback ) {
		mCameraId = id;
		mControllerCallback = callback;
		
    	new Thread( new Runnable(){
			@Override
			public void run() {
		        try {
		        	mCamera = Camera.open( id );
		        	mControllerCallback.onOpened(id);
		        } catch ( Exception e ) {
		        	mControllerCallback.onOpenFailed( id, e );
		        }
				
			}
    	} ).start();
    	
        mAutoFocusCallback = new Camera.AutoFocusCallback() {
			@Override
			public void onAutoFocus(boolean success, Camera camera) {
				mControllerCallback.onAutoFocus(success);
				Log.d( "Focus finished. ", "Result", success,
					   "Focus mode", mCamera.getParameters().getFocusMode() );
			}
        };
	}
	
	@Override
	public void close() {
		if( mCamera != null ) {
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
			mSupportedPreviewSizes = null;
			mSupportedPictureSizes = null;
			mCameraInfo = null;
		}
	}

	@Override
	public void setPreviewSurface( Surface s ) {
		throw new RuntimeException( "Surface not supported yet!" );
	}
	
	@Override
	public List<Size> getSupportedPreviewSize() {
		if( mSupportedPreviewSizes == null ) {
			List<Camera.Size> l = mCamera.getParameters().getSupportedPreviewSizes();
			mSupportedPreviewSizes = convertSizeList(l);
		}
		return mSupportedPreviewSizes;
	}

	@Override
	public List<Size> getSupportedCaptureSize() {
		if( mSupportedPictureSizes == null ) {
			List<Camera.Size> l = getParameters().getSupportedPictureSizes();
			mSupportedPictureSizes = convertSizeList(l);
		}
		return mSupportedPictureSizes;
	}
	
	private List<Size> convertSizeList(List<Camera.Size> l) {
		ArrayList<Size> list = new ArrayList<Size>( l.size() );
		for( Camera.Size size : l ) {
			list.add( new Size( size ) );
		}
		
		return list;
	}

	@Override
	public int getSensorOrientation() {
		CameraInfo info = getCameraInfo();
		return info.orientation;
	}

	private CameraInfo getCameraInfo() {
		if( mCameraInfo == null ) {
			mCameraInfo = new CameraInfo();
			Camera.getCameraInfo( mCameraId, mCameraInfo );
		}
		
		return mCameraInfo;
	}

	@Override
	public void setPreviewTexture(SurfaceTexture texture, Size previewViewSize ) throws IOException {
		mCamera.setPreviewTexture(texture);
		mPreviewViewSize = previewViewSize;
	}

	@Override
	public void startPreview(final PREVIEW_FORMAT format, final PreviewCallback cb) {
		
		final int cameraFormat = setPreviewFormat( format );
		
		if( cb == null ) {
			mCamera.setPreviewCallbackWithBuffer(null);
		} else {
			mCameraPreviewSize = getParameters().getPreviewSize();
			float bytesPerPixel = ImageFormat.getBitsPerPixel(cameraFormat)/8.0f;
			int bufferSize
					= (int) (mCameraPreviewSize.width*mCameraPreviewSize.height
								*bytesPerPixel);
			
			byte[] mPreviewBuffer = new byte[bufferSize];
			mCamera.addCallbackBuffer(mPreviewBuffer);
			mPreviewFrameThread = new PreviewFrameThread( format, bufferSize, cb  );
			Camera.PreviewCallback ccb = new Camera.PreviewCallback() {

				@Override
				public void onPreviewFrame(byte[] data, Camera camera) {
					mPreviewFrameThread.setFrameData(data);
					mCamera.addCallbackBuffer(data);
				}
			};
			
			mPreviewFrameThread.start();
			mCamera.setPreviewCallbackWithBuffer(ccb);
			Log.d( "Preview callback installed. Preview frame format set as",
					getParameters().getPreviewFormat() );
		}
		mCamera.startPreview();
	}

	@Override
	public void stopPreview() {
		if( mPreviewFrameThread != null ) {
			mPreviewFrameThread.stopPreviewProcess();
			mPreviewFrameThread = null;
		}
		mCamera.setPreviewCallbackWithBuffer(null);
		mCamera.stopPreview();
	}

	private Camera.Parameters getParameters() {
		return mCamera.getParameters();
	}
	
	private void setCameraParameters(Camera.Parameters parameters) {
		Log.d( "Camera Parameters", parameters );
	    try {
			mCamera.setParameters(parameters);
   			Log.d( "Set parameter OK!");
	    } catch(RuntimeException e) {
	    	// just in case something has gone wrong
   			Log.e( e, "failed to set parameters", parameters );
	    }
	}
	
	private List<Camera.Area> convertToCameraAreaList(
								List<CameraController.Area> areas) {
		
		List<Camera.Area> cameraAreas = new ArrayList<Camera.Area>();
		for(CameraController.Area area : areas) {
			cameraAreas.add(new Camera.Area(area.rect, area.weight));
		}
		
		return cameraAreas;
	}
	
	private List<CameraController.Area> convertToControllerAreaList(
			List<Camera.Area> cameraAreas) {
		if( cameraAreas == null ) {
			return null;
		}
		List<CameraController.Area> areas = new ArrayList<CameraController.Area>();
		for(Camera.Area cameraArea : cameraAreas) {
			areas.add(new CameraController.Area(cameraArea.rect, cameraArea.weight));
		}
		return areas;
	}

	private String getFocusMode() {
        Camera.Parameters parameters = getParameters();
		String focusMode = parameters.getFocusMode();
		
		// getFocusMode() is documented as never returning null,
		// however I've had null pointer exceptions reported in
		// Google Play from the below line (v1.7),
		// on Galaxy Tab 10.1 (GT-P7500), Android 4.0.3 - 4.0.4;
		// HTC EVO 3D X515m (shooteru), Android 4.0.3 - 4.0.4
		return focusMode == null ? "" : focusMode;
	}
	
	@Override
	public void setFocusMode( CameraController.FOCUS_MODE mode ) {
		String cameraMode;
		switch( mode ) {
			case AUTO:
				cameraMode = Camera.Parameters.FOCUS_MODE_AUTO;
				break;
			case CONTINUOUS_PICTURE:
				cameraMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
				break;
			case CONTINUOUS_VIDEO:
				cameraMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
				break;
			case INFINITY:
				cameraMode = Camera.Parameters.FOCUS_MODE_INFINITY;
				break;
			case MACRO:
				cameraMode = Camera.Parameters.FOCUS_MODE_MACRO;
				break;
			default:
				Log.e( "Unsupported mode", mode );
				return;
		}
		
		Camera.Parameters parameters = getParameters();
		parameters.setFocusMode(cameraMode);
		setCameraParameters(parameters);
	}
	
	@Override
	public boolean setFocusAndMeteringArea(List<CameraController.Area> areas) {
		List<Camera.Area> cameraAreas = convertToCameraAreaList(areas);
        return setFocusAndMeterinArea(cameraAreas);
	}

	private boolean setFocusAndMeterinArea(List<Camera.Area> cameraAreas) {
		Camera.Parameters parameters = getParameters();
		String focusMode = getFocusMode();
        if( parameters.getMaxNumFocusAreas() != 0
    		&& ( focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO) 
    			 || focusMode.equals(Camera.Parameters.FOCUS_MODE_MACRO)
    			 || focusMode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
    			 || focusMode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) ) ) {
        	
		    parameters.setFocusAreas(cameraAreas);

		    // also set metering areas
		    if( parameters.getMaxNumMeteringAreas() == 0 ) {
       			Log.d( "Metering areas not supported" );
		    } else {
		    	parameters.setMeteringAreas(cameraAreas);
		    }

		    setCameraParameters(parameters);

		    return true;
        } else if( parameters.getMaxNumMeteringAreas() != 0 ) {
	    	parameters.setMeteringAreas(cameraAreas);

		    setCameraParameters(parameters);
		    
		    return true;
        }
        
        return false;
	}

	@Override
	public void clearFocusAndMetering() {
        Camera.Parameters parameters = getParameters();
        boolean updateParameters = false;
        if( parameters.getMaxNumFocusAreas() > 0 ) {
        	parameters.setFocusAreas(null);
        	updateParameters = true;
        }
        if( parameters.getMaxNumMeteringAreas() > 0 ) {
        	parameters.setMeteringAreas(null);
        	updateParameters = true;
        }
        if( updateParameters ) {
		    setCameraParameters(parameters);
        }
	}
	
	@Override
	public List<CameraController.Area> getFocusAreas() {
        Camera.Parameters parameters = getParameters();
		List<Camera.Area> cameraAreas = parameters.getFocusAreas();
		return convertToControllerAreaList(cameraAreas);
	}

	@Override
	public List<CameraController.Area> getMeteringAreas() {
        Camera.Parameters parameters = getParameters();
		List<Camera.Area> cameraAreas = parameters.getMeteringAreas();
		return convertToControllerAreaList(cameraAreas);
	}

	@Override
	public boolean supportsAutoFocus() {
		String focusMode = getFocusMode();
        if( focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO)
        	|| focusMode.equals(Camera.Parameters.FOCUS_MODE_MACRO) ) {
        	
        	return true;
        }
        return false;
	}
	
	@Override
	public boolean focusIsVideo() {
		String focusMode = getFocusMode();
		boolean focusIsVideo
			  = focusMode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		
		Log.d( "Current focus mode. ", focusMode, "focus is video", focusIsVideo );
		return focusIsVideo;
	}

	@Override
	public void autoFocus() {
        try {
        	mCamera.autoFocus(mAutoFocusCallback);
        } catch(RuntimeException e) {
			// just in case? We got a RuntimeException report here from 1 user on Google Play:
			// 21 Dec 2013, Xperia Go, Android 4.1
			Log.e(e, "Failed to start auto focus!");
			// should call the callback, so the application isn't left waiting 
			// (e.g., when we autofocus before trying to take a photo)
			mControllerCallback.onAutoFocus(false);
		}
	}
	
	@Override
	public void cancelAutoFocus() {
		try {
			mCamera.cancelAutoFocus();
		} catch(RuntimeException e) {
			// had a report of crash on some devices, see comment 
			// at https://sourceforge.net/p/opencamera/tickets/4/ made on 20140520
			Log.d(e, "cancel auto focus failed");
		}
	}
	
	@Override
	public void startAutoFocusAt( Rect displayRect ) {
		Rect screenRect = new Rect();
		screenRect.left = (int) ((((float)displayRect.left)/mPreviewViewSize.width - 0.5)*2000);
		screenRect.right = (int) ((((float)displayRect.right)/mPreviewViewSize.width - 0.5)*2000);
		screenRect.top = (int) ((((float)displayRect.top)/mPreviewViewSize.height - 0.5)*2000);
		screenRect.bottom = (int) ((((float)displayRect.bottom)/mPreviewViewSize.height - 0.5)*2000);
		Camera.Area a = new Camera.Area( screenRect, 1000 );
		
		ArrayList<Camera.Area> list = new ArrayList<Camera.Area>();
		list.add(a);
		setFocusAndMeterinArea( list );
		
		autoFocus();
	}
	
	@Override
	public void takePicture( final CameraController.PICTURE_TYPE imageType ) {
		Camera.ShutterCallback shutter = new TakePictureShutterCallback();
        PictureCallback pictureCallback = new Camera.PictureCallback() {
			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				mControllerCallback.onPictureReady(imageType, data);
				startPreview(null, null);
			}
		};
		
		Camera.PictureCallback jpeg = null;
		Camera.PictureCallback raw = null;
		
		switch( imageType ) {
			case JPEG:
				jpeg = pictureCallback;
				break;
			case RAW:
				raw = pictureCallback;
				break;
			default:
				Log.e( "Unsupported image type", imageType );
				return;
		}
		
		mCamera.takePicture(shutter, raw, jpeg);
	}
	
	@Override
	public void soundShutter( boolean sound ) {
		if (getCameraInfo().canDisableShutterSound) {
		    mCamera.enableShutterSound(sound);
		    Log.d( "Make the shutter sound.", sound );
		}
	}
	
	@Override
	public void setPreviewCallback( final CameraController.PREVIEW_FORMAT format,
									final PreviewCallback cb ) {
		
		if( cb == null ) {
			mCamera.setPreviewCallback(null);
			Log.d( "Preview callback uninstalled" );
			return;
		}
		
		setPreviewFormat(format);
		
		Camera.PreviewCallback ccb = new Camera.PreviewCallback() {
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				Camera.Size size = getParameters().getPreviewSize();
				cb.onPreviewFrame(format, data, size.width, size.height );
			}
		};
		
		mCamera.setPreviewCallback(ccb);
		Log.d( "Preview callback installed. Preview frame format set as",
				getParameters().getPreviewFormat() );
	}

	private int setPreviewFormat(final PREVIEW_FORMAT format) {
		int cameraFormat;
		if( format == null || format == PREVIEW_FORMAT.NV21 ) {
			cameraFormat = ImageFormat.NV21;
		} else {
			RuntimeException e
				= new IllegalArgumentException( 
						"Preview type is not supported:" + format );
			Log.e( e, "Unsupported preview type", format );
			throw e;
		}
		final Camera.Parameters p = getParameters();
		p.setPreviewFormat( cameraFormat );
		setCameraParameters(p);
		
		return cameraFormat;
	}

	@Override
	public void setPreviewFrameProcessing(boolean processing) {
		synchronized( mPreviewProcessing ) {
			mPreviewProcessing = processing;
		}
	}
}
