package com.nano.lyricview;

import android.content.Context;
import android.graphics.RectF;
import android.util.TypedValue;

public class CompatUtils {
    
	public static float dp2px(Context context,float dpValue){
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP ,dpValue ,
				context.getResources().getDisplayMetrics()) ;
	}

	public static float sp2px(Context context,float spValue){
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP ,spValue ,
				context.getResources().getDisplayMetrics()) ;
	}
	
	public static boolean expendContains(RectF rectf,float x,float y,float expendSize){
		return x >= rectf.left - expendSize && x <= rectf.right + expendSize &&
		    y >= rectf.top - expendSize && y <= rectf.bottom + expendSize ;
	}
	
	public static String timeFormat(long time){
		if(time <= 0){
			return "00:00" ;
		}
		if(time < 60000){
			return String.format("00:%02d",time / 1000) ;
		}
		return String.format("%02d:%02d",
		    time / 60000,
			time % 60000 / 1000 
		) ;
	}
}
