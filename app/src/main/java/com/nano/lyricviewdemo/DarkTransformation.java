package com.nano.lyricviewdemo;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

public class DarkTransformation extends BitmapTransformation {

	private static final String ID = "com.nano.lyricviewdemo.DarkTransformation.$VERSION" ;
	private int d = 35 ;
	public DarkTransformation(Context context){
		super(context) ;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof DarkTransformation ;
	}

	@Override
	public int hashCode() {
		return getId().hashCode() ;
	}
	
	@Override
	public String getId() {
		return ID + ":" + d ;
	}

	@Override
	protected Bitmap transform(BitmapPool bitmapPool, Bitmap toTransform, int outWidth, int outHeight) {
		
		int w = toTransform.getWidth() ;
		int h = toTransform.getHeight() ;

		android.util.Log.d(getClass().getSimpleName(),String.format("w:%d,h:%d",w,h)) ;
		
		Bitmap result = bitmapPool.get(w,h,Bitmap.Config.ARGB_8888) ;
		if(result == null){
			result = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888) ;
		}
		for(int y = 0;y < h;y ++){
			for(int x = 0;x < w;x ++){
				int argb = toTransform.getPixel(x,y) ;
				int a = Color.alpha(argb) ;
				int r = Color.red(argb) - d;
				int g = Color.green(argb) - d;
				int b = Color.blue(argb) - d ;
				result.setPixel(x,y,Color.argb(
				    a,
				    r < 0 ? 0 : r,
					g < 0 ? 0 : g,
					b < 0 ? 0 : b
				)) ;
			}
		}
		return result;
	}

}
