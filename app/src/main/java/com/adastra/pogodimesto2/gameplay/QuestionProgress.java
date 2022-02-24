package com.adastra.pogodimesto2.gameplay;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

import com.adastra.pogodimesto2.R;

public class QuestionProgress extends LinearLayout {

    Integer mQuestionCount;
    Integer mQuestionOffColor;
    Context mContext;
    LayoutParams mParams;
    View mSelectedView;

    public QuestionProgress(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        setOrientation(LinearLayout.HORIZONTAL);

        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.QuestionProgress, 0, 0);
        try {
            mQuestionCount = typedArray.getInteger(R.styleable.QuestionProgress_questionCount, 7);
            mQuestionOffColor = typedArray.getColor(R.styleable.QuestionProgress_colorDisabled, Color.GRAY);
        } finally {
            typedArray.recycle();
        }

        mParams = new LayoutParams(
                0,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics()),
                1.0f
        );
        mParams.setMargins(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics()),
                0,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics()),
                0
        );

        for (int x = 0; x< mQuestionCount; x++)
        {
            View view = new View(context);
            view.setLayoutParams(mParams);
            view.setBackgroundColor(mQuestionOffColor);
            addView(view);
        }
    }


    public void toggle(int pos, int color) {

        mSelectedView = getChildAt(pos);

        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), mQuestionOffColor, color);
        colorAnimation.setDuration(800);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                mSelectedView.setBackgroundColor((Integer) animator.getAnimatedValue());
            }
        });
        colorAnimation.start();
    }

    public void reset() {
        for (int i = 0; i < mQuestionCount; i++){
            getChildAt(i).setBackgroundColor(mQuestionOffColor);
        }
    }


    @Override
    public int getChildCount() {
        return super.getChildCount();
    }


    public Integer getQuestionCount() {
        return mQuestionCount;
    }


    public void setQuestionCount(Integer questionCount) {
        this.mQuestionCount = questionCount;
        while (questionCount < getChildCount()){
            removeViewAt(getChildCount()-1);
        }

        while (questionCount > getChildCount()){
            View view = new View(mContext);
            view.setLayoutParams(mParams);
            view.setBackgroundColor(Color.BLUE);
            addView(view);
        }
    }


    public Integer getQuestionOffColor() {
        return mQuestionOffColor;
    }


    public void setQuestionOffColor(Integer questionOffColor) {
        this.mQuestionOffColor = questionOffColor;
    }
}
