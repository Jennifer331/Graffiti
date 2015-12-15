package com.example.administrator.graffiti;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Stack;

/**
 * Created by Lei Xiaoyue on 2015-12-15.
 */
public class GraffitiView extends View {
    private Path mPath;
    private Paint mPaint;
    private Stack<Trail> mTrails;
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

    private void init(){
        mPath = new Path();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mTrails = new Stack<Trail>();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for(Trail trail : mTrails){
            canvas.drawPath(trail.path,trail.paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        switch(action){
            case MotionEvent.ACTION_DOWN:{
                mPath.reset();
                mPath.moveTo(x, y);
                mTrails.add(new Trail(mPath, mPaint));
                break;
            }
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:{
                Trail trail = mTrails.peek();
                trail.path.lineTo(x, y);
                break;
            }
        }
        invalidate();
        return true;
    }

    public void setPaintColor(int color){
        mPaint.setColor(color);
    }

    public void setStrokeWidth(int width){
        mPaint.setStrokeWidth(width);
    }

    public boolean undo(){
        if(null == mTrails || mTrails.isEmpty()){
            return false;
        }
        mTrails.pop();
        invalidate();
        return true;
    }
}
