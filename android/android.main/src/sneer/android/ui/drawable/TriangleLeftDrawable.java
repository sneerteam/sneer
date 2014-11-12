package sneer.android.ui.drawable;

import android.graphics.Canvas;

public class TriangleLeftDrawable extends TriangleDrawable {

	public TriangleLeftDrawable(int color) {
		super(color);
	}

	@Override
	public void draw(Canvas canvas) {
		path.moveTo(0, 0);
		path.lineTo(width(), 0);
		path.lineTo(width(), height());
		path.lineTo(0, 0);
		path.close();

		canvas.drawPath(path, paint);
	}

}