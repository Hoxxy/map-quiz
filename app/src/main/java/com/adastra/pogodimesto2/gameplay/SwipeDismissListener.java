package com.adastra.pogodimesto2.gameplay;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

public class SwipeDismissListener implements View.OnTouchListener {

    private int mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mAnimationTime;

    private View mView;
    private DismissCallbacks mCallbacks;
    private int mViewHeight = 1; // nije 0 nego 1 da ne dodje do deljenja nulom

    private float mDownX;
    private float mDownY;
    private boolean mSwiping;
    private int mSwipingSlop;
    private VelocityTracker mVelocityTracker;


    /**
     * Callback interface koji obavštava o uspešnom dismiss-u nekog View-a.
     */
    public interface DismissCallbacks {
        /**
         * Poziva se nakon uspešnog dismiss-a
         *
         * @param view
         */
        void onDismiss(View view);
    }

    /**
     * @param view      View za koji će se omogućiti dismiss
     * @param callbacks Callback koji će se pozvati nakon obavljenih funkcija
     */
    public SwipeDismissListener(View view, DismissCallbacks callbacks) {
        mViewHeight = view.getHeight();
        ViewConfiguration vc = ViewConfiguration.get(view.getContext());
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = view.getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);

        mView = view;
        mCallbacks = callbacks;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (mViewHeight < 2) {
            mViewHeight = mView.getHeight();
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                mDownX = event.getRawX();
                mDownY = event.getRawY();
                mVelocityTracker = VelocityTracker.obtain();
                mVelocityTracker.addMovement(event);

                return true;
            }

            case MotionEvent.ACTION_UP: {
                if (mVelocityTracker == null) {
                    break;
                }

                float deltaX = event.getRawX() - mDownX;
                float deltaY = event.getRawY() - mDownY;
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityX = mVelocityTracker.getXVelocity();
                float velocityY = mVelocityTracker.getYVelocity();
                float absVelocityX = Math.abs(velocityX);
                float absVelocityY = Math.abs(velocityY);
                boolean dismiss = false;
                boolean dismissRight = false;
                if (Math.abs(deltaY) > mViewHeight / 2 && mSwiping) {
                    dismiss = true;
                    dismissRight = deltaY > 0;
                } else if (mMinFlingVelocity <= absVelocityY && absVelocityY <= mMaxFlingVelocity
                        && absVelocityX < absVelocityY
                        && absVelocityX < absVelocityY && mSwiping) {

                    dismiss = (velocityY < 0) == (deltaY < 0);
                    //dismissRight = mVelocityTracker.getYVelocity() > 0;
                }
                if (dismiss && !dismissRight) {
                    mView.animate()
                            .translationY(/*dismissRight ? mViewHeight : */-mViewHeight)
                            .alpha(0)
                            .setDuration(mAnimationTime)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mCallbacks.onDismiss(mView);
                                }
                            });
                } else if (mSwiping) {
                    // cancel
                    mView.animate()
                            .translationY(mViewHeight)
                            .alpha(1)
                            .setDuration(mAnimationTime)
                            .setListener(null);
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mDownX = 0;
                mDownY = 0;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                if (mVelocityTracker == null) {
                    break;
                }

                mView.animate()
                        .translationY(mViewHeight)
                        .alpha(1)
                        .setDuration(mAnimationTime)
                        .setListener(null);
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mDownX = 0;
                mDownY = 0;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mVelocityTracker == null) {
                    break;
                }

                mVelocityTracker.addMovement(event);
                float deltaX = event.getRawX() - mDownX;
                float deltaY = event.getRawY() - mDownY;
                if (Math.abs(deltaY) > mSlop && Math.abs(deltaX) < Math.abs(deltaY) / 2) {
                    mSwiping = true;
                    mSwipingSlop = (deltaY > 0 ? mSlop : -mSlop);
                    mView.getParent().requestDisallowInterceptTouchEvent(true);


                    MotionEvent cancelEvent = MotionEvent.obtain(event);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                            (event.getActionIndex() <<
                                    MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                    mView.onTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                }

                if (mSwiping && deltaY < 0) { // delta<0 znaci da je swipe samo na gore
                    mView.setTranslationY(deltaY - mSwipingSlop + mViewHeight);
                    // TODO mozda neki ease-out interpolator
                    mView.setAlpha(Math.max(0f, Math.min(1f, 1f - 2f * Math.abs(deltaY) / mViewHeight)));
                    return true;
                }
                break;
            }
        }
        return false;
    }
}
