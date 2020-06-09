package com.nano.lyricview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff;
import android.graphics.LinearGradient;
import android.util.TypedValue;
import android.content.res.TypedArray;

public class EdgeGradientLayout extends FrameLayout {
    
	public static int TOP_MASK = 1 ;
	/**
	 * 1 << 1
	 */
	public static int BOTTOM_MASK = 2 ;

	private Paint mPaint ;
	private int mGradientLength; 
	private int[] colors ;
	private float[] positions ;
	private int mDirectionMask ;
	
	private Paint mTestPaint ;
	
	public EdgeGradientLayout(Context context) {
		this(context, null) ;
	}

    public EdgeGradientLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0) ;
	}

    public EdgeGradientLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr) ;
		this.mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT)) ;
		this.mPaint.setStyle(Paint.Style.FILL) ;
		
		this.mTestPaint = new Paint(Paint.ANTI_ALIAS_FLAG) ;
		this.mTestPaint.setStyle(Paint.Style.FILL) ;
		this.mTestPaint.setAntiAlias(true) ;
		this.mTestPaint.setColor(Color.RED) ;
		
		initEdgeGradientLayout() ;
		initAttrs(attrs,defStyleAttr) ;
		resetLinearGradient() ;
	}

	private void initEdgeGradientLayout() {
		this.colors = new int[]{
			Color.WHITE,
			Color.TRANSPARENT
		} ;
		this.positions = new float[]{0f,1f} ;
		this.mDirectionMask = TOP_MASK | BOTTOM_MASK ;
		this.mGradientLength = 100 ;
	}
	
	private void initAttrs(AttributeSet attrs,int defStyleAttr){
		if(attrs == null) return ;
		TypedArray ta = getContext().obtainStyledAttributes(attrs,R.styleable.EdgeGradient,defStyleAttr,0) ;
		this.mDirectionMask = ta.getInt(R.styleable.EdgeGradient_edgeGradientDirection,mDirectionMask) ;
		this.mGradientLength = ta.getDimensionPixelOffset(R.styleable.EdgeGradient_edgeGradientLength,mGradientLength) ;
		ta.recycle() ;
	}

	private void resetLinearGradient() {
		mPaint.setShader(new LinearGradient(
		    0,0,0,mGradientLength,
			colors,positions,LinearGradient.TileMode.CLAMP
		)) ; 
	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		
		if(mGradientLength <= 0){
			return super.drawChild(canvas, child, drawingTime);
		}
		
		int pl = getPaddingLeft() ;
		int pt = getPaddingTop() ;
		int pr = getPaddingRight() ;
		int pb = getPaddingBottom() ;
		int w = getWidth() ;
		int h = getHeight() ;
		float cx = w / 2 ;
		float cy = h / 2 ;
		
		int layerCount = canvas.saveLayer(pl,pt,
			w - pr,h - pb,
			null,Canvas.ALL_SAVE_FLAG
		) ;
		
		boolean res = super.drawChild(canvas, child, drawingTime);

		if((mDirectionMask & TOP_MASK) != 0){
			canvas.drawRect(
			    pl,pt,w - pr,pt + mGradientLength,mPaint
			); 
		}
		
		if((mDirectionMask & BOTTOM_MASK) != 0){
			canvas.save() ;
			canvas.rotate(180,cx,cy) ;
			canvas.drawRect(
			    pr,pb,w - pl,pb + mGradientLength,mPaint
			); 
			canvas.restore() ;
		}
		
		canvas.restoreToCount(layerCount) ;
		return res ;
	}
	
	public void setGradient(int[] colors,float[] positions){
		this.colors = colors;
		this.positions = positions ;
		resetLinearGradient();
		invalidate() ;
	}
	
	public void setGradient(int[] colors){
		setGradient(colors,this.positions); 
	}
	
	public void setGradient(float[] positions){
		setGradient(this.colors,positions) ;
	}
	
	public void setGradientLength(int len){
		this.mGradientLength = len ;
		resetLinearGradient() ;
		invalidate() ;
	}
	
	public void setDirection(int mask){
		this.mDirectionMask = mask ;
		invalidate() ;
	}
	
}
