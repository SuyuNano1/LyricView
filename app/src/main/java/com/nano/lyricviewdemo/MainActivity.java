package com.nano.lyricviewdemo;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.nano.lyricview.CompatUtils;
import com.nano.lyricview.Lyric;
import com.nano.lyricview.LyricLine;
import com.nano.lyricview.LyricView;
import com.nano.lyricview.PlayButtonDrawable;
import jp.wasabeef.glide.transformations.BlurTransformation;
import org.json.JSONException;

import static com.nano.lyricview.CompatUtils.dp2px;
import static com.nano.lyricview.CompatUtils.sp2px; 


public class MainActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener,LyricView.OnPlayButtonClickListener {
    
	private static final int REFRESH = 1 ;
	
	private static final int[][] SONGS = {
		{R.raw.song_snzmlx,R.raw.lyr_snzmlx,1,R.mipmap.album_snzmlx},
		{R.raw.song_feng,R.raw.lyr_feng,0,R.mipmap.album_feng},
		{R.raw.song_iloveyouop,R.raw.lyr_iloveyouop,1,R.mipmap.album_iloveyouop},
		{R.raw.song_kanong,-1,1,R.mipmap.album_kanong}
	} ;
	
	private int currentIndex = 0 ;
	
	private MediaPlayer mediaPlayer = new MediaPlayer() ;
	private LyricView mLyricView ;
	private ImageView mImageBackground ;
	private SeekBar mSeekBar ;
	private TextView mTvCurrentDuration ;
	private TextView mTvDuration ;
	
