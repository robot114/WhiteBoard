package com.zsm.whiteboard.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;

public class PreviewOperator extends ImageView {

	private static final int INDICATOR_TIME = 1000;
	private static final int COLOR_FAILED = Color.RED;
	private static final int COLOR_SUCCESS = Color.GREEN;
	private static final int COLOR_FOCUSING = 0xeed7d7d7;
	private static final float FOCUS_AREA_SIZE_FACTOR = .05f;
	
	private static int mFocusAreaSize = 0;
	private static int mFocusPathSegment;
	private Paint mPaint;
	private Path mFocusIndicator;
	private Handler mHandler;
	private Runnable mDismissIndicatorCallback;
	
	public PreviewOperator(Context context, AttributeSet attrs) {
		super(context, attrs);
		mPaint = new Paint();
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeWidth(5);
        mHandler = new Handler();
        mDismissIndicatorCallback = new Runnable() {
            @Override
            public void run() {
            	mFocusIndicator = null;
            	invalidate();
            }
        };
        
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		mFocusAreaSize = (int) (getWidth()*FOCUS_AREA_SIZE_FACTOR);
		mFocusPathSegment = mFocusAreaSize*2/3;
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if(mFocusIndicator != null){
			canvas.drawPath(mFocusIndicator, mPaint);
	    }
	}

	public void setTouched(float x, float y) {
        getFocusIndicator(x, y);
        mPaint.setColor( COLOR_FOCUSING );
    	invalidate();
   		mHandler.removeCallbacks( mDismissIndicatorCallback );
		mHandler.postDelayed(mDismissIndicatorCallback, INDICATOR_TIME);
	}

	private void getFocusIndicator(float x, float y) {
		Rect touchArea = getFocusArea(x, y);
		mFocusIndicator = new Path();
		mFocusIndicator.moveTo( touchArea.left, touchArea.top);
		mFocusIndicator.rLineTo( mFocusPathSegment, 0);
		mFocusIndicator.rMoveTo( mFocusPathSegment, 0);
		mFocusIndicator.rLineTo( mFocusPathSegment, 0);
		mFocusIndicator.rLineTo( 0, mFocusPathSegment);
		mFocusIndicator.rMoveTo( 0, mFocusPathSegment);
		mFocusIndicator.rLineTo( 0, mFocusPathSegment);
		mFocusIndicator.rLineTo( -mFocusPathSegment, 0);
		mFocusIndicator.rMoveTo( -mFocusPathSegment, 0);
		mFocusIndicator.rLineTo( -mFocusPathSegment, 0);
		mFocusIndicator.rLineTo( 0, -mFocusPathSegment);
		mFocusIndicator.rMoveTo( 0, -mFocusPathSegment);
		mFocusIndicator.rLineTo( 0, -mFocusPathSegment);
	}

	public void setFocusSuccess( boolean success ) {
		int color = success ? COLOR_SUCCESS : COLOR_FAILED;
		mPaint.setColor( color );
		invalidate();
	}
	
	public Rect getFocusArea(float x, float y) {
		Rect rect = new Rect(
            (int)(x - mFocusAreaSize), 
            (int)(y - mFocusAreaSize), 
            (int)(x + mFocusAreaSize), 
            (int)(y + mFocusAreaSize));
		if( rect.left < 0 ) {
			rect.right -= rect.left;
			rect.left = 0;
		}
		if( rect.right >= getWidth() ) {
			rect.left = getWidth()-mFocusAreaSize*2-1;
			rect.right = getWidth()-1;
		}
		if( rect.top < 0 ) {
			rect.bottom -= rect.top;
			rect.top = 0;
		}
		
		if( rect.bottom >= getHeight() ) {
			rect.top = getHeight() - mFocusAreaSize*2 - 1;
			rect.bottom = getHeight() - 1;
		}
		return rect;
	}
}
