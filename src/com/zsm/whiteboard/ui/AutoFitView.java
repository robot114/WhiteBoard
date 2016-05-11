package com.zsm.whiteboard.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class AutoFitView extends ViewGroup {

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
	private View mView;

    public AutoFitView(Context context) {
        this(context, null);
    }

    public AutoFitView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitView(Context context, AttributeSet attrs, int defStyle) {
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

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		mView.layout(l, t, r, b);
	}

	@Override
	public void addView(View child) {
		if( getChildCount() > 0 ) {
			throw new IllegalStateException( "Only one child can be added" );
		}
		mView = child;
		super.addView(child);
	}
}
