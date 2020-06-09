package com.nano.lyricview;

import java.util.ArrayList;
import java.util.Collections;

public class Lyric {
	
	private ArrayList<LyricLine> mLines ;	
	private String hint ;
	private boolean isComputed ;

	public Lyric() {
		mLines = new ArrayList<>() ;
	}
	
	public Lyric addLine(LyricLine line) {
		mLines.add(line) ;
		return this ;
	}

	public Lyric addLine(int startTime, String lyric) {
		addLine(new LyricLine(lyric, startTime)) ;
		return this ;
	}
	
	/**
	 * 当前歌词为空时，歌词控件会显示提示。
	 */
	public void setHint(String hint){
		this.hint = hint ;
	}
	
	public String getHint(){
		return hint ;
	}
	
	public LyricLine get(int position) {
		return mLines.get(position) ;
	}

	public int count() {
		return mLines.size() ;
	}
	
	public LyricLine getLast(){
		return mLines.get(mLines.size() - 1) ;
	}

	protected final void sort() {
		Collections.sort(mLines) ;
	}
	
	protected final void markComputed(){
		this.isComputed = true ;
	}
	
	protected final boolean isComputed(){
		return this.isComputed ;
	}
}
