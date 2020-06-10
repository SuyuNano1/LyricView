package com.nano.lyricviewdemo;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.bumptech.glide.Glide;
import com.nano.lyricview.CompatUtils;
import com.nano.lyricview.Lyric;
import com.nano.lyricview.LyricLine;
import com.nano.lyricview.LyricView;
import com.nano.lyricview.PlayButtonDrawable;
import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;
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
	private ImageView mImageBackground ;
	private SeekBar mSeekBar ;
	private TextView mTvCurrentDuration ;
	private TextView mTvDuration ;
	private ViewPager mViewPager ;
	
	private MyPagerAdapter mPagerAdapter ;
	
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
		mSeekBar = findViewById(R.id.seekbar) ;
		mTvDuration = findViewById(R.id.tv_duration) ;
		mTvCurrentDuration = findViewById(R.id.tv_current_duration) ;
		mViewPager = findViewById(R.id.viewpager) ;
		
		
		mediaPlayer.setOnCompletionListener(this) ;
		
		mPagerAdapter = new MyPagerAdapter(); 
		mViewPager.setAdapter(mPagerAdapter) ;
		
		setCurrentSong(0) ;
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
				mPagerAdapter.mLyricView.setIndicatorShow(false) ;
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
		mPagerAdapter.mLyricView.setIndicatorShow(false) ;
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
			mPagerAdapter.mLyricView.setAutoScrollToLyric(false) ;
		}
	}
	
	public void play(View v){
		mediaPlayer.start() ;
		isUpdateProgress = true ;
		progress.sendEmptyMessage(REFRESH) ;
		mPagerAdapter.mLyricView.setAutoScrollToLyric(true) ;
	}
	
	private void setCurrentSong(final int index){
		new Thread(new Runnable(){
			@Override public void run() {
				final int[] song = SONGS[index] ;
				mPagerAdapter.loadLyric() ;
				mPagerAdapter.loadAlbum() ;
				play(song[0]) ;
				switchBackground() ;
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
		mPagerAdapter.mLyricView.setCurrentPlayingLyricByTime(time,0) ;
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
	
	private class MyPagerAdapter extends PagerAdapter {

		private ImageView mAlbumView ;
		private LyricView mLyricView ;
		
		private View mAlbumLayout ;
		private View mLyricLyout ;
		
		public MyPagerAdapter(){
			mAlbumLayout = LayoutInflater.from(MainActivity.this).inflate(R.layout.album,null,false) ;
			mAlbumView = mAlbumLayout.findViewById(R.id.album) ;
			
			mLyricLyout = LayoutInflater.from(MainActivity.this).inflate(R.layout.lyric,null,false) ;
			mLyricView = mLyricLyout.findViewById(R.id.lyric) ;
			mLyricView.setOnPlayButtonClickListener(MainActivity.this) ;
			mLyricView.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					Toast.makeText(v.getContext(),"Click",0).show() ;
				}
			}) ;
			
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
		
		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public boolean isViewFromObject(View view, Object obj) {
			return view == obj;
		}

		@Override
		@NonNull
		public Object instantiateItem(ViewGroup container, int position) {
			switch(position){
				case 0 :
					container.addView(mAlbumLayout) ; 
					return mAlbumLayout ;
				case 1 :
					container.addView(mLyricLyout) ; 
					return mLyricLyout ;
			}
			throw new IndexOutOfBoundsException(); 
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View)object) ;
		}
		
		private void loadAlbum(){
			mAlbumView.post(new Runnable(){
				@Override
				public void run() {
					Glide.with(MainActivity.this)
						.load(SONGS[currentIndex][3])
						.centerCrop()
						.bitmapTransform(new RoundedCornersTransformation(
							MainActivity.this,45,0						
						))
						.into(mAlbumView) ;
				}
			}) ;
		}
		
		private void loadLyric(){
			try {
				int[] song = SONGS[currentIndex] ;
				Lyric lyric ;
				mLyricView.setLoadingTipShow(true) ;
				if (song[2] == 0) {
					lyric = LyricParser.parseKuWoLyric(MainActivity.this, song[1]) ;
				} else {
					lyric = LyricParser.parseNeteaseLyric(MainActivity.this, song[1]) ;
				}
				mLyricView.loadLyric(lyric) ;
				mLyricView.setLoadingTipShow(false) ;
			} catch (JSONException e) {
				e.printStackTrace() ;
			}
		}
	}
}
