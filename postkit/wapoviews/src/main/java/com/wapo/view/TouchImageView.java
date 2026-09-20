package com.wapo.view;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import com.wapo.android.commons.util.Logger;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

public class TouchImageView extends ImageView {
    public static final String TAG = TouchImageView.class.getName();
    private static final String MatrixParam = TouchImageView.class.getName() + ".matrix";
    private static final String ParentStateParam = TouchImageView.class.getName() + ".parentState";

    public static final int BORDERS_VISIBILITY_NONE = 0;
    public static final int BORDERS_VISIBILITY_TOP = 1;
    public static final int BORDERS_VISIBILITY_RIGHT = 2;
    public static final int BORDERS_VISIBILITY_BOTTOM = 4;
    public static final int BORDERS_VISIBILITY_LEFT = 8;


    private float _maxScaleFactor = 3;
    private float _initialScale = 1;
    private boolean _isScaling = false;
    private boolean _isInitialized = false;
    private boolean shouldResetZoom = false;

    private GestureDetector _gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return TouchImageView.this.onScroll(e1, e2, distanceX, distanceY);
        }
    });

    private ScaleGestureDetector _scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = detector.getScaleFactor();

            Matrix imageMatrix = getImageMatrix();
            float[] mv = new float[9];
            imageMatrix.getValues(mv);
            float scaleFactor = mv[0];
            float maxScaleFactor = _initialScale * _maxScaleFactor;
            if (scaleFactor >= maxScaleFactor && scale >= 1) {
                return false;
            }

            Drawable drawable = getDrawable();
            int intrinsicWidth = drawable.getIntrinsicWidth();
            int intrinsicHeight = drawable.getIntrinsicHeight();
            float [] leftUpper = new float[2];
            float [] rightBottom = new float[2];
            imageMatrix.mapPoints(leftUpper, new float[]{0, 0});
            imageMatrix.mapPoints(rightBottom, new float[] { intrinsicWidth, intrinsicHeight});

            float px = detector.getFocusX();
            float py = detector.getFocusY();

            Matrix m = new Matrix();
            m.setScale(scale, scale, px, py);
            m.preConcat(imageMatrix);


            m.getValues(mv);
            m = adjustImageMatrix(m, intrinsicWidth, intrinsicHeight);

            m.getValues(mv);
            scaleFactor = mv[0];
            setImageMatrix(m);
            Logger.d(TAG, "onScale: " + (scaleFactor > _initialScale && scaleFactor < maxScaleFactor));
            return scaleFactor > _initialScale && scaleFactor < maxScaleFactor;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            Logger.d(TAG, "onScaleBegin: true");
            _isScaling = true;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            Logger.d(TAG, "onScaleEnd");
            _isScaling = false;
        }
    });

    public TouchImageView(Context context) {
        super(context);
        setPadding(0, 0, 0, 0);
        setScaleType(ScaleType.MATRIX);
    }

    public TouchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPadding(0, 0, 0, 0);
        setScaleType(ScaleType.MATRIX);
    }

    public TouchImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setPadding(0, 0, 0, 0);
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        int width = getWidth();
        int height = getHeight();
        _isInitialized = false;
        if (drawable != null && width > 0 && height > 0) {
            Matrix m = new Matrix();
            m.setRectToRect(
                    new RectF(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight()),
                    new RectF(getPaddingLeft(), getPaddingTop(), width - getPaddingRight(), height - getPaddingBottom()),
                    Matrix.ScaleToFit.CENTER
            );
            setImageMatrix(m);
            float [] mv = new float[9];
            m.getValues(mv);
            _initialScale = mv[0];
            _isInitialized = true;
        }
    }

    public void resetZoom() {
        Drawable drawable = getDrawable();
        int width = getWidth();
        int height = getHeight();
        _isInitialized = false;
        if (drawable != null && width > 0 && height > 0) {
            Matrix m = new Matrix();
            m.setRectToRect(
                    new RectF(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight()),
                    new RectF(getPaddingLeft(), getPaddingTop(), width - getPaddingRight(), height - getPaddingBottom()),
                    Matrix.ScaleToFit.CENTER
            );
            setImageMatrix(m);
            float [] mv = new float[9];
            m.getValues(mv);
            _initialScale = mv[0];
            _isInitialized = true;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        //LogUtil.d(TAG, "dispatchTouchEvent");
        Matrix m = getImageMatrix();
        Drawable drawable = getDrawable();
        if (drawable == null) {
            //LogUtil.d(TAG, "dispatch: false");
            return false;
        }
        /*
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();

        float [] leftUpper = new float[2];
        float [] rightBottom = new float[2];
        m.mapPoints(leftUpper, new float[] {0,0});
        m.mapPoints(rightBottom, new float[] { intrinsicWidth, intrinsicHeight});

        float ex = event.getX();
        float ey = event.getY();
        int width = getWidth();
        int height = getHeight();

        if (ex < Math.max(leftUpper[0], 0) || ex > Math.min(rightBottom[0], width)  ||
                ey < Math.max(leftUpper[1], 0) || ey > Math.min(rightBottom[1], height)) {
            //LogUtil.d(TAG, "dispatch: false");
            return false;
        }
        */
        //LogUtil.d(TAG, String.format("Image Rect: (%d, %d)", imageRect.width(), imageRect.height()));
        //LogUtil.d(TAG, String.format("dispatchTouchEvent: (%f, %f)", event.getX(), event.getY()));
        //LogUtil.d(TAG, String.format("(%f, %f) (%f %f) (%f %f)", ex, ey, leftUpper[0], leftUpper[1], rightBottom[0], rightBottom[1]));
        boolean result = _scaleDetector.onTouchEvent(event);
        int action = event.getAction();
        boolean isScrollOrMove = action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_SCROLL;
        //LogUtil.d(TAG, "scaleDetector: " + result + "; scaling: " + _isScaling);
        if (!_isScaling) {
            //result |= _gestureDetector.onTouchEvent(event);
            boolean gestureRes = _gestureDetector.onTouchEvent(event);
            if (isScrollOrMove) {
                result = gestureRes;
            } else {
                result |= gestureRes;
            }
        }
        //LogUtil.d(TAG, "dispatch: " + result);
        return result;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (shouldResetZoom) {
            shouldResetZoom = false;
            resetZoom();
            return;
        }

        Drawable d = getDrawable();
        if (d == null) {
            return;
        }

        float intrinsicWidth = d.getIntrinsicWidth();
        float intrinsicHeight = d.getIntrinsicHeight();

        if (!_isInitialized) {
            Matrix m = new Matrix();
            m.setRectToRect(
                    new RectF(0, 0, intrinsicWidth, intrinsicHeight),
                    new RectF(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom()),
                    Matrix.ScaleToFit.CENTER
            );
            setImageMatrix(m);
            _isInitialized = true;
        } else {
            setImageMatrix(adjustImageMatrix(getImageMatrix(), d.getIntrinsicWidth(), d.getIntrinsicHeight()));
        }
        _initialScale = Math.min((float)getWidth() / intrinsicWidth, (float)getHeight() / intrinsicHeight);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable result = super.onSaveInstanceState();
        Bundle b = new Bundle();
        b.putParcelable(ParentStateParam, result);
        float [] matrix = new float[9];
        getImageMatrix().getValues(matrix);
        if (matrix[0] > _initialScale) {
            b.putFloatArray(MatrixParam, matrix);
        }
        return b;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle b = (Bundle)state;
            Parcelable parentState = b.getParcelable(ParentStateParam);
            if (parentState != null) {
                super.onRestoreInstanceState(parentState);
            }
            float[] matrix = b.getFloatArray(MatrixParam);
            if (matrix != null) {
                Matrix m = new Matrix();
                m.setValues(matrix);
                Drawable d = getDrawable();
                if (d != null) {
                    int intrinsicWidth = d.getIntrinsicWidth();
                    int intrinsicHeight = d.getIntrinsicHeight();
                    m = adjustImageMatrix(m, intrinsicWidth, intrinsicHeight);
                    setImageMatrix(m);
                    _isInitialized = true;
                }
            }
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    public int getBordersVisibility() {
        Drawable d = getDrawable();
        if (d == null) {
            return BORDERS_VISIBILITY_BOTTOM |
                   BORDERS_VISIBILITY_TOP |
                   BORDERS_VISIBILITY_RIGHT |
                   BORDERS_VISIBILITY_LEFT;
        }

        float [] upperLeft = new float[2];
        float [] bottomRight = new float[2];
        Matrix m = getImageMatrix();
        m.mapPoints(upperLeft, new float[] {0, 0} );
        m.mapPoints(bottomRight, new float[] {d.getIntrinsicWidth(), d.getIntrinsicHeight()});

        int result = BORDERS_VISIBILITY_NONE;
        if (upperLeft[0] >= getPaddingLeft()) {
            result |= BORDERS_VISIBILITY_LEFT;
        }
        if (upperLeft[1] >= getPaddingTop()) {
            result |= BORDERS_VISIBILITY_TOP;
        }
        if (bottomRight[0] <= getWidth() - getPaddingRight()) {
            result |= BORDERS_VISIBILITY_RIGHT;
        }
        if (bottomRight[1] <= getHeight() - getPaddingBottom()) {
            result |= BORDERS_VISIBILITY_BOTTOM;
        }

        return result;
    }

    public float getMaxScaleFactor() {
        return _maxScaleFactor;
    }

    public void setMaxScaleFactor(float maxScaleFactor) {
        _maxScaleFactor = maxScaleFactor;
    }

    private boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Matrix imageMatrix = getImageMatrix();
        Drawable drawable = getDrawable();
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        float [] leftUpper = new float[2];
        float [] rightBottom = new float[2];
        imageMatrix.mapPoints(leftUpper, new float[]{0, 0});
        imageMatrix.mapPoints(rightBottom, new float[] { intrinsicWidth, intrinsicHeight});

        float borderLeft = getPaddingLeft();
        float borderTop = getPaddingTop();
        float borderRight = getWidth() - getPaddingRight();
        float borderBottom = getHeight() - getPaddingBottom();

        if (leftUpper[0] >= borderLeft && rightBottom[0] <= borderRight && leftUpper[1] >= borderTop && rightBottom[1] <= borderBottom) {
            return false;
        }

        float dx = 0;
        float dy = 0;

        if (distanceX > 0 && rightBottom[0] >= borderRight) {
            dx = Math.min(distanceX, rightBottom[0] - borderRight);
        } else if (distanceX < 0 && leftUpper[0] <= borderLeft) {
            dx = Math.max(distanceX, leftUpper[0] - borderLeft);
        }

        if (distanceY > 0 && rightBottom[1] >= borderBottom) {
            dy = Math.min(distanceY, rightBottom[1] - borderBottom);
        } else if (distanceY < 0 && leftUpper[1] <= borderTop) {
            dy = Math.max(distanceY, leftUpper[1] - borderTop);
        }

        if (dx == 0 && dy == 0) {
            return false;
        }

        Matrix m = new Matrix();
        m.setTranslate(-dx, -dy);
        m.preConcat(imageMatrix);

        setImageMatrix(m);

        return true;
    }

    private Matrix adjustImageMatrix(Matrix imageMatrix, int intrinsicWidth, int intrinsicHeight) {
        int borderLeft = getPaddingLeft();
        int borderRight = getWidth() - getPaddingRight();
        int borderTop = getPaddingTop();
        int borderBottom = getHeight() - getPaddingBottom();

        int width = borderRight - borderLeft;
        int height = borderBottom - borderTop;

        imageMatrix = imageMatrix == null ? new Matrix() : imageMatrix;

        float [] upperLeft = new float[2];
        float [] bottomRight = new float[2];

        imageMatrix.mapPoints(upperLeft, new float[] {0, 0});
        imageMatrix.mapPoints(bottomRight, new float[] {intrinsicWidth, intrinsicHeight});

        float imageWidth = bottomRight[0] - upperLeft[0];
        float imageHeight = bottomRight[1] - upperLeft[1];

        if (imageWidth == 0 || imageHeight == 0) {
            return imageMatrix;
        }

        if (imageHeight < height && imageWidth < width) {
            Matrix m = new Matrix();
            m.setRectToRect(
                    new RectF(0, 0, intrinsicWidth, intrinsicHeight),
                    new RectF(borderLeft, borderTop, borderRight, borderBottom),
                    Matrix.ScaleToFit.CENTER
            );
            return m;
        }

        float dx = 0;
        float dy = 0;
        if (imageHeight < height) {
            float top = borderTop + (height - imageHeight) / 2.0f;
            dy = top - upperLeft[1];
        } else if (upperLeft[1] > borderTop) {
            dy = borderTop - upperLeft[1];
        } else if (bottomRight[1] < borderBottom) {
            dy = borderBottom - bottomRight[1];
        }

        if (imageWidth < width) {
            float left = borderLeft + (width - imageWidth) / 2.0f;
            dx = left - upperLeft[0];
        } else if (upperLeft[0] > borderLeft) {
            dx = borderLeft - upperLeft[0];
        } else if (bottomRight[0] < borderRight) {
            dx = borderRight - bottomRight[0];
        }

        if (dx != 0 || dy != 0) {
            Matrix m = new Matrix();
            m.setTranslate(dx, dy);
            m.preConcat(imageMatrix);
            imageMatrix = m;
        }

        return imageMatrix;
    }

    public void setShouldResetZoom(boolean shouldResetZoom) {
        this.shouldResetZoom = shouldResetZoom;
    }
}
