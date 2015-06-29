package com.shellware.genesisconnect;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class NoTouchSeekBar extends SeekBar {

	public NoTouchSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public NoTouchSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public NoTouchSeekBar(Context context) {
		super(context);
	}
	
	@Override
	public synchronized void setProgress(int progress) {
		super.setProgress(progress);
		
		if (progress == 10) {
			setEnabled(false);
		} else {
			setEnabled(true);
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// inhibit touch
		return true;
	}	
}
