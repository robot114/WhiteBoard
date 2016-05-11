package com.zsm.whiteboard.camera;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.view.Display;
import android.view.Surface;

import com.zsm.log.Log;
import com.zsm.whiteboard.ui.AutoFitTextureView;
import com.zsm.whiteboard.util.Size;

public class CameraController implements CameraControllerCallback{

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    
	public enum CAMERA_STATE {
    	NOTINITIALIZED, 
    	OPENED,
    	FAILED,
    	PREVIEW_SET,
    	PREVIEWING,
    };
    
    public enum PICTURE_TYPE {
    	JPEG,
    	RAW
    }
    
    public enum PREVIEW_FORMAT {
    	NV21,
    }
    
    public enum FOCUS_MODE {
    	AUTO,
    	CONTINUOUS_PICTURE,
    	CONTINUOUS_VIDEO,
    	INFINITY,
    	MACRO
    }
    
	public static class Area {
		public Rect rect = null;
		public int weight = 0;
		
		public Area(Rect rect, int weight) {
			this.rect = rect;
			this.weight = weight;
		}
	}
	
	private class CompareSizesByArea implements
			Comparator<Size> {

		@Override
		public int compare(Size s1, Size s2) {
			return s1.area() - s2.area();
		}

	}

	private Context mContext;
	private CameraInterface mCamera;

	private Size mPreviewSensorSize;

	private AutoFitTextureView mPreviewView;

	protected CAMERA_STATE mCameraState = CAMERA_STATE.NOTINITIALIZED;

	private Size mPreviewViewSize;

	private CameraUserInterface mCameraUserInterface;

	public CameraController( Context c, CameraInterface camera ) {
		mContext = c;
		mCamera = camera;
	}
	
	public void openCamera( final int id, CameraUserInterface ui ) {
		mCameraUserInterface = ui;
        mCamera.open( id, this );
	}
	
	public void releaseCamera() {
		if( mCamera != null ) {
			mCamera.close();
			mCameraState = CAMERA_STATE.NOTINITIALIZED;
		}
	}

    /**
     * Sets up member variables related to camera. This MUST be invoked when the 
     * preview surface is available. So generally, it is called in 
     * {@code}TextureView.SurfaceTextureListener.onSurfaceTextureAvailable
     *  
     * @param preview Preview view
     * @param viewWidth  The width of available size for camera preview
     * @param viewHeight The height of available size for camera preview
     * @param screenRotation rotation of the screen to the original orientation
     * @param screenSize size of the screen
     */
    public void setUpPreviewView( AutoFitTextureView preview,
    								int viewWidth, int viewHeight,
    								int screenRotation, Size screenSize ) {
    	
    	mPreviewView = preview;
    	mPreviewViewSize = new Size( viewWidth, viewHeight );
    	if( getState() == CAMERA_STATE.OPENED ) {
	    	setupCameraOutputs(preview, screenRotation, screenSize);
    	}
    }

	private void setupCameraOutputs(AutoFitTextureView preview, int screenRotation,
									Size screenSize) {
		
		mPreviewSensorSize
    		= initPreviewSize(mPreviewViewSize.width, mPreviewViewSize.height,
    						  screenRotation, screenSize);

        // We fit the aspect ratio of TextureView to the size of preview we picked.
        int orientation = mContext.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        	preview.setAspectRatio(
                    mPreviewSensorSize.getWidth(), mPreviewSensorSize.getHeight());
        } else {
        	preview.setAspectRatio(
                    mPreviewSensorSize.getHeight(), mPreviewSensorSize.getWidth());
        }

