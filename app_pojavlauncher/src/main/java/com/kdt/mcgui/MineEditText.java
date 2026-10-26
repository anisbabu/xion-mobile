package com.kdt.mcgui;

import android.content.*;
import android.util.*;
import android.graphics.*;
import android.widget.EditText;

public class MineEditText extends androidx.appcompat.widget.AppCompatEditText {
	public MineEditText(Context ctx) {
		super(ctx);
		init();
	}

	public MineEditText(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		init();
	}

	public void init() {
		setBackground(androidx.core.content.res.ResourcesCompat.getDrawable(getResources(), net.kdt.pojavlaunch.R.drawable.xion_input, null));
		int pad = (int) (12 * getResources().getDisplayMetrics().density);
		setPaddingRelative(pad, getPaddingTop(), pad, getPaddingBottom());
	}
}
