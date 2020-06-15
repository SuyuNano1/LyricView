package com.nano.lyricview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import static com.nano.lyricview.CompatUtils.dp2px;
import static com.nano.lyricview.CompatUtils.sp2px;
import android.view.animation.LinearInterpolator; 

/**
 * 歌词控件，用于显示歌词。
 *
 * @author nano1
 */
public class LyricView extends View {

	private static final int MSG_HIDE_INDICATOR = 0 ;
	private static final int MSG_ALIGN_TO_SELECTED_LYRIC = 1 ;
	private static final int MSG_SCROLL_TO_PLAT = 2;
	
	private static final int INVALID_POINTER = -1 ;
	private static final int PLAYBUTTON_EXPAND_SIZE = 75 ;

	/**
	 * 加载中的提示文本
	 */
	private String mLoadingTip ;
	/**
	 * 是否显示加载中的提示文本
	 */
	private boolean mIsShowLoadingTip ;

	/**
	 * 歌词的行间距。
	 */
	private int mLineSpace ;
	
	/**
	 * 歌词最大宽度，计算方式：{@code maxWidth = widthPercent * width - widthMargin*2;}
	 *
	 * @see #mLyricWidthPercent
	 * @see #mLyricWidthMargin
	 */
	private int mLyricMaxWidth ;
	/**
	 * 歌词最大宽度占用整个屏幕的百分比。
	 */
	private float mLyricWidthPercent ;
	/**
	 * 歌词左右边距。
	 */
	private float mLyricWidthMargin ;

	/**
	 * 默认歌词的颜色。
	 */
	private int mLyricColor = Color.BLACK ;
	/**
	 * 当前播放的歌词高亮颜色。
	 */
	private int mHighlightLyricColor = Color.BLUE ;
	/**
	 * 指示器选中颜色。
	 */
	private int mSelectedLyricColor = Color.RED ;

	private View.OnClickListener mOnClickListener;
	
	private OnPlayButtonClickListener mOnPlayBtnClickListener ;
	private boolean mPlayBtnClickable ;

	/** 
	 * 播放按钮的范围，不会计算上 scrollY
	 */
	private RectF mPlayBtnRect ;

	/**
	 * 播放按钮的右边距。
	 */
	private int mPlayButtonRigth ;

	/**
	 * 播放按钮的宽度。
	 */
	private int mPlayButtonWidth ;

	/**
	 * 播放按钮的高度。
	 */
	private int mPlayButtonHeight ;
	private Drawable mPlayButtonDrawable ;

	/**
	 * 指示器高度。
	 */
	private int mIndicatorHeight = 3 ;
	private Drawable mIndicatorDrawable ;

	/**
	 * 显示歌词时间文本的左边距。
	 */
	private int mTimeTextLeft = 12 ;

	private int mTouchSlop ;
	private int mMaximumVelocity ;
	private int mMinimumVelocity ;
	
	private int mDurationOfScrollToPlayLyric ;
	private int mDurationOfAlignToSelectedLyric ;
	private boolean mCanScroll ;

	/**
	 * 是否正在按压播放按钮。
	 */
	private boolean mPressedPlayButton ;
	private boolean mIsClickView ;
	private float mLastTouchY ;
	private int mOverScrollDistance ;

	/**
	 * 当前播放的位置索引。
	 */
	private int mCurrentPlayLyricIndex = -1 ;

	private boolean mIsBeingDragged ;
	private int mActivePointerId = INVALID_POINTER ;
	private int mPlayerActivePointerId = INVALID_POINTER ;
	private boolean mIsAutoScrollToPlayLyric = true ;
	private boolean mIsAutoHideIndicator = true ;
	private boolean mIsAutoAlignToSelectedLyric = true ;
	private boolean mIndicatorShow = false ;

	/**
	 * 执行隐藏 Indicator 的延迟时间。
	 */
	private int mDelayMillisOfHideIndicator = 3600 ;
	
	private int mDelayMillisOfScrollToPlayLyric = 3600 ;

	/**
	 * 执行滚动到指示器指示的位置的延迟时间。
	 */
	private int mDelayMillisOfAlignToSelectedLyric = 700 ;

	private Lyric mLyric ;

	private TextPaint mLyricPaint ;
	private TextPaint mTimePaint ;
	private TextPaint mHintPaint ;
	private TextPaint mLoadingTipPaint ;

	private VelocityTracker mVelocityTracker ;
	private LOverScroller mScroller ;
	private Thread mThread ;
	
	private Runnable mCalculateAllLyricsPositions = new CalculateAllLyricsPositions() ;

