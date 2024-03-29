package com.shanewhelan.podcastinate;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

@SuppressWarnings("UnusedDeclaration")
public class SquareImageButton extends ImageButton {
    public SquareImageButton(Context context)
    {
        super(context);
    }

    public SquareImageButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public SquareImageButton(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth()); //Snap to width
    }
}