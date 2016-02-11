package com.pl4za.help;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Created by Admin on 12/02/2015.
 */
public class CustomViewPager extends ViewPager {

    private float x1, x2;
    private int position;

    public CustomViewPager(Context context) {
        super(context);
    }

    public CustomViewPager(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void onPageScrolled(int position, float offset, int offsetPixels) {
        super.onPageScrolled(position, offset, offsetPixels);
        this.position=position;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            x1 = ev.getX();
        } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            x2 = ev.getX();
            if (position==0) {
                if (x2 < x1) {
                    return super.onTouchEvent(ev);
                } else {
                    return false;
                }
            } else {
                if (x1 < x2) {
                    return super.onTouchEvent(ev);
                } else {
                    return false;
                }
            }
        }
        return false;
    }
}