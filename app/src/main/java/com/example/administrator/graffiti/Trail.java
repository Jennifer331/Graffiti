package com.example.administrator.graffiti;

import android.graphics.Paint;
import android.graphics.Path;

/**
 * Created by Lei Xiaoyue on 2015-12-15.
 */
public class Trail {
    public Path path;
    public Paint paint;
    public boolean valid = false;//if there isn't any point in the path is in the destination area ,this field will be set to false
    public Trail(Path path,Paint paint){
        this.path = new Path(path);
        this.paint = new Paint(paint);
    }
}