	private boolean isSeekbarUserTouching ;
	private boolean isUpdateProgress = true;
	private Handler progress = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if(msg.what == REFRESH){
				if(mediaPlayer.isPlaying()){
					changeProgress(mediaPlayer.getCurrentPosition(),false) ;
					if(isUpdateProgress && !hasMessages(REFRESH)){
						
					    progress.sendEmptyMessageDelayed(REFRESH,1000) ;
					}
				}
			}
		}
	} ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
			
		mImageBackground = findViewById(R.id.image_bg) ;
		mLyricView = findViewById(R.id.lyric) ;
		mSeekBar = findViewById(R.id.seekbar) ;
		mTvDuration = findViewById(R.id.tv_duration) ;
		mTvCurrentDuration = findViewById(R.id.tv_current_duration) ;
		
		// changeAttrs() ;
		changePlayButtonColors() ;
		
		mediaPlayer.setOnCompletionListener(this) ;
		mLyricView.setOnPlayButtonClickListener(this) ;
		setCurrentSong(0) ;
		
		mLyricView.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Toast.makeText(v.getContext(),"Click",0).show() ;
			}
		}) ;
		
		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			@Override
			public void onProgressChanged(SeekBar seekbar, int progress, boolean isUser) {
				mTvCurrentDuration.setText(CompatUtils.timeFormat(progress)) ; 
				
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekbar) {
				isSeekbarUserTouching = true ;
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekbar) {
				mLyricView.setIndicatorShow(false) ;
				isSeekbarUserTouching = false ;
				changeProgress(seekbar.getProgress(),true) ;
			}
		}) ;
	}

	@Override
	protected void onPause() {
		super.onPause();
		isUpdateProgress = false ;
		progress.removeMessages(REFRESH) ;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(mediaPlayer.isPlaying()){
			isUpdateProgress = true ;
		    progress.sendEmptyMessage(REFRESH) ;
		}
	}
	
	@Override
	public void onPlayBtnClick(int position, LyricLine line) {
		mLyricView.setIndicatorShow(false) ;
		changeProgress(line.getBeginTime(),true) ;
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		isUpdateProgress = false ;
		progress.removeMessages(REFRESH) ;
	}
	
	public void next(View v){
		currentIndex = ++ currentIndex % SONGS.length ;
		setCurrentSong(currentIndex) ;
	}
	
	public void pause(View v){
		if(mediaPlayer.isPlaying()){
			mediaPlayer.pause() ;
			isUpdateProgress = false ;
			progress.removeMessages(REFRESH) ;
			mLyricView.setAutoScrollToLyric(false) ;
		}
	}
	
	public void play(View v){
		mediaPlayer.start() ;
		isUpdateProgress = true ;
		progress.sendEmptyMessage(REFRESH) ;
		mLyricView.setAutoScrollToLyric(true) ;
	}
	
	private void setCurrentSong(final int index){
		new Thread(new Runnable(){
			@Override public void run() {
				try {
					final int[] song = SONGS[index] ;
					Lyric lyric ;
					mLyricView.setLoadingTipShow(true) ;
					if (song[2] == 0) {
						lyric = LyricParser.parseKuWoLyric(MainActivity.this, song[1]) ;
					} else {
						lyric = LyricParser.parseNeteaseLyric(MainActivity.this, song[1]) ;
					}
					mLyricView.loadLyric(lyric) ;
					mLyricView.setLoadingTipShow(false) ;
					play(song[0]) ;
					switchBackground() ;
				} catch (JSONException e) {
					e.printStackTrace() ;
				}
			}	
		}).start() ;
	}
    
	private void play(@RawRes int rawId){
		try {
			mediaPlayer.reset() ;
			mediaPlayer.setDataSource(getResources().openRawResourceFd(rawId)) ;
			mediaPlayer.prepare() ;
			mediaPlayer.start() ;
			
			progress.removeMessages(REFRESH) ;
			isUpdateProgress = true ;
			progress.sendEmptyMessage(REFRESH) ;
			mSeekBar.post(new Runnable(){
				@Override
				public void run() {
					mSeekBar.setMax(mediaPlayer.getDuration()) ;
					mTvDuration.setText(CompatUtils.timeFormat(mediaPlayer.getDuration())) ;
				}
			});
		} catch (Exception e) {
			e.printStackTrace() ;
			return ;
		}
	}
	
	private void changeProgress(int time,boolean changeMediaPlayer){
		if(changeMediaPlayer) mediaPlayer.seekTo(time);
		
		if(!isSeekbarUserTouching) {
			mTvCurrentDuration.setText(CompatUtils.timeFormat(time)) ;
			mSeekBar.setProgress(time) ;
		}
		mLyricView.setCurrentPlayingLyricByTime(time,0) ;
	}
	
	
	private void changePlayButtonColors(){
		Drawable playButtonDrawable = mLyricView.getPlayButtonDrawable() ;
		if(playButtonDrawable instanceof PlayButtonDrawable){
			PlayButtonDrawable pbd = (PlayButtonDrawable) playButtonDrawable ;
			pbd.setColor(
			    new ColorStateList(
				    new int[][]{
						{android.R.attr.state_pressed},
						{}
					},
					new int[]{
						0x00ffffff | (100 << 24),
						0x00ffffff | (180 << 24)
					}
				)
			) ;
			pbd.setStrokeWidth(6) ;
		}
	}
	
	private void switchBackground(){
		mImageBackground.post(new Runnable(){
			@Override
			public void run() {
				int res = SONGS[currentIndex][3] ;
				if(res == -1) return ;
				Glide.clear(mImageBackground) ;
				Glide.with(MainActivity.this)
				    .load(res)
					.fitCenter()
					.bitmapTransform(
					new BlurTransformation(MainActivity.this,25,10))
					.into(mImageBackground);
			}
		}) ;
	}
	
	
	private void changeAttrs(){
		
		mLyricView.setScrollDuration(2000) ;
		mLyricView.setScrollToSelectedLyricDelayMillis(900) ;
		mLyricView.setHideIndicatorDelayMillis(3000) ;
		
		
		int lr = randomMiddleSize() ;
		mLyricView.setTimeTextLeft(lr) ;
		mLyricView.setTimeTextColor(randomColor()) ;
		mLyricView.setTimeTextSize(randomSmallTextSize()) ;
		
		mLyricView.setLoadingTipColor(randomColor()) ;
		mLyricView.setLoadingTipSize(randomLargeTextSize()) ;
		mLyricView.setLoadingTip("正在加载歌词中") ;
		
		mLyricView.setHintTextSize(randomLargeTextSize()) ;
		mLyricView.setHintTextColor(randomColor()) ;
		
		mLyricView.setLyricSelectedTextColor(randomColor()) ;
		mLyricView.setLyricHighlightTextColor(randomColor()) ;
		mLyricView.setLyricTextColor(randomColor()) ;
		
		mLyricView.setIndicatorHeight(3) ;
		GradientDrawable gd = new GradientDrawable() ;
		gd.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT) ;
		gd.setGradientType(GradientDrawable.LINEAR_GRADIENT) ;
		gd.setColors(new int[]{randomColor(),randomColor(),randomColor()}) ;
		mLyricView.setIndicatorDrawable(gd); 
		
		int w ;
		mLyricView.setPlayButtonSize(
		    w = randomMiddleSize(),
			w + 2
		) ;
		mLyricView.setPlayButtonRight(lr) ;
		
		changePlayButtonColors() ;
	}

	private int randomColor() {
		return 255 << 24 |
		       (int)(256 * Math.random()) << 16 |
			   (int)(256 * Math.random()) <<  8 |
			   (int)(256 * Math.random()) ;
	}
	
	private int randomSmallTextSize(){
		return (int)sp2px(this,(int)(10 + Math.random() * 5)) ;
	}
	
	private int randomMiddleTextSize(){
		return (int)sp2px(this,(int)(15 + Math.random() * 5)) ;
	}
	
	private int randomLargeTextSize(){
		return (int)sp2px(this,(int)(20 + Math.random() * 5)) ;
	}

	private int randomMiddleSize(){
		return (int)dp2px(this,(int)(10 + Math.random() * 5)) ;
	}

	private int randomLargeSize(){
		return (int)dp2px(this,(int)(20 + Math.random() * 5)) ;
	}

}
