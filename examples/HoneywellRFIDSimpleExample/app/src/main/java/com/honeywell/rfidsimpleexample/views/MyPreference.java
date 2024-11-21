package com.honeywell.rfidsimpleexample.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class MyPreference extends Preference {

    private OnBindViewListener mOnBindViewListener;

    public MyPreference(Context context) {
        super(context);
    }

    public MyPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (null != mOnBindViewListener) {
            mOnBindViewListener.onBindViewCalled(holder);
        }

    }

    public interface OnBindViewListener {
        void onBindViewCalled(PreferenceViewHolder view);
    }

    public void setOnBindViewListener(OnBindViewListener onBindViewListener) {
        mOnBindViewListener = onBindViewListener;
    }
}
