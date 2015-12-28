package com.example.administrator.graffiti;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.Stack;

/**
 * Created by Lei Xiaoyue on 2015-12-15.
 */
//public class GraffitiView extends ImageView implements GestureDetector.OnGestureListener {
public class GraffitiView extends View implements GestureDetector.OnGestureListener {
    private static final String TAG = "GraffitiView";
    private static final int TRANSPARENT_COLOR = 0x00000000;
    private Path mPath;
    private Paint mPaint;
    private Stack<Trail> mTrails;
    private Bitmap mRenderBuffer;
    private int mBufferStep = -1;

    private RectF mDestBound;
    private float mPresentImageWidth;
    private float mPresentImageHeight;
    private float mLastX, mLastY;
    private GestureDetector mDetector;

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
        init(context);
    }

    private void init(Context context) {
        mPath = new Path();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mTrails = new Stack<Trail>();
        mDetector = new GestureDetector(context.getApplicationContext(), this);
        setFakeArea();
    }

    private void setFakeArea() {
        mDestBound = new RectF(200, 0, 600, 600);
        mPresentImageWidth = 1200;
        mPresentImageHeight = 1800;
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long startTime = System.currentTimeMillis();
        Log.v(TAG, "[onDrawStart]" + startTime);
        canvas.drawRect(mDestBound,mPaint);
        canvas.save();
        if (null != mRenderBuffer && !mRenderBuffer.isRecycled()) {
            float scale = mDestBound.width() / mPresentImageWidth;
            canvas.translate(mDestBound.left, mDestBound.top);
            canvas.scale(scale, scale);
            canvas.drawBitmap(mRenderBuffer, 0, 0, null);
        }
        canvas.restore();
        canvas.save();
        long middleTime = System.currentTimeMillis();
        Log.v(TAG, "[onDrawAfterBitmap]" + (middleTime - startTime) + ", GID = " + (mRenderBuffer != null ? mRenderBuffer.getGenerationId() : 0));
        canvas.clipRect(mDestBound);
        if (null != mTrails && mBufferStep < mTrails.size()) {
            for (int i = mBufferStep + 1; i < mTrails.size(); i++) {
                Trail trail = mTrails.get(i);
                canvas.drawPath(trail.path, trail.paint);
            }
        }
        canvas.restore();
        Log.v(TAG, "[onDrawFinish]" + (System.currentTimeMillis() - middleTime));
    }

    private void touchStart(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mTrails.push(new Trail(mPath, mPaint));
        mLastX = x;
        mLastY = y;
    }

    private void touchMove(float x, float y) {
        Trail trail = mTrails.peek();
        if (Math.abs(x - mLastX) > Config.BEZIER_START_EDGE
                || Math.abs(y - mLastY) > Config.BEZIER_START_EDGE) {
            trail.path.quadTo(mLastX, mLastY, (x + mLastX) / 2, (y + mLastY) / 2);
            mLastX = x;
            mLastY = y;
        }
    }

    private void touchUp(float x, float y) {
        Trail trail = mTrails.peek();
        trail.path.lineTo(mLastX, mLastY);
    }

    private void touchTap(float x, float y) {
        Trail trail = mTrails.peek();
        trail.path.addCircle(x, y, trail.paint.getStrokeWidth() / 2, Path.Direction.CW);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = mDetector.onTouchEvent(event);
        invalidate();
        int action = event.getAction();
        if(action == MotionEvent.ACTION_UP){
            float x = event.getX();
            float y = event.getY();
            touchUp(x, y);
            refreshPathValidation(x, y);
        }
        if(!mTrails.peek().valid){
            mTrails.pop();
        }
        if (mTrails.size() - 1 - mBufferStep > Config.BACK_PERMIT * 2) {
            pushToBuffer(mTrails.size() - 1 - Config.BACK_PERMIT,false);
        }
        return result;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        touchStart(x, y);
        refreshPathValidation(x,y);
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        touchTap(x, y);
        refreshPathValidation(x, y);
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float x = e2.getX();
        float y = e2.getY();
        touchMove(x, y);
        refreshPathValidation(x, y);
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent eventStart, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    public void setPaintColor(int color) {
        mPaint.setColor(color);
    }

    public void setStrokeWidth(int width) {
        mPaint.setStrokeWidth(width);
    }

    public void setDestBound(RectF destBound){
        this.mDestBound = new RectF(destBound);
    }

    public void setRenderBufferSize(float width,float height){
        this.mPresentImageWidth = width;
        this.mPresentImageHeight = height;
        checkBuffer();
    }

    private void checkBuffer(){
        if (null == mRenderBuffer
                || mRenderBuffer.getWidth() != mPresentImageWidth
                || mRenderBuffer.getHeight() != mPresentImageHeight) {
            mRenderBuffer = Bitmap.createBitmap((int) mPresentImageWidth,
                    (int) mPresentImageHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mRenderBuffer);
            canvas.drawColor(TRANSPARENT_COLOR, PorterDuff.Mode.CLEAR);
        }
    }

    public boolean undo() {
        if (null == mTrails || mTrails.isEmpty()) {
            return false;
        }
        mTrails.pop();
        if (-1 != mBufferStep && mBufferStep == mTrails.size() - 1) {
            resetBufferStep();
        }
        invalidate();
        return true;
    }

    private void resetBufferStep() {
        mBufferStep = mTrails.size() - Config.BACK_PERMIT - 1;
        if (mBufferStep < 0) {
            mBufferStep = -1;
        }
        refreshBuffer();
    }

    private void refreshBuffer() {
        pushToBuffer(mBufferStep, true);
    }

    private void pushToBuffer(int step,boolean isRebuild) {
        Log.e(TAG, "[pushToBuffer]");
        Log.v(TAG, "[pushToBuffer]step:" + step);
        checkBuffer();
        Canvas canvas = new Canvas(mRenderBuffer);
        if (isRebuild) {
            canvas.drawColor(TRANSPARENT_COLOR, PorterDuff.Mode.CLEAR);
        }
        float scale = mPresentImageWidth / mDestBound.width() ;
        canvas.scale(scale, scale);
        canvas.translate(-mDestBound.left, -mDestBound.top);
        int i = isRebuild ? 0 : mBufferStep + 1;
        for (; i <= step; i++) {
            Trail trail = mTrails.get(i);
            canvas.drawPath(trail.path, trail.paint);
            mBufferStep = i;
        }
    }

    public void clear(){
        mBufferStep = -1;
        if(null != mRenderBuffer && !mRenderBuffer.isRecycled()) {
            mRenderBuffer.recycle();
        }
        if(null != mTrails){
            mTrails.clear();
        }
    }

    /**
     * if a point enters the present area ,sets the path valid field to be true
     * so that the path will not be deleted after drawing
     */
    private final void refreshPathValidation(float x,float y) {
        Trail mCurrentPath = mTrails.peek();
        if (mCurrentPath.valid) {
            return;
        }
        if (x > mDestBound.left && x < mDestBound.right
                && y > mDestBound.top
                && y < mDestBound.bottom) {
            mCurrentPath.valid = true;
        }
    }

    /**
     * gets only the Graffiti the user drawn
     *
     * @return
     */
    public Bitmap getDoodle() {
        pushToBuffer(mTrails.size() - 1,false);
        return mRenderBuffer;
    }
}
