package com.example.administrator.graffiti;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.util.Stack;

/**
 * Created by Lei Xiaoyue on 2015-12-15.
 */
public class GraffitiView extends ImageView {
    private static final String TAG = "GraffitiView";
    private Path mPath;
    private Paint mPaint;
    private Stack<Trail> mTrails;
    private Uri mBitmapPath;
    private RectF mDestBounds;
    private float mLastX, mLastY;
    private boolean mOnePointFlag;

    public GraffitiView(Context context) {
        this(context, null);
    }

    public GraffitiView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GraffitiView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GraffitiView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mPath = new Path();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mTrails = new Stack<Trail>();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Trail trail : mTrails) {
            canvas.drawPath(trail.path, trail.paint);
        }
    }

    private void touchStart(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mTrails.add(new Trail(mPath, mPaint));
        mLastX = x;
        mLastY = y;
        mOnePointFlag = true;
    }

    private void touchMove(float x, float y) {
        Trail trail = mTrails.peek();
        if (Math.abs(x - mLastX) > Config.BEZIER_START_EDGE
                || Math.abs(y - mLastY) > Config.BEZIER_START_EDGE) {
            trail.path.quadTo(mLastX, mLastY, (x + mLastX) / 2, (y + mLastY) / 2);
            mLastX = x;
            mLastY = y;
            if (mOnePointFlag) {
                mOnePointFlag = false;
            }
        }
    }

    private void touchUp(float x, float y) {
        Trail trail = mTrails.peek();
        if (x == mLastX && y == mLastY && mOnePointFlag) {
            trail.path.addCircle(x, y, trail.paint.getStrokeWidth() / 2, Path.Direction.CW);
            Log.v(TAG, "[touchUp] 点" + x + "," + y);
        } else {
            trail.path.lineTo(mLastX, mLastY);
        }
        mPath.reset();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        refreshPaintArea();
        Log.v(TAG, mDestBounds + "");
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        if (x < mDestBounds.left || x > mDestBounds.right
                || y < mDestBounds.top || y > mDestBounds.bottom) {
            return true;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                touchStart(x, y);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                touchMove(x, y);
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                touchUp(x, y);
                break;
            }
        }
        invalidate();
        return true;
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        refreshPaintArea();
    }

    public void setPaintColor(int color) {
        mPaint.setColor(color);
    }

    public void setStrokeWidth(int width) {
        mPaint.setStrokeWidth(width);
    }

    public boolean undo() {
        if (null == mTrails || mTrails.isEmpty()) {
            return false;
        }
        mTrails.pop();
        invalidate();
        return true;
    }

    public void setSrc(Uri uri) {
        this.mBitmapPath = uri;
        setImageURI(uri);
        mTrails.clear();
        refreshPaintArea();
    }

    private void refreshPaintArea() {
        mDestBounds = new RectF(getDrawable().getBounds());
        Matrix matrix = getImageMatrix();
        matrix.mapRect(mDestBounds);
    }

    public Bitmap getResult() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        Bitmap result = Bitmap.createBitmap(bitmap, (int) mDestBounds.left, (int) mDestBounds.top,
                (int) mDestBounds.width(), (int) mDestBounds.height());
        bitmap.recycle();
        bitmap = null;
        setImageBitmap(result);
        return result;
    }
}
