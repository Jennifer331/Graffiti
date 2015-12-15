package com.example.administrator.graffiti;

import android.graphics.Paint;
import android.graphics.Path;

/**
 * Created by Lei Xiaoyue on 2015-12-15.
 */
public class Trail {
    public Path path;
    public Paint paint;
    public Trail(Path path,Paint paint){
        this.path = new Path(path);
        this.paint = new Paint(paint);
    }
}
