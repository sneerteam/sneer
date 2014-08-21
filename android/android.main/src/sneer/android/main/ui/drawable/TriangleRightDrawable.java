package sneer.android.main.ui.drawable;

import android.graphics.*;

public class TriangleRightDrawable extends TriangleDrawable {

	public TriangleRightDrawable(int color) {
		super(color);
	}

	@Override
	public void draw(Canvas canvas) {
		path.moveTo(0, 0);
		path.lineTo(width(), height());
		path.lineTo(0, height());
		path.lineTo(0, 0);
		path.close();
		
		canvas.drawPath(path, paint);
	}

} 