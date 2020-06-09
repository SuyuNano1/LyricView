package com.nano.lyricview;

public class LyricLine implements Comparable<LyricLine>{
    
	private String lyric ;
	private int beginTime ;
	private String formatedTime ;
	
	protected float position ;
	protected int lineHeight ;

	public LyricLine() {
		this("",0) ;
	}

	public LyricLine(String lyric, int beginTime) {
		this.lyric = lyric;
		setBeginTime(beginTime) ;
	}
	
	public void appendLyric(String lyric){
		this.lyric += lyric ;
	}

	public void setLyric(String lyric) {
		this.lyric = lyric;
	}

	public String getLyric() {
		return lyric ;
	}

	public void setBeginTime(int beginTime) {
		if (this.beginTime == beginTime && this.formatedTime != null) {
			return ;
		}
		this.beginTime = beginTime;
		this.formatedTime = CompatUtils.timeFormat(beginTime) ;
	}

	public int getBeginTime() {
		return beginTime;
	}
	
	protected final String getFormatedTime(){
		return formatedTime ;
	}

	protected final float getPosition() {
		return position;
	}

	protected final int getLineHeight() {
		return lineHeight;
	}

	@Override
	public int compareTo(LyricLine line) {
		return Long.compare(beginTime, line.getBeginTime());
	}
	
}
