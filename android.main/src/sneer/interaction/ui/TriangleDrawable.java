package sneer.interaction.ui;

import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;

public abstract class TriangleDrawable extends Drawable {

	protected Path path;
	protected Paint paint;

	public TriangleDrawable(int color) {
		path = new Path();
		paint = new Paint();
		paint.setColor(color);
		paint.setStyle(Paint.Style.FILL);
	}

	@Override
	public void setAlpha(int alpha) {
	
	}
	
	@Override
	public void setColorFilter(ColorFilter cf) {
	
	}
	
	@Override
	public int getOpacity() {
		return 255;
	}
	
	protected int width() {
		return getBounds().width();
	}
	
	protected int height() {
		return getBounds().height();
	}
	
} 