package com.munger.stereocamera.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;import com.munger.stereocamera.R;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public class OrientationWidget extends View
{
	public OrientationWidget(Context context)
	{
		this(context, null, 0);
	}

	public OrientationWidget(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public OrientationWidget(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);

		background = BitmapFactory.decodeResource(context.getResources(), R.drawable.compass);
		arrow = BitmapFactory.decodeResource(context.getResources(), R.drawable.compass_arrow);
		arrow2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.compass_arrow2);
		targetRect = new Rect(0, 0, background.getWidth(), background.getHeight());
		arrowMatrix = new Matrix();
	}

	private Bitmap background;
	private Bitmap arrow;
	private Bitmap arrow2;

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		canvas.drawBitmap(background, null, targetRect, null);
		canvas.drawBitmap(arrow, arrowMatrix, null);
		canvas.drawBitmap(arrow2, arrowMatrix2, null);
	}

	private int width;
	private int height;
	private float rotation = 0.0f;
	private float rotation2 = 0.0f;
	private Rect targetRect;
	private Matrix arrowMatrix;
	private Matrix arrowMatrix2;

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);

		width = w;
		height = h;
		targetRect = new Rect(0, 0, width, height);

		setRotation(rotation);
		setRotation2(rotation2);
	}

	public void setRotation(float angle)
	{
		arrowMatrix = calculateMatrix(arrow, angle);
		rotation = angle;
		invalidate();
	}

	public void setRotation2(float angle)
	{
		arrowMatrix2 = calculateMatrix(arrow2, angle);
		rotation2 = angle;
		invalidate();
	}

	private Matrix calculateMatrix(Bitmap arrow, float angle)
	{
		int aw = arrow.getWidth();
		float awhalf = (float) aw * 0.5f;
		int ah = arrow.getHeight();
		float ahhalf = (float) ah * 0.5f;
		float scalex = (float)width / (float) aw;
		float scaley = (float) height / (float) ah;

		Matrix ret = new Matrix();
		ret.postTranslate(-awhalf, -ahhalf);
		ret.postRotate(angle);
		ret.postTranslate(awhalf, ahhalf);
		ret.postScale(scalex, scaley);

		return ret;
	}
}