	private Handler mTaskHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
				case MSG_ALIGN_TO_SELECTED_LYRIC :
					if(mIndicatorShow){
				    	privateAlignToSelectedLyric(msg.arg1 != 0,msg.arg2) ;
					}
					break ;
				case MSG_SCROLL_TO_PLAT :
					privateScrollToPlayLyric(msg.arg1 != 0,msg.arg2) ;
					break ;
				case MSG_HIDE_INDICATOR :
					hideIndicator() ;
					break ;
			}
		}
	} ;

	/**
	 * 滑动结束时候的回调
	 * 
	 * @see #onTouchOrScrollToEnd()
	 */
	private LOverScroller.Callback mScrollEndCallback = new LOverScroller.Callback(){
		@Override
		public void onScrollAnimationEnd() {
			onTouchOrScrollToEnd() ;
		}
	} ;
	
    public LyricView(Context context) {
		this(context, null) ;
	}

    public LyricView(Context context, AttributeSet attrs) {
		this(context, attrs, 0) ;
	}

    public LyricView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr,R.style.LyricViewStyle) ;

		this.mThread = Thread.currentThread() ;
		
		ViewConfiguration configuration = ViewConfiguration.get(getContext()) ;
		this.mTouchSlop = configuration.getTouchSlop() ;
		this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity() ;
		this.mMinimumVelocity = configuration.getScaledMinimumFlingVelocity() ;
		this.mOverScrollDistance = configuration.getScaledOverscrollDistance() ;

		this.mScroller = new LOverScroller(getContext()) ;
		this.mScroller.setSingleCallback(true) ;
		initPaint() ;
		initLyricView() ;
		initAttrs(attrs,defStyleAttr,R.style.LyricViewStyle) ;
	}

	private void initPaint() {
		this.mLyricPaint = Paints.createFontPaint(
		    (int)sp2px(getContext(), 16),
			mLyricColor,
			Paint.Align.CENTER
		);

		this.mTimePaint = Paints.createFontPaint(
		    (int)sp2px(getContext(), 12),
			Color.GRAY,
			Paint.Align.RIGHT
		);

		this.mHintPaint = Paints.createFontPaint(
		    (int)sp2px(getContext(), 16),
			Color.BLACK,
			Paint.Align.CENTER
		) ;

		this.mLoadingTipPaint = Paints.createFontPaint(
		    (int)sp2px(getContext(), 16),
			Color.BLACK,
			Paint.Align.CENTER
		) ;
	}

	/** 
	 * <p>初始化歌词控件的默认值。</p>
	 */
	private void initLyricView() {
		this.mLoadingTip = "Loading" ;
		this.mPlayButtonRigth = (int)dp2px(getContext(), 4) ;
		this.mLineSpace = (int)dp2px(getContext(), 16) ;
		this.mLyricWidthPercent = 0.7f ;
		this.mDurationOfScrollToPlayLyric = 1400 ;
		this.mDurationOfAlignToSelectedLyric = 400 ;
		this.mPlayButtonWidth = (int)dp2px(getContext(), 12) ;
		this.mPlayButtonHeight = (int)dp2px(getContext(), 14) ;
		this.mPlayButtonDrawable = new PlayButtonDrawable() ;
		GradientDrawable indicatorDrawable = new GradientDrawable() ;
		indicatorDrawable.setCornerRadius(mIndicatorHeight) ;
		indicatorDrawable.setColor(Color.RED & 0x00FFFFFF | (172 << 24)) ;
		this.mIndicatorDrawable = indicatorDrawable ;
	}
	
	private void initAttrs(AttributeSet attrs,int defStyleAttr,int defStyleRes){
		TypedArray ta = getContext().obtainStyledAttributes(attrs,R.styleable.LyricView,defStyleAttr,defStyleRes) ;
		if(ta == null)
			return ;
			
		int defaultTextSize = (int)sp2px(getContext(),16) ;
			
		this.mLineSpace = ta.getDimensionPixelSize(R.styleable.LyricView_lineSpace,mLineSpace) ;
		
		this.mLyricWidthPercent = ta.getFraction(R.styleable.LyricView_lyric_max_widthPercent,1,1,mLyricWidthPercent) ;
		this.mLyricWidthMargin = ta.getDimensionPixelSize(R.styleable.LyricView_lyric_max_widthMargin,(int)mLyricWidthMargin) ;
		
		this.mPlayButtonWidth = ta.getDimensionPixelSize(R.styleable.LyricView_play_button_width,mPlayButtonWidth) ;
		this.mPlayButtonHeight = ta.getDimensionPixelSize(R.styleable.LyricView_play_button_height,mPlayButtonHeight) ;
		this.mPlayButtonRigth = ta.getDimensionPixelSize(R.styleable.LyricView_play_button_right,this.mPlayButtonRigth) ;
		
		if(ta.hasValue(R.styleable.LyricView_play_button)){
		    setPlayButtonDrawable(ta.getDrawable(R.styleable.LyricView_play_button)) ;
		}
		
		this.mIndicatorHeight = ta.getDimensionPixelSize(R.styleable.LyricView_indicator_height,mIndicatorHeight) ;
		if(ta.hasValue(R.styleable.LyricView_indicator)){
			setIndicatorDrawable(ta.getDrawable(R.styleable.LyricView_indicator)) ;
		}
		
		int timeTextSize = ta.getDimensionPixelSize(R.styleable.LyricView_time_textSize,(int)sp2px(getContext(),12)) ;
		this.mTimePaint.setTextSize(timeTextSize) ;
		int timeTextColor = ta.getColor(R.styleable.LyricView_time_textColor,mTimePaint.getColor()) ;
		this.mTimePaint.setColor(timeTextColor) ;
		this.mTimeTextLeft = ta.getDimensionPixelSize(R.styleable.LyricView_time_textLeft,this.mTimeTextLeft) ;
		
		this.mLyricColor = ta.getColor(R.styleable.LyricView_lyric_textColor,mLyricColor) ;
		this.mHighlightLyricColor = ta.getColor(R.styleable.LyricView_lyric_highlight_textColor,mHighlightLyricColor) ;
		this.mSelectedLyricColor = ta.getColor(R.styleable.LyricView_lyric_selected_textColor,mSelectedLyricColor) ;
		
		int lyricTextSize = ta.getDimensionPixelSize(R.styleable.LyricView_lyric_textSize,defaultTextSize) ;
		this.mLyricPaint.setTextSize(lyricTextSize) ;
		
		int hintColor = ta.getColor(R.styleable.LyricView_hint_textColor,mHintPaint.getColor()) ;
		this.mHintPaint.setColor(hintColor) ;
		int hintTextSize = ta.getDimensionPixelSize(R.styleable.LyricView_hint_textSize,defaultTextSize) ;
		this.mHintPaint.setTextSize(hintTextSize) ;
		
		int loadingTipColor = ta.getColor(R.styleable.LyricView_loading_tip_textColor,mLoadingTipPaint.getColor()) ;
		this.mLoadingTipPaint.setColor(loadingTipColor); 
		int loadingTipSize = ta.getDimensionPixelSize(R.styleable.LyricView_loading_tip_textSize,defaultTextSize) ;
		this.mLoadingTipPaint.setTextSize(loadingTipSize) ;
		if(ta.hasValue(R.styleable.LyricView_loading_tip_text)){
			this.mLoadingTip = ta.getString(R.styleable.LyricView_loading_tip_text) ;
		}
		
		int duration ,delayMillis;
		
		duration = ta.getInteger(R.styleable.LyricView_duration_of_scroll_to_play_lyric,mDurationOfScrollToPlayLyric) ;
		this.mDurationOfScrollToPlayLyric = duration < 0 ? 0 : duration ;
		
		duration = ta.getInteger(R.styleable.LyricView_duration_of_align_to_selected_lyric,mDurationOfAlignToSelectedLyric) ;
		this.mDurationOfAlignToSelectedLyric = duration < 0 ? 0 : duration ;
		
		delayMillis = ta.getInteger(R.styleable.LyricView_delayMillis_of_align_to_selected_lyric,mDelayMillisOfAlignToSelectedLyric) ;
		this.mDelayMillisOfAlignToSelectedLyric = delayMillis < 0 ? 0 : delayMillis ;
		
		delayMillis = ta.getInteger(R.styleable.LyricView_delayMillis_of_hide_indicator,mDelayMillisOfHideIndicator); 
		this.mDelayMillisOfHideIndicator = delayMillis < 0 ? 0 : delayMillis ;
		
		delayMillis = ta.getInteger(R.styleable.LyricView_delayMillis_of_scroll_to_play_lyric,mDelayMillisOfScrollToPlayLyric) ;
		this.mDelayMillisOfScrollToPlayLyric = delayMillis ;
		ta.recycle() ;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		resetPlayBtnRect() ;
		measureLyricMaxWidth() ;
	}

	private void measureLyricMaxWidth() {
		this.mLyricMaxWidth = (int)(getMeasuredWidth() * mLyricWidthPercent- mLyricWidthMargin) ;
	}

	/**
	 * 重置 PlayBtn 的范围(Rect)
	 */
	private void resetPlayBtnRect() {
		if (this.mPlayBtnRect == null) {
			this.mPlayBtnRect = new RectF() ;
		}
		float halfH = mPlayButtonHeight / 2 + mIndicatorHeight / 2 ;
		this.mPlayBtnRect.left = getPaddingLeft()  ;
		this.mPlayBtnRect.top = getIndicatorViewY() - halfH;
		this.mPlayBtnRect.right = this.mPlayBtnRect.left + mPlayButtonWidth ;
		this.mPlayBtnRect.bottom = this.mPlayBtnRect.top + mPlayButtonHeight + mIndicatorHeight;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (this.mIsShowLoadingTip) {
			drawTextInCenterOfCanvas(canvas, mLoadingTip, mLoadingTipPaint) ;
			return ;
		}

		if (mLyric == null || !mLyric.isComputed()) {
			return ;
		}
		
		if (mLyric.count() == 0) {
			drawTextInCenterOfCanvas(canvas, mLyric.getHint(), mHintPaint) ;
			return ;
		}

		if (this.mIndicatorShow) {
			drawIndicatorAndTime(canvas) ;
			drawLyric(canvas) ;
			drawPlayer(canvas) ;
		}else{
			drawLyric(canvas) ;
		}
	}

	/**
	 * 绘制歌词
	 */
	private void drawLyric(Canvas canvas) {

		int count = getLyricCount() ;
		float top = getTopOffset() ;
		float x = getMeasuredWidth() / 2 ;

		for (int i = 0;i < count;i ++) {	
			LyricLine line = mLyric.get(i) ;

			// 根据歌词文本的位置进行计算
			float y = top + line.getPosition() ; 
			float dy = y - getScrollY() ;

			if (dy + line.lineHeight < 0) {
				// 表示在屏幕外上方 continue
				continue ;
			}

			if (dy > getHeight()) {
				// 表示在屏幕外下方，后面的歌词也没必要计算和绘制了。
				break ;
			}

			if (i == mCurrentPlayLyricIndex) { // 表示当前歌词是播放中的歌词
				mLyricPaint.setColor(mHighlightLyricColor) ;
			} else if (isInIndicatedArea(line) && this.mIndicatorShow) {
				mLyricPaint.setColor(mSelectedLyricColor) ;
			} else {
				mLyricPaint.setColor(mLyricColor) ;
			}

			StaticLayout staticLayout = obtainStaticLayout(line) ;
			canvas.save() ;
			canvas.translate(x, y) ;
			staticLayout.draw(canvas) ;
			canvas.restore() ;
		}
	}

	/**
	 * 在画布中心绘制文本。
	 */
	private void drawTextInCenterOfCanvas(Canvas canvas, String text, Paint paint) {
		if (TextUtils.isEmpty(text)) {
			return ;
		}
		float y = getScrollY() + getHeight() * 0.5f 
		    + Paints.getAlignCenterOffset(mHintPaint) ;
		float x = getWidth() * 0.5f ;
		canvas.drawText(text, x, y, paint) ;
	}

	/**
	 * 绘制播放按钮。
	 */
	private void drawPlayer(Canvas canvas) {
		if (mPlayButtonDrawable != null) {
			mPlayButtonDrawable.setBounds(
			    (int)mPlayBtnRect.left,
				(int)mPlayBtnRect.top + getScrollY(),
				(int)mPlayBtnRect.right,
				(int)mPlayBtnRect.bottom + getScrollY()
			); 
			mPlayButtonDrawable.draw(canvas) ;
		}
	}

	/**
	 * 绘制指示器和歌词时间文本。
	 */
	private void drawIndicatorAndTime(Canvas canvas) {
		int indicatorRight = getPaddingRight() ;

		// 获取当前指示器区域内的歌词索引，该方法会进行越界处理。
		// 当前歌词无效将返回 -1
		int pos = getSelectedLyricIndex() ;
		if (pos >= 0) { // 绘制时间文本
			LyricLine line = mLyric.get(pos) ;
			int width = (int)mTimePaint.measureText(
			    line.getFormatedTime()
			) ;
			// 计算 Indicator 与右边的距离
			indicatorRight += this.mTimeTextLeft + width ;

			float x = getWidth() - getPaddingRight() ;
			float y = getScrollY() + getIndicatorViewY() 
			    // 根据画笔计算文本中间(centerVertical)与 baseline 对齐的偏移量。
			    + Paints.getAlignCenterOffset(mTimePaint) ; 
			canvas.drawText(line.getFormatedTime(), x, y, mTimePaint) ;
		}

		if (mIndicatorDrawable != null) { // 绘制指示器

		    int startX = (int)(getPaddingLeft() + mPlayButtonRigth + mPlayBtnRect.width()) ;
			int startY = (int)(getScrollY() + getIndicatorViewY()) ;
			int width = getWidth() - (startX + indicatorRight) ;

			mIndicatorDrawable.setBounds(
			    startX, startY,
				startX + width, startY + mIndicatorHeight
			) ;
			mIndicatorDrawable.draw(canvas) ;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int actionMasked = event.getActionMasked() ;
		if (mVelocityTracker != null) {
			mVelocityTracker.addMovement(event) ;
		}
		switch (actionMasked) {
			case MotionEvent.ACTION_DOWN :

				float y = event.getY() ;
				float x = event.getX() ;

				removeCallbacks() ;

				// 是否点击到 playbutton 
				if (this.mPressedPlayButton = isTouchPlayButton(x, y)) {
					// 记住按压播放按钮的手指id
					this.mPlayerActivePointerId = event.getPointerId(0) ;
					// 如果点击到 playbutton 就更改 playbutton drawable 的状态。
					changePlayButtonDrawableState() ;
				} else {
					this.mActivePointerId = event.getPointerId(0) ; 
				}
				this.mCanScroll = isValidLyric() && !mIsShowLoadingTip;
				this.mIsClickView = !this.mPressedPlayButton ;
				this.mLastTouchY = y ;
				
				// 如果Scroller还没有计算完成，则取消。
				if (!mScroller.isFinished()) {
					// 取消动画。
					mScroller.abortAnimation() ;
					// 不触发单击事件。
					mIsClickView = false ;
				}
				
				initOrResetVelocityTracker() ;
				mVelocityTracker.addMovement(event) ;
				return mCanScroll || mPressedPlayButton || mIsClickView ;

			case MotionEvent.ACTION_MOVE :
				
				if (mPlayerActivePointerId != INVALID_POINTER) {
					// 表示有手指正在触摸播放按钮
					touchPlayButton(event) ;
				}
				
				final int activePointerIndex = event.findPointerIndex(mActivePointerId) ;
				if (activePointerIndex == -1 || !mCanScroll) {
					break ;
				}
				
				float moveY = event.getY(activePointerIndex) ;
				int dy = (int)(mLastTouchY - moveY) ;

				if (!mIsBeingDragged && Math.abs(dy) > mTouchSlop) {
					if (dy > 0) {
						dy -= mTouchSlop ;
					} else if (dy < 0) {
						dy += mTouchSlop ;
					}
					mIsBeingDragged = true ;
					mIndicatorShow = true ;
					mIsClickView = false ;
				}

				if (this.mIsBeingDragged) {	
					int range = (int)getScrollRange() ;
					if (overScrollBy(0, dy, 0, getScrollY(), 0, range, 0, mOverScrollDistance, true)) {
						mVelocityTracker.clear() ;
					}
					this.mLastTouchY = moveY ;
				}
				break ;

			case MotionEvent.ACTION_UP :

				if (this.mPressedPlayButton) {
					onPlayButtonDrawableClick() ;
				}else if(!this.mIsBeingDragged && this.mIsClickView){
					if(mOnClickListener != null && isClickable()){
						mOnClickListener.onClick(this) ;
					}
					this.mIsClickView = false ;
				}

				if (this.mIsBeingDragged) {
					
					this.mIsBeingDragged = false ;
					// 计算当前速度
					mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity) ;	
					int velocityY = (int)mVelocityTracker.getYVelocity(mActivePointerId) ;
					
					// 当速度大于最小速度值，开始fling。
					if (Math.abs(velocityY) > mMinimumVelocity) {
						fling(-velocityY) ;
					} else if (mScroller.springBack(0, getScrollY(), 0, 0, 0,
						    (int)getScrollRange(), mScrollEndCallback)) { 
						postInvalidateOnAnimation();
					} else {
						onTouchOrScrollToEnd() ;
					}		
				}else{
					onTouchOrScrollToEnd() ;
				}
				
				recycleVelocityTracker() ;
				mActivePointerId = mPlayerActivePointerId = INVALID_POINTER ;
				break ;
				
			case MotionEvent.ACTION_CANCEL :
				recycleVelocityTracker() ;
				cancelPlayButtonPressed() ;
				mIsBeingDragged = false ;
				mActivePointerId = mPlayerActivePointerId = INVALID_POINTER ;
				onTouchOrScrollToEnd() ;
				break ;

			case MotionEvent.ACTION_POINTER_DOWN :
				final int index = event.getActionIndex() ;
				final float pointerDownX = event.getX(index) ;
				final float pointerDownY = event.getY(index) ;
				
				// 新的手指触碰到了按钮。
				if (isTouchPlayButton(pointerDownX, pointerDownY)) {
					// 记住触屏到播放按钮的 PointerId
					this.mPlayerActivePointerId = event.getPointerId(index) ;
					this.mPressedPlayButton = true ;
					changePlayButtonDrawableState() ;		
				} else {
					this.mActivePointerId = event.getPointerId(index) ;
					initOrResetVelocityTracker() ;
					this.mLastTouchY = event.getY(index) ;
				}
				break ;

			case MotionEvent.ACTION_POINTER_UP :
				final int pointerIndex = event.getActionIndex() ;
				final int pointerId = event.getPointerId(pointerIndex) ;
				
				// 判断弹起来的手指是否是触摸播放按钮的手指
				if(pointerId == mPlayerActivePointerId){
					// 触发点击事件。
					if(this.mPressedPlayButton){
						onPlayButtonDrawableClick() ;
					}
				}
				
				int newIndex = pointerIndex == 0 ? 1 : 0 ;
				int newPointerId = event.findPointerIndex(newIndex) ;
				final float newPointerX = event.getX(newIndex) ;
				final float newPointerY = event.getY(newIndex) ;
				
				// 判断新的 Pointer 是否正在触摸播放按钮
				if(this.mPlayBtnClickable && isTouchPlayButton(newPointerX,newPointerY)){
					if(newPointerId == mActivePointerId){
						// 新的 PointerId 属于滑动事件的 Id
						break ;
					}
					this.mPlayerActivePointerId = newPointerId ;
					this.mPressedPlayButton = true ;
					changePlayButtonDrawableState() ;
				}else if(pointerId == mActivePointerId){
					this.mActivePointerId = newPointerId ;
					initOrResetVelocityTracker() ;
					this.mLastTouchY = newPointerY ;
				}
				break ;
		}
		return true;
	}

	private void touchPlayButton(MotionEvent event) {
		if (this.mPressedPlayButton) {
			final int index = event.findPointerIndex(mPlayerActivePointerId) ;
			if (index == -1) {
				return ;
			}
			float x = event.getX(index) ;
			float y = event.getY(index) ; 
			if (this.mPressedPlayButton) {
				if (!isTouchPlayButton(x, y)) {
					cancelPlayButtonPressed() ;
				}
			}
		}
	}
	
	private void cancelPlayButtonPressed(){
		this.mPressedPlayButton = false ;
		this.mPlayerActivePointerId = INVALID_POINTER ;
		changePlayButtonDrawableState() ;
	}
	
	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			scrollTo(0, mScroller.getCurrY()) ;	
			postInvalidateOnAnimation() ;
	    }
	}

	@Override
	protected int computeVerticalScrollRange() {
		final int contentHeight = getHeight() ;
		if (!isValidLyric()) {
			return contentHeight ;
		}
		final LyricLine lastLine = mLyric.getLast() ;
		float scrollRange = lastLine.position + lastLine.lineHeight 
		    + getTopOffset() + getBottomOffset() ;

		final float overScrollBottom = Math.max(0, scrollRange - contentHeight) ;
		final int scrollY = getScrollY() ;
		if (scrollY < 0) {
			scrollRange -= scrollY ;
		} else if (scrollY > overScrollBottom) {
			scrollRange += scrollY - overScrollBottom ;
		}
		return (int)scrollRange ;
	}

	@Override
	protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
		scrollTo(scrollX, scrollY) ;
	}

	/**
	 * 滚动到指定位置。
	 *
	 * 调用该方法并不会触发 {@link #onScrollEnd()} 事件。
	 * 
	 * @param index 指定的歌词索引。
	 * @param offset Y轴偏移量。
	 * @param isAnimation 是否执行滚的动画。
	 * @param duration 动画时长，负数为默认时长。如果为0，将不会执行动画。
	 */
	private void scrollToPosition(int index, float offset, boolean isAnimation, int duration) {
		int lyricCount = getLyricCount() ;
		if (index >= lyricCount) {
			index = lyricCount - 1 ;
		}
		if (index < 0) {
			index = 0 ;
		}
		if (duration < 0) {
			duration = 250 ;
		}
		LyricLine line = mLyric.get(index) ;
		float y = line.position + line.lineHeight * 0.5f + getTopOffset() - offset ;
		int dy = (int)(y - getScrollY()) ;
		if (dy == 0) return ;
		if (!mScroller.isFinished()) {
			mScroller.abortAnimation() ;
		}
		if (isAnimation && duration != 0) {
		    mScroller.startScroll(0, getScrollY(), 0, dy, duration) ;
			postInvalidateOnAnimation() ;
		} else {
			scrollTo(0, (int)y) ;
		}
	}

	/**
	 * 是否触摸到播放按钮。 
	 */
	private boolean isTouchPlayButton(float eventX, float eventY) {
		return this.mPlayBtnClickable && this.mIndicatorShow && CompatUtils.expendContains(
		    mPlayBtnRect, eventX, eventY, PLAYBUTTON_EXPAND_SIZE
		) ;
	}

	/**
	 * 获取 Indicator 在视图高度中的坐标，视图高度是指当前 View 显示在屏幕中的高度。
	 */
	private float getIndicatorViewY() {
		return getMeasuredHeight() * .5f ;
	}

	/**
	 * 获取所有歌词在画布中上边的偏移量，这个值决定了可滚动的范围。
	 */
	private float getTopOffset() {
		return getHeight() * .5f ;
	}

	/**
	 * 获取所有歌词在画布中下边的偏移量，这个值决定了可滚动的范围。
	 */
	private float getBottomOffset() {
		return getHeight() * .5f ;
	}

	/**
	 * 滚动到播放的歌词，歌词上边的偏移量。
	 */
	private float getScrollTopOffset() {
		return getHeight() * .4f ;
	}

	/**
	 * 获取歌词滚动的范围。
	 */
	protected float getScrollRange() {
		int count = getLyricCount() ;
		if (count <= 0) return 0 ;
		LyricLine lastLine = mLyric.get(count - 1) ;
		return lastLine.position + lastLine.lineHeight - 
		    getHeight() + getTopOffset() + getBottomOffset() ;
	}

	/**
	 * 判断指定歌词是否在被 {@code Indicator} 选中的区域。
	 *
	 * @param line 要判断的歌词
	 */
	private boolean isInIndicatedArea(LyricLine line) {
		float indicatotY = getScrollY() + getIndicatorViewY() ;
		float lineY = line.position + getTopOffset() ;
		return indicatotY >= lineY - mLineSpace / 2 &&
		    indicatotY < lineY + line.lineHeight + mLineSpace / 2 ;
	}

	private void fling(int velocityY) {
		int scrollRange = (int)getScrollRange() ;
		if ((getScrollY() > 0 || velocityY > 0) && 
		    (getScrollY() < scrollRange || velocityY < 0)) {
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation() ;
			}
			mScroller.fling(
			    0, getScrollY(), 
				0, velocityY,
				0, 0, 
				0, scrollRange, 
			    0, mOverScrollDistance,
				mScrollEndCallback 
			) ;
			postInvalidateOnAnimation();
		}
	}

	private StaticLayout obtainStaticLayout(LyricLine line) {
		return StaticLayout.Builder
			.obtain(line.getLyric(),
					0, line.getLyric().length(),
					mLyricPaint, LyricView.this.mLyricMaxWidth)
			.setAlignment(Layout.Alignment.ALIGN_NORMAL)
			.build() ;
	}

	/**
	 * 判断当前歌词是否有效。
	 */
	private boolean isValidLyric() {
		return mLyric != null && mLyric.isComputed() && mLyric.count() != 0 ;
	}

	private void initOrResetVelocityTracker() {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain() ;
		} else {
			mVelocityTracker.clear() ;
		}
	}

	private void recycleVelocityTracker() {
		if (mVelocityTracker != null) {
			mVelocityTracker.recycle() ;
			mVelocityTracker = null ;
		}
	}

	private int getLyricCount() {
		return mLyric == null ? 0 : mLyric.count();
	}

	/**
	 * 更改当前播放按钮 Drawable 的状态。
	 *
	 * @return 是否更改。
	 */
	private boolean changePlayButtonDrawableState() {
		if (mPlayButtonDrawable != null && mPlayButtonDrawable.isStateful()) {
			mPlayButtonDrawable.setState(
			    mPressedPlayButton ? new int[]{android.R.attr.state_pressed} : new int[]{}
			) ;
			invalidatePlayBtn() ;
			return true;
		}
		return false ;
	}

	public void removeCallbacks() {
		mTaskHandler.removeMessages(MSG_HIDE_INDICATOR) ;
		mTaskHandler.removeMessages(MSG_ALIGN_TO_SELECTED_LYRIC) ;
		mTaskHandler.removeMessages(MSG_SCROLL_TO_PLAT) ;
	}
	
	protected final void autoInvalidate(int l,int t,int r,int b){
		if (Thread.currentThread() == this.mThread) {
			invalidate(l,t,r,b) ;
		} else {
			postInvalidate(l,t,r,b) ;
		}
	}

	protected final void autoInvalidate() {
		if (Thread.currentThread() == this.mThread) {
			invalidate() ;
		} else {
			postInvalidate() ;
		}
	}

	protected final void invalidatePlayBtn() {
		if (mPlayBtnRect == null) {
			return ;
		}
		autoInvalidate(
		    (int)mPlayBtnRect.left,
			(int)mPlayBtnRect.top + getScrollY(),
			(int)mPlayBtnRect.right,
			(int)mPlayBtnRect.bottom + getScrollY()
		) ;
	}

	/**
	 * 当手指触摸结束或者滚动结束会触发该方法。
	 */
	protected void onTouchOrScrollToEnd() {
		if(mIsAutoAlignToSelectedLyric){
		    alignToSelectedLyricDelayed(true,mDurationOfAlignToSelectedLyric) ;
		}
		if(mIsAutoHideIndicator){
		    hideIndicatorDelayed() ;
		}
		if(mIsAutoScrollToPlayLyric){
			scrollToPlayLyricDelayed(true,mDurationOfScrollToPlayLyric) ;
		}
	}

	/**
	 * 当歌词发生改变时会调用该方法。
	 */
	protected void onLyricChanged(Lyric newLyric) {
		mPressedPlayButton = false ;
		mIndicatorShow = false ;
		mIsBeingDragged = false ;
		mActivePointerId = mPlayerActivePointerId = INVALID_POINTER ;
		removeCallbacks() ;
		changePlayButtonDrawableState() ;
		if(isValidLyric()){
			setPlayLyricIndex(0) ;
			scrollToPlayLyric() ;
		}else{
		    scrollTo(0, 0) ;
		}
		
	}
	
	protected void onPlayButtonDrawableClick(){
		cancelPlayButtonPressed() ;
		if (mOnPlayBtnClickListener != null) {
			int pos = getSelectedLyricIndex() ;
			if (pos >= 0) {
				mOnPlayBtnClickListener.onPlayBtnClick(
					pos, mLyric.get(pos)
				) ;
			}
		}
	}

	// --------------------------------------------------------------------------------
	//                                PUBLIC METHOD
	// --------------------------------------------------------------------------------

	@Override
	public void setOnClickListener(View.OnClickListener l) {
		if(!isClickable()){
			setClickable(true) ;
		}
		this.mOnClickListener = l ;
	}
	
	//------------------------------- Sets Lyric -------------------------------//
	
	/**
	 * 加载歌词，加载歌词后不能对歌词进行更改。
	 */
	public void loadLyric(Lyric lyric) {
		this.mLyric = lyric ;
		if (lyric == null) {
			invalidate() ;
			return ;
		}
		mLyric.sort() ;
		removeCallbacks(mCalculateAllLyricsPositions) ;
		post(mCalculateAllLyricsPositions) ;
	}
	
	public Lyric getLyric(){
		return mLyric ;
	}
	
	//------------------------------- Indicator Control -------------------------------//
	
	/**
	 * 直接隐藏指示器。
	 */
	public void hideIndicator(){
		if(mIndicatorShow){
			mIndicatorShow = false ;
			autoInvalidate() ;
		}
	}
	
	public void hideIndicatorDelayed(){
		hideIndicatotDelayed(mDelayMillisOfHideIndicator) ;
	}
	
	public void hideIndicatotDelayed(int delayMillis){
		if(delayMillis > 0){
			Message msg = mTaskHandler.obtainMessage(MSG_HIDE_INDICATOR) ;
			mTaskHandler.sendMessageDelayed(msg,delayMillis) ;
		}else{
			hideIndicator() ;
		}
	}
	
	public void showIndicator(){
		if(!mIndicatorShow){
			mIndicatorShow = true ;
			autoInvalidate() ;
		}
	}
	
	//------------------------------- Indicator Params -------------------------------//
	
	public void setIndicatorHeight(int height){
		this.mIndicatorHeight = height ;
		autoInvalidate() ;
	}

	public void setIndicatorDrawable(Drawable drawable){
		if(this.mIndicatorDrawable == drawable){
			return ;
		}
		mIndicatorDrawable = drawable ;
		autoInvalidate() ;
	}

	public Drawable getIndicatorDrawable(){
		return mIndicatorDrawable ;
	}

	public void setIndicatorColor(int color){
		if(this.mIndicatorDrawable instanceof ColorDrawable){
			((ColorDrawable)mIndicatorDrawable).setColor(color) ;
		}else if(this.mIndicatorDrawable instanceof GradientDrawable){
			((GradientDrawable)mIndicatorDrawable).setColor(color) ;
		}else {
			GradientDrawable indicatorDrawable = new GradientDrawable() ;
			indicatorDrawable.setColor(color) ;
			indicatorDrawable.setCornerRadius(mIndicatorHeight/2) ;
			setIndicatorDrawable(indicatorDrawable) ; 
		}
	}
	
	public boolean isIndicatorShow(){
		return mIndicatorShow ;
	}
	
	public void setDelayMillisOfHideIndicator(int delayMillis){
		if(delayMillis < 0){
			delayMillis = 0 ;
		}
		this.mDelayMillisOfHideIndicator = delayMillis ;
	}
	
	public int getDelayMillisOfHideIndicator(){
		return mDelayMillisOfHideIndicator ;
	}
	
	//------------------------------- Play Button Click -------------------------------//
	
	/**
	 * 设置播放按钮的点击事件。
	 */
	public void setOnPlayButtonClickListener(OnPlayButtonClickListener onPlayBtnClickListener) {
		setPlayButtonClickable(true) ;
		this.mOnPlayBtnClickListener = onPlayBtnClickListener ;
	}

	/**
	 * 设置播放按钮是否是可以点击的。
	 */
	public void setPlayButtonClickable(boolean clickable) {
		this.mPlayBtnClickable = clickable ;
	}

	public boolean isPlayButtonClickable(){
		return this.mPlayBtnClickable ;
	}
	
	//------------------------------- Play Button Shape -------------------------------//
	
	/**
	 * 修改播放按钮Drawable。
	 */
	public void setPlayButtonDrawable(Drawable drawable){
		if(drawable == mPlayButtonDrawable){
			return ;
		}
		this.mPlayButtonDrawable = drawable;
		changePlayButtonDrawableState() ;
		invalidatePlayBtn() ;
	}

	/**
	 * 获取播放按钮。
	 */
	public Drawable getPlayButtonDrawable(){
		return this.mPlayButtonDrawable ;
	}
	
	/** 
	 * 设置播放按钮右边距。
	 */
	public void setPlayButtonRight(int right){
		this.mPlayButtonRigth = right ;
		autoInvalidate() ;
	}

	/**
	 * 设置播放按钮宽度。
	 */
	public void setPlayButtonWidth(int width){
		this.mPlayButtonWidth = width ;
		resetPlayBtnRect() ;
		invalidatePlayBtn() ;
	}

	/**
	 * 设置播放按钮高度。
	 */
	public void setPlayButtonHeight(int height){
		this.mPlayButtonHeight = height ;
		resetPlayBtnRect() ;
		invalidatePlayBtn() ;
	}

	/**
	 * 设置播放按钮大小。
	 *
	 * @param width 播放按钮宽度。
	 * @param height 播放按钮高度。
	 */
	public void setPlayButtonSize(int width,int height){
		this.mPlayButtonWidth = width ;
		this.mPlayButtonHeight = height ;
		resetPlayBtnRect() ;
		invalidatePlayBtn() ;
	}
	
	//------------------------------- Hint Text Shape -------------------------------//
	
	/*
	 * 设置提示文本颜色。
	 *
	 * @see Lyric#setHint(String)
	 */
	public void setHintTextColor(int color) {
		this.mHintPaint.setColor(color) ;
		if (!isValidLyric()) {
			autoInvalidate() ;
		}
	}

	/**
	 * 设置提示文本大小。
	 *
	 * @param size px
	 *
	 * @see Lyric#setHint(String)
	 */
	public void setHintTextSize(float size) {
		this.mHintPaint.setTextSize(size) ;
		if (!isValidLyric()) {
			autoInvalidate() ;
		}
	}
	
	
	//------------------------------- Lyric Time Text -------------------------------//
	
	/**
	 * 设置时间文本左边距。
	 */
	public void setTimeTextLeft(int left){
		this.mTimeTextLeft = left ;
		autoInvalidate() ;
	}

	/**
	 * 设置歌词时间文本颜色。
	 */
	public void setTimeTextColor(int color){
		this.mTimePaint.setColor(color) ;
		autoInvalidate() ;
	}

	/**
	 * 设置歌词时间文本大小。
	 */
	public void setTimeTextSize(float timeSize){
		this.mTimePaint.setTextSize(timeSize) ;
	}
	
	//------------------------------- Loading Tip Control -------------------------------//
	
	/**
	 * 设置是否显示加载中的提示文本。
	 */
	public void setLoadingTipShow(boolean isShow) {
		this.mIsShowLoadingTip = isShow ;
		autoInvalidate() ;
	}
	
	//------------------------------- Loading Tip Shape -------------------------------//
	
	/**
	 * 设置加载时的提示文本。
	 */
	public void setLoadingTip(String loadingTip) {
		this.mLoadingTip = loadingTip ;
		if (this.mIsShowLoadingTip && !TextUtils.isEmpty(this.mLoadingTip)) {
			autoInvalidate() ;
		}
	}

	/**
	 * 获取加载提示文本。
	 */
	public String getLoadingTip(){
		return this.mLoadingTip ;
	}
	
	/**
	 * 设置加载时的提示文本颜色。
	 */
	public void setLoadingTipColor(int color) {
		this.mLoadingTipPaint.setColor(color) ;
		if (this.mIsShowLoadingTip && !TextUtils.isEmpty(this.mLoadingTip)) {
			autoInvalidate() ;
		}
	}

	/**
	 * 设置加载时的提示文本大小。
	 *
	 * @param size px
	 */
	public void setLoadingTipSize(float size) {
		this.mLoadingTipPaint.setTextSize(size) ;
		if (this.mIsShowLoadingTip && !TextUtils.isEmpty(this.mLoadingTip)) {
			autoInvalidate() ;
		}
	}
	
	//------------------------------- Lyric Shape -------------------------------//
	
	/**
	 * 设置歌词默认的文本颜色。
	 */
	public void setLyricColor(int color) {
		if (this.mLyricColor == color) {
			return ;
		}
		this.mLyricColor = color ;
		invalidate() ;
	}

	/**
	 * 设置当前播放歌词的高亮颜色。
	 */
	public void setHighlightLyricColor(int color) {
		if (this.mHighlightLyricColor == color) {
			return ;
		}
		this.mHighlightLyricColor = color ;
		invalidate() ;
	}

	/**
	 * 设置被 {@code Indicator} 选中的颜色。
	 */
	public void setSelectedLyricColor(int color) {
		if (this.mSelectedLyricColor == color) {
			return ;
		}
		this.mSelectedLyricColor = color ;
		autoInvalidate() ;
	}
	
	//------------------------------- Operation Of Scroll To Play Lyric -------------------------------//
	
	public void scrollToPlayLyric(){
		privateScrollToPlayLyric(false,0) ;
	}
	
	public void scrollToPlayLyricDelayed(boolean isAnimation,int duration){
		scrollToPlayLyricDelayed(mDelayMillisOfScrollToPlayLyric,isAnimation,duration) ;
	}

	public void scrollToPlayLyricDelayed(int delayMillis, boolean isAnimation, int duration) {
		if(delayMillis < 0){
			privateScrollToPlayLyric(isAnimation,duration) ;
		}else{
			Message msg = mTaskHandler.obtainMessage(MSG_SCROLL_TO_PLAT) ;
			msg.arg1 = isAnimation ? 1 : 0 ;
			msg.arg2 = duration ;
			mTaskHandler.sendMessageDelayed(msg,delayMillis) ;
		}
	}
	
	public void smoothScrollToPlayLyric(int duration){
		privateScrollToPlayLyric(true,duration) ;
	}
	
	private void privateScrollToPlayLyric(boolean isAnimation,int duration){
		if(isValidLyric() && mCurrentPlayLyricIndex >= 0){
			scrollToPosition(mCurrentPlayLyricIndex,getScrollTopOffset(),isAnimation,duration) ;
		}
	}
	
	//------------------------------- Operation Of Align To Selected Lyric -------------------------------//
	
	public void alignToSelectedLyric() {
		privateAlignToSelectedLyric(false,0) ;
	}
	
	public void alignToSelectedLyricDelayed(boolean isAnimation,int duration){
		alignToSelectedLyricDelayed(mDelayMillisOfAlignToSelectedLyric,isAnimation,duration) ;
	}

	public void alignToSelectedLyricDelayed(int delayMillis,boolean isAnimation,int duration){
		if(delayMillis < 0){
			privateAlignToSelectedLyric(isAnimation,duration) ;
		}else{
			Message msg = mTaskHandler.obtainMessage(MSG_ALIGN_TO_SELECTED_LYRIC) ;
			msg.arg1 = isAnimation ? 1 : 0 ;
			msg.arg2 = duration ;
			mTaskHandler.sendMessageDelayed(msg,delayMillis) ;
		}
	}

	public void smoothAlignToSelectedLyric(int duration){
		privateAlignToSelectedLyric(true,duration) ;
	}
	
	private void privateAlignToSelectedLyric(boolean isAnimation,int duration){
		int i = getSelectedLyricIndex() ;
		if(i > 0){
			scrollToPosition(i,getIndicatorViewY(),isAnimation,duration) ;
		}
	}
	
	//------------------------------- Sets Lyric Index To Play -------------------------------//
	
	public void setPlayLyricIndex(int index) {
		if(!isValidLyric()){
			return ;
		}
		final int count = getLyricCount() ; 
		if(index < 0 && index >= count){
			throw new IndexOutOfBoundsException("Index= " + index +",Range=[0," + count + ")") ;
		}
		this.mCurrentPlayLyricIndex = index ;
		autoInvalidate();
	}
	
	public void setPlayLyricIndexByTimeMillis(int timeMillis,int timeOffset){
		if (!isValidLyric()) 
			return ;
		int count = getLyricCount() ;
		timeMillis += timeOffset ;
		int start = 0 ;
		int end = count - 1 ;
		while (start <= end) {
			int mid = start + (end - start) / 2  ;
			LyricLine line = mLyric.get(mid) ;
			int lyricTime = line.getBeginTime() ;
			if (lyricTime > timeMillis) {
				end = mid - 1;
			} else if (lyricTime < timeMillis) {
				start = mid + 1;
			} else {
				start = mid + 1 ;
				break ;
			}
		}
		start -- ;
		this.mCurrentPlayLyricIndex = start ;
		autoInvalidate() ;
	}
	
	/**
	 * 获得当前播放的歌词索引。
	 */
	public int getPlayLyricIndex(){
		return mCurrentPlayLyricIndex ;
	}
	
	//------------------------------- Params Of Scroll -------------------------------//
	
	
	public void setDurationOfScrollToPlayLyric(int duration){
		if(duration < 0){
			duration = 0 ;
		}
		this.mDurationOfScrollToPlayLyric = duration ;
	}
	
	public int getDurationOfScrollToPlayLyric(){
		return mDurationOfScrollToPlayLyric ;
	}
	
	public void setDelayMillisOfScrollToPlayLyric(int delayMillis){
		if(delayMillis < 0){
			delayMillis = 0 ;
		}
		this.mDelayMillisOfScrollToPlayLyric = delayMillis ;
	}
	
	public int getDelayMillisOfScrollToPlayLyric(){
		return mDelayMillisOfScrollToPlayLyric ;
	}
	
	public void setDurationOfAlignToSelectedLyric(int duration){
		if(duration < 0){
			duration = 0 ;
		}
		this.mDurationOfAlignToSelectedLyric = duration ;
	}
	
	public int getDurationOfAlignToSelectedLyric(){
		return mDurationOfAlignToSelectedLyric; 
	}
	
	public void setDelayMillisOfAlignToSelectedLyric(int delayMillis){
		if(delayMillis < 0){
			delayMillis = 0 ;
		}
		this.mDelayMillisOfAlignToSelectedLyric = delayMillis ;
	}
	
	public int getDelayMillisOfAlignToSelectedLyric(){
		return mDelayMillisOfAlignToSelectedLyric ;
	}
	
	public int getSelectedLyricIndex() {	
		if (!isValidLyric()) {
			return -1 ;
		}
		int lyricCount = getLyricCount() ;
		float y = getScrollY() + getIndicatorViewY() ;
		float halfLineSpace = mLineSpace / 2 ;
		float topOffset = getTopOffset() ;
		int start = 0 ;
		int end = lyricCount - 1 ;
		while (start <= end) {
			int mid = start + (end - start) / 2 ;
			LyricLine line = mLyric.get(mid) ;
			float lineY = topOffset + line.position - halfLineSpace ;
			if (lineY > y) {
				end = mid - 1;
			} else if (lineY < y) {
				start = mid + 1;
			} else {
				start = mid + 1 ;
				break ;
			}
		}
		int pos = start - 1 ;
		return pos < 0 ? 0 : pos >= lyricCount ? lyricCount - 1 : pos;
	}

	public LyricLine getSelectedLyricLine() {
		int pos = getSelectedLyricIndex() ;
		if (pos < 0)
			return null; 
		return mLyric.get(pos) ;
	}


	//------------------------------- Auto Scroll Control -------------------------------//
	
	public void setAutoScrollToPlayLyric(boolean isAuto){
		this.mIsAutoScrollToPlayLyric = isAuto ;
	}
	
	public void setAutoAlignToSelectedLyric(boolean isAuto){
		this.mIsAutoAlignToSelectedLyric = isAuto ;
	}
	
	public void setAutoHideIndicator(boolean isAuto){
		this.mIsAutoHideIndicator = isAuto ;
	}

	private class CalculateAllLyricsPositions implements Runnable {
		@Override
		public void run() {
			if (mLyric == null || mLyric.isComputed()) {
				return ;
			}
			measureLyricMaxWidth() ;
			int count = mLyric.count() ;
			int top = 0 ;
			for (int i = 0;i < count;i ++) {
				LyricLine line = mLyric.get(i) ;
				StaticLayout staticLayout = obtainStaticLayout(line) ;
				int lineHeight = staticLayout.getHeight() ;
				line.position = top;
				line.lineHeight = lineHeight ;
				top += lineHeight + mLineSpace ;
			}
			mLyric.markComputed() ;
			requestLayout() ;
			onLyricChanged(mLyric) ;
		}
	}

	public interface OnPlayButtonClickListener {
		public void onPlayBtnClick(int position, LyricLine line) ;
	}
}
