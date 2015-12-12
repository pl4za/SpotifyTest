package com.pl4za.help;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by Admin on 12/02/2015.
 */
public class MyViewPager extends ViewPager {

    public MyViewPager(Context context) {
        super(context);
    }

    public MyViewPager(Context context,AttributeSet attributeSet) {
        super(context,attributeSet);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //Log.i("MyViewPager", "Fingers:" + ev.getPointerCount() + " - " + ev.getAction());
        /*
        if (MainActivity.currentPage==2)
            return super.onInterceptTouchEvent(ev);
        else {
            return false;
        }*/
        return false;
    }
}