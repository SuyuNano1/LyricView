package com.nano.lyricview;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class PlayButtonDrawable extends Drawable {

	private ColorStateList mColors ;
	private Path mPath ;
	private Paint mPaint;
	
	public PlayButtonDrawable(){
		this.mPath = new Path() ;
		this.mPaint = new Paint(Paint.ANTI_ALIAS_FLAG) ;
		this.mPaint.setStyle(Paint.Style.FILL_AND_STROKE) ;
		this.mPaint.setStrokeJoin(Paint.Join.ROUND) ;
		this.mPaint.setStrokeWidth(6) ;
		
		int states[][] =  {
			{android.R.attr.state_pressed},
			{}
		};
		int colors[] = {
			0x41FF0000,
			0x91FF0000
		} ;
		
		setColor(new ColorStateList(states,colors)) ;
	}
	
	@Override
	public void draw(Canvas canvas) {
		if(mColors == null){
			return ;
		}
		Rect bounds = getBounds() ;
		mPath.reset() ;
		mPath.moveTo(bounds.left,bounds.top) ;
		mPath.lineTo(bounds.right,bounds.top + bounds.height() / 2) ;
		mPath.lineTo(bounds.left,bounds.bottom) ;
		mPath.close() ;
		canvas.drawPath(mPath,mPaint) ;
	}

	@Override
	public void setAlpha(int alpha) {
	}

	@Override
	public void setColorFilter(ColorFilter colorFilter) {
		mPaint.setColorFilter(colorFilter) ;
	}

	@Override
	public int getOpacity() {
		return 0;
	}

	@Override
	public boolean isStateful() {
		return true ;
	}

	@Override
	protected boolean onStateChange(int[] state) {
		if(mColors == null || !mColors.isStateful()){
			return false ;
		}
		resetCurrentStateColor(state) ;
		return true;
	}
	
	private void resetCurrentStateColor(int[] state){
		this.mPaint.setColor(mColors.getColorForState(
		    state,
			mColors.getDefaultColor()
		)) ;
	}
	
	public void setColor(ColorStateList color){
		this.mColors = color ;
		if(this.mColors != null){
			resetCurrentStateColor(getState()) ;
		}
	}
	
	public void setColor(int color){
		this.setColor(ColorStateList.valueOf(color)); 
	}
	
	public ColorStateList getColor(){
		return mColors;
	}
	
	public void setStrokeWidth(int strokeWidth){
		mPaint.setStrokeWidth(strokeWidth) ;
	}
	
}
