package com.nano.lyricview;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.OverScroller;
import java.util.LinkedList;

public class LOverScroller extends OverScroller {

	private LinkedList<Callback> mCallbacks ;
	private boolean isSingleCallback ;
	
	public LOverScroller(Context context) {
		this(context, null) ;
	}

	public LOverScroller(Context context, Interpolator interpolator) {
		super(context, interpolator);
		this.mCallbacks = new LinkedList<>() ;
	}
	
	public void setSingleCallback(boolean isSingleCallback){
		this.isSingleCallback = isSingleCallback ;
	}

	@Override
	public boolean computeScrollOffset() {
		boolean res = super.computeScrollOffset();
		if(res){
			if(isFinished()){
				for(Callback c : mCallbacks){
					c.onScrollAnimationEnd() ;
				}
				mCallbacks.clear();
			}
		}
		return res ;
	}
	
	@Override
	public void abortAnimation() {
		super.abortAnimation();
		mCallbacks.clear() ;
	}

	@Override
	public void startScroll(int startX, int startY, int dx, int dy, int duration) {
		this.startScroll(startX, startY, dx, dy, duration, null) ;
	}

	public void startScroll(int startX, int startY, int dx, int dy, int duration,Callback callback) {
		super.startScroll(startX, startY, dx, dy, duration) ;
		addCallback(callback);
	}

	@Override
	public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY, int overX, int overY) {
		this.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, overX, overY, null);
	}

	public void fling(int startX, int startY, 
	                  int velocityX, int velocityY, 
					  int minX, int maxX, 
					  int minY, int maxY, 
					  int overX, int overY,
					  Callback callback) {
		super.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, overX, overY);
		addCallback(callback) ;
	}

	@Override
	public boolean springBack(int startX, int startY, int minX, int maxX, int minY, int maxY) {
		return this.springBack(startX, startY, minX, maxX, minY, maxY,null);
	}
	
	public boolean springBack(int startX, int startY, int minX, int maxX, int minY, int maxY,Callback callback) {
		boolean superResult =  super.springBack(startX, startY, minX, maxX, minY, maxY);
		addCallback(callback) ;
		return superResult ;
	}
	
	private void addCallback(Callback callback) {
		if (!isFinished() && isSingleCallback) {
			mCallbacks.clear() ;
		}
		if (callback != null) {
			mCallbacks.add(callback) ;
		}
	}
	
	public interface Callback {
		void onScrollAnimationEnd() ;
	}

}
