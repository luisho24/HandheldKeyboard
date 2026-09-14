package com.liskovsoft.leankeyboard.addons.theme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.drawable.GradientDrawable;

/** Adds a restrained specular rim to the keyboard's own glass surface. */
final class LiquidGlassSurfaceDrawable extends GradientDrawable {
    private final float mCornerRadius;
    private final float mDensity;
    private final Paint mRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix mShaderMatrix = new Matrix();
    private final RectF mBounds = new RectF();
    private SweepGradient mSweepGradient;
    private int mGradientWidth = -1;
    private int mGradientHeight = -1;

    LiquidGlassSurfaceDrawable(Context context, int color, int cornerRadiusDp) {
        mDensity = context.getResources().getDisplayMetrics().density;
        mCornerRadius = cornerRadiusDp * mDensity;
        setColor(color);
        setCornerRadius(mCornerRadius);
        mInnerPaint.setStyle(Paint.Style.STROKE);
        mInnerPaint.setStrokeWidth(Math.max(1f, mDensity));
        mInnerPaint.setColor(Color.argb(35, 255, 255, 255));
        mRimPaint.setStyle(Paint.Style.STROKE);
        mRimPaint.setStrokeWidth(Math.max(1f, mDensity * 1.35f));
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0) return;
        super.draw(canvas);
        mBounds.set(bounds.left + mRimPaint.getStrokeWidth() / 2f,
                bounds.top + mRimPaint.getStrokeWidth() / 2f,
                bounds.right - mRimPaint.getStrokeWidth() / 2f,
                bounds.bottom - mRimPaint.getStrokeWidth() / 2f);
        if (mSweepGradient == null || mGradientWidth != bounds.width() || mGradientHeight != bounds.height()) {
            int[] colors = {
                    Color.argb(18, 255, 255, 255),
                    Color.argb(176, 255, 255, 255),
                    Color.argb(106, 120, 224, 255),
                    Color.argb(28, 255, 255, 255),
                    Color.argb(18, 255, 255, 255)
            };
            float centerX = bounds.exactCenterX();
            float centerY = bounds.exactCenterY();
            mSweepGradient = new SweepGradient(centerX, centerY, colors,
                    new float[]{0f, 0.13f, 0.24f, 0.60f, 1f});
            mGradientWidth = bounds.width();
            mGradientHeight = bounds.height();
        }
        mShaderMatrix.setRotate(78f, bounds.exactCenterX(), bounds.exactCenterY());
        mSweepGradient.setLocalMatrix(mShaderMatrix);
        mRimPaint.setShader(mSweepGradient);
        canvas.drawRoundRect(mBounds, mCornerRadius, mCornerRadius, mRimPaint);
        mRimPaint.setShader(null);

        RectF inner = new RectF(bounds.left + mDensity * 2f, bounds.top + mDensity * 2f,
                bounds.right - mDensity * 2f, bounds.bottom - mDensity * 2f);
        canvas.drawRoundRect(inner, Math.max(0f, mCornerRadius - mDensity * 2f),
                Math.max(0f, mCornerRadius - mDensity * 2f), mInnerPaint);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mSweepGradient = null;
        mGradientWidth = mGradientHeight = -1;
    }
}
