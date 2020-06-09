package com.nano.lyricviewdemo;

import android.content.Context;
import android.util.SparseArray;
import com.nano.lyricview.Lyric;
import com.nano.lyricview.LyricLine;
import com.nano.lyricview.LyricView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LyricParser {

	public static Lyric parseKuWoLyric(Context context, int lyricRawId) throws JSONException {
		Lyric lyric = new Lyric() ;
		JSONObject root = new JSONObject(readString(context,lyricRawId)) ;
		JSONObject data = root.getJSONObject("data") ;
		JSONArray lyrList = data.getJSONArray("lrclist") ;
		int count = lyrList.length() ;
		for (int i = 0;i < count;i ++) {
			JSONObject lyrLine = lyrList.getJSONObject(i) ;
			String[] timeStr = lyrLine.getString("time").split("\\.") ;

			lyric.addLine(
				Integer.valueOf(timeStr[0]) * 1000 + Integer.valueOf(timeStr[1].substring(0, 1)) * 100,
				lyrLine.getString("lineLyric")
			);
		}

		return lyric ;
	}

	public static Lyric parseNeteaseLyric(Context context, int lyricRawId) throws JSONException {
		Lyric lyric = new Lyric() ;
		
		if(lyricRawId == -1){
			lyric.setHint("纯音乐，请欣赏") ;
			return lyric ;
		}
		
		JSONObject root = new JSONObject(readString(context,lyricRawId)) ;
		String lyricString = root.getString("lyric") ;
		SparseArray<LyricLine> lineArray = new SparseArray<>(); 

		String[] lines = lyricString.split("\n"); 
		for (String line : lines) {
			int end = getTimeEnd(line) ;
			int time = getTime(line, end) ;
			LyricLine l = new LyricLine() ;
			String lyricText = line.substring(end + 1) ;
			if(lyricText == null || lyricText.length() == 0){
				continue; 
			}
			l.setBeginTime(time) ;
			l.setLyric(lyricText) ;
			lyric.addLine(l) ;
			lineArray.append(time, l) ;
		}

		if (root.has("translateLyric")) {
			String translateLyric = root.getString("translateLyric") ;
			lines = translateLyric.split("\n"); 
			for (String line : lines) {
				if(line == null || line.length() == 0){
					continue ;
				}
				int end = getTimeEnd(line) ;
				int time = getTime(line, end) ;
				LyricLine l = lineArray.get(time) ;
				if (l != null)
					l.appendLyric("\n" + line.substring(end + 1)) ;
			}
		}

		return lyric ;
	}

	public static int getTime(String line, int end) {
		String[] timeStr = line.substring(1, end).split(":") ;
		int time = Integer.valueOf(timeStr[0]) * 60000 ;
		String[] secondStr = timeStr[1].split("\\.") ;
		return time + Integer.valueOf(secondStr[0]) * 1000
		    + Integer.valueOf(secondStr[1]) ;
	}

	public static int getTimeEnd(String line) {

		for (int i = 1;i < line.length();i ++) {
			if (line.charAt(i) == ']') {
				return i ;

			}
		}
		return -1 ;
	}


	public static String readString(Context context,int raw) {
		InputStream is = context.getResources().openRawResource(raw) ;
		BufferedReader br = new BufferedReader(new InputStreamReader(is)) ;

		StringBuilder lyr = new StringBuilder() ;

		try {
			String line = null ;
			while ((line = br.readLine()) != null) {
				lyr.append(line).append("\n") ;
			}
		} catch (IOException e) {
			e.printStackTrace() ;
		} finally {
			try {
				br.close() ;
			} catch (IOException e) {
				e.printStackTrace() ;
			}
		}

		return lyr.toString() ;
	}
}
