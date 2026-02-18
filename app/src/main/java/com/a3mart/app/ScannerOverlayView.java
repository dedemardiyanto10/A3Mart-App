package com.a3mart.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class ScannerOverlayView extends View {

    private final Paint scrimPaint = new Paint();
    private final Paint reticlePaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint scanLinePaint = new Paint();

    private RectF reticle;
    private String hintText = "";

    private float scanY;
    private float scanSpeed;

    public ScannerOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        scrimPaint.setColor(0x99000000);
        scrimPaint.setStyle(Paint.Style.FILL);

        reticlePaint.setColor(0xFFFFFFFF);
        reticlePaint.setStrokeWidth(6f);
        reticlePaint.setStyle(Paint.Style.STROKE);
        reticlePaint.setAntiAlias(true);
        reticlePaint.setStrokeCap(Paint.Cap.ROUND);
        reticlePaint.setStrokeJoin(Paint.Join.ROUND);

        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(42f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        scanLinePaint.setColor(0xFF00FF00);
        scanLinePaint.setStrokeWidth(4f);
        scanLinePaint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float size = Math.min(w, h) * 0.6f;
        float left = (w - size) / 2f;
        float top = (h - size) / 2f;
        reticle = new RectF(left, top, left + size, top + size);

        scanY = reticle.top;
        scanSpeed = size / 120f; 
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (reticle == null) return;

        canvas.drawRect(0, 0, getWidth(), reticle.top, scrimPaint);
        canvas.drawRect(0, reticle.top, reticle.left, reticle.bottom, scrimPaint);
        canvas.drawRect(reticle.right, reticle.top, getWidth(), reticle.bottom, scrimPaint);
        canvas.drawRect(0, reticle.bottom, getWidth(), getHeight(), scrimPaint);

        canvas.drawLine(reticle.left + 10, scanY, reticle.right - 10, scanY, scanLinePaint);

        scanY += scanSpeed;
        if (scanY > reticle.bottom) {
            scanY = reticle.top;
        }

        float corner = reticle.width() * 0.08f;

        canvas.drawLine(
                reticle.left, reticle.top, reticle.left + corner, reticle.top, reticlePaint);
        canvas.drawLine(
                reticle.left, reticle.top, reticle.left, reticle.top + corner, reticlePaint);

        canvas.drawLine(
                reticle.right - corner, reticle.top, reticle.right, reticle.top, reticlePaint);
        canvas.drawLine(
                reticle.right, reticle.top, reticle.right, reticle.top + corner, reticlePaint);

        canvas.drawLine(
                reticle.left, reticle.bottom - corner, reticle.left, reticle.bottom, reticlePaint);
        canvas.drawLine(
                reticle.left, reticle.bottom, reticle.left + corner, reticle.bottom, reticlePaint);

        canvas.drawLine(
                reticle.right - corner,
                reticle.bottom,
                reticle.right,
                reticle.bottom,
                reticlePaint);
        canvas.drawLine(
                reticle.right,
                reticle.bottom,
                reticle.right,
                reticle.bottom - corner,
                reticlePaint);

        if (!hintText.isEmpty()) {
            canvas.drawText(hintText, getWidth() / 2f, reticle.bottom + 70, textPaint);
        }

        postInvalidateOnAnimation();
    }

    public void setHint(String text) {
        hintText = text;
        invalidate();
    }

    public RectF getReticle() {
        return reticle;
    }
}
