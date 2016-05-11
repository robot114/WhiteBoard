package com.zsm.whiteboard.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.TextureView;

/**
 * A {@link TextureView} that can be adjusted to a specified aspect ratio.
 */
@SuppressLint("ClickableViewAccessibility")
public class AutoFitTextureView extends TextureView {

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
	private PreviewOperator mPreviewOperator;
	private OnTouchListener mOnTouchListener;

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if( mPreviewOperator == null ) {
			return false;
		}
		
		boolean result = false;
		
		int action = event.getAction();
		switch( action ) {
			case MotionEvent.ACTION_DOWN:
		        if (mPreviewOperator != null) {
		        	mPreviewOperator.setTouched(event.getX(), event.getY() );
		        }
		        
		        if( mOnTouchListener != null ) {
		        	result = mOnTouchListener.onTouch( this, event );
		        }
		        break;
		    default:
		    	break;
		}

		return result;
	}
	
	protected void setPreviewOperator( PreviewOperator view ) {
    	mPreviewOperator = view;
    }

	@Override
	public void setOnTouchListener(OnTouchListener l) {
		mOnTouchListener = l;
	}
	
	public Rect getFocusAreaAt( float x, float y ) {
		return mPreviewOperator.getFocusArea(x, y);
	}

	public void setFocusSuccess( boolean success ) {
		mPreviewOperator.setFocusSuccess(success);
	}
}