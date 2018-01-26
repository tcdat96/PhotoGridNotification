package com.example.cpu10661.customnotification;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by cpu10661 on 1/25/18.
 */

public class AdjustNumberView extends LinearLayout {

    private TextView mNumberTextView, mDescriptionTextView;

    private int mUpperBound, mLowerBound;
    private int mNumber;

    public AdjustNumberView(Context context) {
        super(context);
        init(null, 0);
    }

    public AdjustNumberView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public AdjustNumberView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(@Nullable AttributeSet attrs, int defStyleAttr) {
        inflateLayout();

        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs, R.styleable.AdjustNumberView, defStyleAttr, 0);
        String description = a.getString(R.styleable.AdjustNumberView_description);
        mUpperBound = a.getInt(R.styleable.AdjustNumberView_upperBound, Integer.MAX_VALUE);
        mLowerBound = a.getInt(R.styleable.AdjustNumberView_lowerBound, Integer.MIN_VALUE);
        a.recycle();

        if (description != null) {
            ((TextView)findViewById(R.id.tv_description)).setText(description);
        }

        mNumber = mLowerBound;
        updateTextView();
    }

    private void inflateLayout() {
        inflate(getContext(), R.layout.adjust_number_view_layout, this);

        mNumberTextView = findViewById(R.id.tv_number);
        mDescriptionTextView = findViewById(R.id.tv_description);

        ImageView mIncreaseImageView = findViewById(R.id.iv_increase_button);
        mIncreaseImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mNumber < mUpperBound) {
                    mNumber++;
                    updateTextView();
                }
            }
        });

        ImageView mDecreaseImageView = findViewById(R.id.iv_decrease_button);
        mDecreaseImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mNumber > mLowerBound) {
                    mNumber--;
                    updateTextView();
                }
            }
        });
    }

    private void updateTextView() {
        mNumberTextView.setText(String.valueOf(mNumber));
    }

    public void setUpperBound(int upperBound) {
        mUpperBound = upperBound;
    }

    public void setLowerBound(int lowerBound) {
        mLowerBound = lowerBound;
    }

    public void setBounds(int upperBound, int lowerBound) {
        setUpperBound(upperBound);
        setLowerBound(lowerBound);
    }

    public void setDescription(String description) {
        mDescriptionTextView.setText(description);
    }

    public int getNumberValue() { return mNumber; }
}
