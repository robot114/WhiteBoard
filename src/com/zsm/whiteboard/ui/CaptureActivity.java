package com.zsm.whiteboard.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Display;
import android.view.MotionEvent;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.zsm.log.Log;
import com.zsm.whiteboard.R;
import com.zsm.whiteboard.camera.CameraController;
import com.zsm.whiteboard.camera.CameraController.PICTURE_TYPE;
import com.zsm.whiteboard.camera.CameraController.PREVIEW_FORMAT;
import com.zsm.whiteboard.camera.CameraControllerException;
import com.zsm.whiteboard.camera.CameraUserInterface;
import com.zsm.whiteboard.camera.LegacyCamera;
import com.zsm.whiteboard.camera.PreviewCallback;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
@SuppressWarnings("deprecation")
public class CaptureActivity extends Activity
	implements CameraUserInterface, SurfaceTextureListener {
	
	private CameraController mCameraController;
	private AutoFitTextureView mPreviewView;
	protected Bitmap mBitmap;
	private Handler mHandler;
	private PreviewOperator mPreviewOperator;

	@SuppressLint("ClickableViewAccessibility")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
							 WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.activity_capture);

		mPreviewOperator = (PreviewOperator)findViewById( R.id.viewOperator );
		mPreviewView = (AutoFitTextureView) findViewById(R.id.viewPreview);
		
		mPreviewView.setSurfaceTextureListener( this );
		mPreviewView.setPreviewOperator(mPreviewOperator);
		mHandler = new Handler();

		mCameraController = new CameraController( this, new LegacyCamera() );
		
		mPreviewView.setOnTouchListener( new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mCameraController
					.startAutoFocusAt( 
						mPreviewView.getFocusAreaAt( event.getX(), event.getY() ) );
				return true;
			}
		} );
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mCameraController.openCamera( CameraInfo.CAMERA_FACING_BACK, this );
	}

	@Override
	protected void onPause() {
		super.onPause();
		mCameraController.releaseCamera();
	}

	@Override
	public void cameraOpened(int id) {
		Log.d( "Camera opened, id is ", id );
		mHandler.post( new Runnable() {

			@Override
			public void run() {
				Display display = getWindowManager().getDefaultDisplay();
				Point size = new Point();
				display.getSize( size );
				mCameraController
					.setUpPreviewView(mPreviewView, size.x, size.y,
										display );
				Log.d( "preview size", size );
				startPreview();
			}
			
		} );
	}

	private void startPreview() {
		if( mCameraController.getState() == CameraController.CAMERA_STATE.PREVIEW_SET ) {
			PreviewCallback pc = new PreviewCallback(){
				@Override
				public void onPreviewFrame( final PREVIEW_FORMAT format, final byte[] frameData,
											final int width, final int height ) {
					
					mHandler.post( new Runnable() {
						@Override
						public void run() {
							processPreviewFrame(frameData, width, height);
						}
					} );
				}
			};
			
			try {
				mCameraController.startPreview(PREVIEW_FORMAT.NV21, pc);
				Log.d( "Started to preview successfully!" );
			} catch (CameraControllerException e) {
				Log.e( e, "Failed to start preview!" );
				showToast( R.string.promptPreviewFailed );
			}
		}
	}

	@Override
	public void cameraPermissionDenied(int id) {
		Log.d( "Camera access denied! id is ", id );
		showToast( R.string.promptCameraAccessDenied );
	}

	@Override
	public void cameraOpenFailed(int id, Exception e) {
		Log.e( e, "Open camera failed! id is ", id );
		showToast( R.string.promptOpenCameraFailed );
		finish();
	}

	@Override
	public void onAutoFocus(boolean success) {
		Log.d( "Atuo focus result: ", success );
		mPreviewView.setFocusSuccess(success);
	}

	@Override
	public void savePicture(PICTURE_TYPE imageType, byte[] pictureData) {
		String dirPath = Environment.getExternalStorageDirectory() + "/WhiteBoard";
		File path = new File( dirPath );
		File pictureFile = new File( dirPath, "test.jpg" );
		int promptId;
		try {
			path.mkdir();
			OutputStream os = new FileOutputStream( pictureFile );
			os.write(pictureData);
			os.close();
			promptId = R.string.promptSavePictureSuccess;
		} catch (IOException e) {
			Log.e( e, "Savet picture to file failed!", pictureFile );
			promptId = R.string.promptSavePictureFailed;
		}
		String prompt = getResources().getString( promptId, pictureFile.toString() );
		Toast.makeText( this, prompt, Toast.LENGTH_LONG ).show();
		mCameraController.soundShutter( true );
	}

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final int id) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast
                	.makeText(CaptureActivity.this.getApplicationContext(),
                			  id, Toast.LENGTH_LONG)
                	.show();
            }
        });
    }

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
										  int height) {
		
		mCameraController
			.setUpPreviewView(mPreviewView, width, height,
								getWindowManager().getDefaultDisplay() );
		Log.d( "Surface is available", surface );
		startPreview();
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
											int height) {
		mCameraController
			.setUpPreviewView(mPreviewView, width, height,
								getWindowManager().getDefaultDisplay() );
		Log.d( "Surface size changed", surface );
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		return false;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
		// TODO Auto-generated method stub
		
	}
	
	public void onTakePicture( View v ) {
		mCameraController.soundShutter( false );
		mCameraController.takePicture();
	}

	private void processPreviewFrame(byte[] frameData, final int width, final int height) {
		mCameraController.setPreviewFrameProcessing( true );
		if( mBitmap == null ) {
			mBitmap = Bitmap.createBitmap(width, height,
		            Bitmap.Config.ARGB_8888);
			mPreviewOperator.setImageBitmap(mBitmap);
		}
		
		int max = 0;
		int pos = 0;
		int len = frameData.length*2/3;
		for( int i = 0; i < len; i++ ) {
			int data = frameData[i] & 0xFF;
			if( data > max ) {
				max = data;
				pos = i;
			}
		}
		int x = pos % width, y = pos / width;
		x = Math.min( x, width - 101 );
		y = Math.min( y, height - 101 );
		int pixels[] = new int[100*100];
		mBitmap.setPixels(pixels, 0, 100, x, y, 100, 100);
		for( int i = 0; i < pixels.length; i++ ){
			pixels[i] = 0xFFFF0000;
		}
		mPreviewOperator.invalidate();
		mBitmap.setPixels(pixels, 0, 100, x, y, 100, 100);
		mPreviewOperator.setImageBitmap(mBitmap);
		mCameraController.setPreviewFrameProcessing( false );
	}
}
