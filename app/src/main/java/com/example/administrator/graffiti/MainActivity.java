package com.example.administrator.graffiti;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;

/**
 * Created by Lei Xiaoyue on 2015-12-15.
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private GraffitiView mView;
    private SeekBar mSeekBar;
    private RadioGroup mRadioGroup;
    private Button mChoosePicBtn, mSaveBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        registerWidget();
        initListener();
    }

    private void registerWidget() {
        mView = (GraffitiView) findViewById(R.id.canvas);
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
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mView.setPaintColor(Config.PAINT_COLORS[checkedId]);
            }
        });
        mRadioGroup.findViewById(0).performClick();
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mView.setStrokeWidth(progress);
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
                Bitmap bitmap = mView.getResult();
                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, System.currentTimeMillis() + ".jpg", "");
                Toast.makeText(getApplicationContext(),"已保存",Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (!mView.undo()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (RESULT_OK == resultCode && Config.PIC_REQUEST_CODE == requestCode) {
            Uri uri = data.getData();
            mView.setSrc(uri);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