        setPreviewTexture( );
        configureTransform(mPreviewViewSize.width, mPreviewViewSize.height,
        				   screenRotation);
	}

    /**
     * Sets up member variables related to camera. This MUST be invoked when the 
     * preview surface is available. So generally, it is called in 
     * {@code}TextureView.SurfaceTextureListener.onSurfaceTextureAvailable
     *  
     * @param preview Preview view
     * @param viewWidth  The width of available size for camera preview
     * @param viewHeight The height of available size for camera preview
     * @param display the Display object on which the preview is displayed
     * @throws CameraControllerException 
     */
    public void setUpPreviewView( AutoFitTextureView preview,
    							  int viewWidth, int viewHeight,
    							  Display display ) {
    	
    	Point size = new Point();
    	display.getSize(size);
    	setUpPreviewView( preview, viewWidth, viewHeight,
    						display.getRotation(), new Size( size.x, size.y ) );
    }

	private void setPreviewTexture() {
		
		if( getState() == CAMERA_STATE.OPENED ) {
			SurfaceTexture surfaceTexture = mPreviewView.getSurfaceTexture();
			try {
				mCamera.setPreviewTexture(surfaceTexture, mPreviewViewSize);
				mCameraState = CAMERA_STATE.PREVIEW_SET;
			} catch(IOException e) {
				Log.e( e, "Set preview texture failed!", surfaceTexture );
			}
		}
	}

	public CAMERA_STATE getState() {
		return mCameraState;
	}

	public void startPreview() throws CameraControllerException {
		try {
			mCamera.startPreview(null, null);
			mCameraState = CAMERA_STATE.PREVIEWING;
		} catch(RuntimeException e) {
			Log.e( e, "Start preview failed!" );
			throw new CameraControllerException();
		}
	}
	
	public void startPreview( final PREVIEW_FORMAT format, final PreviewCallback cb )
					throws CameraControllerException {
		
		try {
			mCamera.startPreview(format, cb);
		} catch(RuntimeException e) {
			Log.e( e, "Start preview failed!" );
			throw new CameraControllerException();
		}
	}
	
	public void stopPreview() {
		mCamera.stopPreview();
		mCameraState = CAMERA_STATE.PREVIEW_SET;
	}
	
	private Size initPreviewSize(int width, int height, int screenRotation,
								 Size screenSize) {
		
		// For still image captures, we use the largest available size.
		Size largest
			= Collections.max( mCamera.getSupportedCaptureSize(),
							   new CompareSizesByArea());

		int sensorOrientation = mCamera.getSensorOrientation();
		boolean swappedDimensions = false;
		switch (screenRotation) {
		    case Surface.ROTATION_0:
		    case Surface.ROTATION_180:
		        if (sensorOrientation  == 90 || sensorOrientation == 270) {
		            swappedDimensions = true;
		        }
		        break;
		    case Surface.ROTATION_90:
		    case Surface.ROTATION_270:
		        if (sensorOrientation == 0 || sensorOrientation == 180) {
		            swappedDimensions = true;
		        }
		        break;
		    default:
		        Log.w("Display rotation is invalid: ", screenRotation);
		}

		int rotatedPreviewWidth = width;
		int rotatedPreviewHeight = height;
		int maxPreviewWidth = screenSize.width;
		int maxPreviewHeight = screenSize.height;

		if (swappedDimensions) {
		    rotatedPreviewWidth = height;
		    rotatedPreviewHeight = width;
		    maxPreviewWidth = screenSize.width;
		    maxPreviewHeight = screenSize.height;
		}

		if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
		    maxPreviewWidth = MAX_PREVIEW_WIDTH;
		}

		if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
		    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
		}

		// Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
		// bus' bandwidth limitation, resulting in gorgeous previews but the storage of
		// garbage capture data.
		Size previewSize
			= chooseOptimalSize(mCamera.getSupportedCaptureSize(),
								rotatedPreviewWidth, rotatedPreviewHeight,
								maxPreviewWidth, maxPreviewHeight, largest);
		
		return previewSize;
	}
    
    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param viewWidth  The width of the texture view relative to sensor coordinate
     * @param viewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private Size chooseOptimalSize(List<Size> choices, int viewWidth,
            					   int viewHeight, int maxWidth,
            					   int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= viewWidth &&
                    option.getHeight() >= viewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.w("Couldn't find any suitable preview size");
            return choices.get( 0 );
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     * @param screenRotation rotation of the screen to the original orientation
     */
    private void configureTransform(int viewWidth, int viewHeight, int screenRotation) {
        if (null == mPreviewView || null == mPreviewSensorSize) {
            return;
        }
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSensorSize.getHeight(), mPreviewSensorSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == screenRotation || Surface.ROTATION_270 == screenRotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSensorSize.getHeight(),
                    (float) viewWidth / mPreviewSensorSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (screenRotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == screenRotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mPreviewView.setTransform(matrix);
    }

    public void startAutoFocusAt( Rect displayRect ) {
    	mCamera.setFocusMode( FOCUS_MODE.AUTO );
    	mCamera.startAutoFocusAt(displayRect);
    }
    
    public void takePicture() {
    	mCamera.takePicture( CameraController.PICTURE_TYPE.JPEG);
    }
    
	public void soundShutter( boolean sound ) {
		mCamera.soundShutter(sound);
	}
	
	public void setPreviewFrameProcessing( boolean processing ) {
		mCamera.setPreviewFrameProcessing( processing );
	}
	
	@Override
	public void onOpened(int id) {
		mCameraState = CAMERA_STATE.OPENED;
		if( mPreviewView != null ) {
			setPreviewTexture();
		}
		mCameraUserInterface.cameraOpened(id);
	}

	@Override
	public void onPermissionDenied(int id) {
		mCameraUserInterface.cameraPermissionDenied(id);
	}

	@Override
	public void onOpenFailed(int id, Exception e) {
		mCameraState = CAMERA_STATE.FAILED;
		mCameraUserInterface.cameraOpenFailed(id, e);
	}

	@Override
	public void onAutoFocus(boolean success) {
		mCameraUserInterface.onAutoFocus(success);
	}

	@Override
	public void onPictureReady(PICTURE_TYPE imageType, byte[] pictureData) {
		mCameraUserInterface.savePicture(imageType, pictureData);
	}

}
