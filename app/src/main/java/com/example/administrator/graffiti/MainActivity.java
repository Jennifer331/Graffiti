package com.example.administrator.graffiti;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;

/**
 * Created by Lei Xiaoyue on 2015-12-15.
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private GraffitiView mGraffitiView;
    private SeekBar mSeekBar;
    private RadioGroup mRadioGroup;
    private Button mChoosePicBtn, mSaveBtn;
    private ImageView mImageView;
    private Uri mImageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        registerWidget();
        initListener();
    }

    private void registerWidget() {
        mImageView = (ImageView)findViewById(R.id.image);
        mGraffitiView = (GraffitiView) findViewById(R.id.graffiti);
        mSeekBar = (SeekBar) findViewById(R.id.seekbar);
        mSeekBar.setMax(Config.MAX_STROKE_WIDTH);
        mRadioGroup = (RadioGroup) findViewById(R.id.colorPicker);
        int i = 0;
        for (; i < Config.PAINT_COLORS.length; i++) {
            RadioButton button = new RadioButton(this);

            Paint paint = new Paint();
            paint.setColor(Config.PAINT_COLORS[i]);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Config.COLOR_PICKER_ICON_PAINT_WIDTH);
            Bitmap bitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawCircle(60, 60, 25, paint);

            Paint focusPaint = new Paint();
            focusPaint.setColor(Config.PAINT_COLORS[i]);
            focusPaint.setStyle(Paint.Style.FILL);
            Bitmap focusBitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888);
            Canvas focusCanvas = new Canvas(focusBitmap);
            focusCanvas.drawCircle(60, 60, 25, focusPaint);

            StateListDrawable drawable = new StateListDrawable();
            drawable.addState(new int[]{android.R.attr.state_pressed},
                    new BitmapDrawable(this.getResources(), focusBitmap));
            drawable.addState(new int[]{android.R.attr.state_checked},
                    new BitmapDrawable(this.getResources(), focusBitmap));
            drawable.addState(new int[]{},
                    new BitmapDrawable(this.getResources(), bitmap));
            button.setButtonDrawable(drawable);
            button.setId(i);
            button.setFocusable(true);
            mRadioGroup.addView(button, i);
        }
        mChoosePicBtn = (Button) findViewById(R.id.choosePics);
        mSaveBtn = (Button) findViewById(R.id.save);
    }

    private void initListener() {
        mImageView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                refreshGraffiti();
            }
        });
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mGraffitiView.setPaintColor(Config.PAINT_COLORS[checkedId]);
            }
        });
        mRadioGroup.findViewById(0).performClick();
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mGraffitiView.setStrokeWidth(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mSeekBar.setProgress(Config.DEFAULT_STROKE_WIDTH);
        mChoosePicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, Config.PIC_REQUEST_CODE);
            }
        });
        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap script = mGraffitiView.getDoodle();
                Bitmap origin;
                if(null == mImageUri){
                    //if no src image,get the default image set to imageView
                    Log.v(TAG,"[save button clicked]use example pic");
                    Drawable drawable = mImageView.getDrawable();
                    int width = drawable.getIntrinsicWidth();
                    int height = drawable.getIntrinsicHeight();
                    origin = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888)
                            .copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(origin);
                    drawable.setBounds(0,0,width,height);
                    drawable.draw(canvas);
                }else {
                    Log.v(TAG,"[save button clicked]use assigned pic");
                    ParcelFileDescriptor mFileDescriptor = null;
                    try {
                        mFileDescriptor = getContentResolver().openFileDescriptor(mImageUri, "r");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    origin = BitmapFactory.decodeFileDescriptor(mFileDescriptor.getFileDescriptor(), null, null)
                            .copy(Bitmap.Config.ARGB_8888, true);
                }
                compositeToOrigin(origin, script);
                MediaStore.Images.Media.insertImage(getContentResolver(), origin, System.currentTimeMillis() + ".jpg", "");
                Toast.makeText(getApplicationContext(), "已保存", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void compositeToOrigin(Bitmap origin,Bitmap script){
        Canvas canvas = new Canvas(origin);
        float scale = (float)origin.getWidth() / (float)script.getWidth();
        canvas.scale(scale,scale);
        canvas.drawBitmap(script,0,0,new Paint());
    }

    @Override
    public void onBackPressed() {
        if (!mGraffitiView.undo()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (RESULT_OK == resultCode && Config.PIC_REQUEST_CODE == requestCode) {
            mGraffitiView.clear();
            mImageUri = data.getData();
            mImageView.setImageURI(mImageUri);
            refreshGraffiti();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * to set the drawable area in the GraffitiView same as image bounds
     */
    private void refreshGraffiti(){
        RectF destBounds = new RectF(mImageView.getDrawable().getBounds());
        Matrix matrix = mImageView.getImageMatrix();
        matrix.mapRect(destBounds);
        mGraffitiView.setDestBound(destBounds);
        mGraffitiView.setRenderBufferSize(destBounds.width() * 3, destBounds.height() * 3);
        mGraffitiView.postInvalidate();
    }
}
