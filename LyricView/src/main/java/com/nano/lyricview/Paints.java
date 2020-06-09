package com.nano.lyricview;

import android.graphics.Paint;
import android.text.TextPaint;

class Paints {
    
	private Paints() {
	}
	
	public static TextPaint createFontPaint(int textSize,int textColor,Paint.Align align){
		TextPaint p = new TextPaint() ;
		p.setAntiAlias(true) ;
		p.setColor(textColor) ;
		p.setTextSize(textSize);
		p.setTextAlign(align); 
		p.setStyle(Paint.Style.FILL) ; 
		return p; 
	}
	
	public static Paint createStrokePaint(int color,int strokeWidth){
		Paint p = new Paint() ;
		p.setAntiAlias(true) ;
		p.setColor(color) ;
		p.setStrokeWidth(strokeWidth) ;
		p.setStyle(Paint.Style.FILL_AND_STROKE) ;
		p.setStrokeCap(Paint.Cap.ROUND) ;
		p.setStrokeJoin(Paint.Join.ROUND) ;
		return p; 
	}
	
	public static float getAlignCenterOffset(TextPaint paint){
		Paint.FontMetrics metrics = paint.getFontMetrics() ;
		return (-metrics.ascent + metrics.descent) / 2 - metrics.descent ;
	}
	
}
