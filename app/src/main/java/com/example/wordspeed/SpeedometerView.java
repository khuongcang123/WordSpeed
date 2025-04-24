package com.example.wordspeed;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

public class SpeedometerView extends View {

    private static final float MAX_SPEED = 240f;
    private static final float NEEDLE_BASE_WIDTH_DP = 8f;
    private static final float CENTER_DOT_RADIUS_DP = 9f;

    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needleShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needleHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float speed = 0f;
    private float totalDistance = 0f;

    private boolean isLandscape = false;
    private float centerX;
    private float centerY;
    private float radius;
    private float needleBaseWidth;
    private Typeface digitalTypeface;
    private ValueAnimator distanceAnimator;
    private int animatedDistance = 0;


    public SpeedometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        digitalTypeface = ResourcesCompat.getFont(getContext(), R.font.digital_font);
        textPaint.setTypeface(digitalTypeface);
        initializePaints();
    }

    private void initializePaints() {
        configureBorderPaint();
        configureNeedlePaints();
        configureTickPaint();
        configureTextPaint();
        configureCenterDotPaint();
    }

    private void configureBorderPaint() {
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(dpToPx(8));
        borderShadowPaint.setColor(Color.argb(100, 0, 0, 0));
    }

    private void configureNeedlePaints() {
        needlePaint.setStyle(Paint.Style.FILL);
        needleShadowPaint.setColor(Color.argb(100, 0, 0, 0));
        needleShadowPaint.setShadowLayer(dpToPx(4), dpToPx(2), dpToPx(2), Color.argb(100, 0, 0, 0));
        needleHighlightPaint.setColor(Color.argb(150, 255, 255, 255));
    }

    private void configureTickPaint() {
        tickPaint.setColor(ContextCompat.getColor(getContext(), R.color.tick_color));
        tickPaint.setStrokeWidth(dpToPx(2));
    }

    private void configureTextPaint() {
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_color));
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void configureCenterDotPaint() {
        centerDotPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateDimensions(w, h);
        updateGradientShaders();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
        drawBorder(canvas);
        drawTicks(canvas);
        drawNeedle(canvas);
        drawCenterDot(canvas);
        drawTextInfo(canvas);
    }

    private void drawBackground(@NonNull Canvas canvas) {
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new RadialGradient(
                centerX, centerY, radius,
                new int[]{
                        ContextCompat.getColor(getContext(), R.color.background_center),
                        ContextCompat.getColor(getContext(), R.color.background_edge)
                },
                new float[]{0.6f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(centerX, centerY, radius, bgPaint);
    }

    private void drawBorder(@NonNull Canvas canvas) {
        canvas.drawCircle(centerX, centerY, radius + dpToPx(4), borderShadowPaint);
        canvas.drawCircle(centerX, centerY, radius, circlePaint);
    }

    private void drawTicks(@NonNull Canvas canvas) {
        float tickLength = isLandscape ? radius * 0.1f : radius * 0.15f;
        for (int i = 0; i <= 240; i += 10) {
            double angle = Math.toRadians(135 + (270.0 * i / 240));
            drawSingleTick(canvas, tickLength, angle, i);
        }
    }

    private void drawSingleTick(@NonNull Canvas canvas, float tickLength, double angle, int value) {
        float startX = (float) (centerX + radius * Math.cos(angle));
        float startY = (float) (centerY + radius * Math.sin(angle));
        float stopX = (float) (centerX + (radius - tickLength) * Math.cos(angle));
        float stopY = (float) (centerY + (radius - tickLength) * Math.sin(angle));
        canvas.drawLine(startX, startY, stopX, stopY, tickPaint);
        if (value % 30 == 0) drawTickLabel(canvas, angle, value);
    }

    private void drawTickLabel(@NonNull Canvas canvas, double angle, int value) {
        float labelRadius = radius - (isLandscape ? dpToPx(40) : dpToPx(60));
        float labelX = (float) (centerX + labelRadius * Math.cos(angle));
        float labelY = (float) (centerY + labelRadius * Math.sin(angle)) + dpToPx(10);
        textPaint.setTextSize(isLandscape ? dpToPx(14) : dpToPx(16));
        canvas.drawText(String.valueOf(value), labelX, labelY, textPaint);
    }

    private void drawNeedle(@NonNull Canvas canvas) {
        double needleAngle = Math.toRadians(135 + (270.0 * speed / MAX_SPEED));
        float needleLength = radius * (isLandscape ? 0.8f : 0.85f);
        drawNeedleShadow(canvas, needleAngle, needleLength);
        drawNeedleBody(canvas, needleAngle, needleLength);
        drawNeedleHighlight(canvas, needleAngle, needleLength);
    }

    private void drawNeedleShadow(@NonNull Canvas canvas, double angle, float length) {
        canvas.save();
        canvas.translate(dpToPx(2), dpToPx(2));
        canvas.drawPath(createNeedlePath(angle, length), needleShadowPaint);
        canvas.restore();
    }

    private void drawNeedleBody(@NonNull Canvas canvas, double angle, float length) {
        canvas.drawPath(createNeedlePath(angle, length), needlePaint);
    }

    private void drawNeedleHighlight(@NonNull Canvas canvas, double angle, float length) {
        canvas.drawPath(createHighlightPath(angle, length), needleHighlightPaint);
    }

    private void drawCenterDot(@NonNull Canvas canvas) {
        canvas.drawCircle(centerX + dpToPx(1), centerY + dpToPx(1), dpToPx(10), borderShadowPaint);
        canvas.drawCircle(centerX, centerY, dpToPx(9), centerDotPaint);
        canvas.drawCircle(centerX - dpToPx(3), centerY - dpToPx(3), dpToPx(2), needleHighlightPaint);
    }

    private void drawTextInfo(@NonNull Canvas canvas) {
        textPaint.setTextSize(dpToPx(48));
        canvas.drawText(String.format("%.0f", speed), centerX, centerY + radius * 0.5f, textPaint);

        textPaint.setTextSize(dpToPx(20));
        canvas.drawText("km/h", centerX, centerY + radius * 0.5f + dpToPx(24), textPaint);

        textPaint.setTextSize(dpToPx(30));
        int displayDistance = (int) (totalDistance * 10);
        String distanceStr = String.format("%06d", displayDistance);

        float totalWidth = textPaint.measureText(distanceStr);
        float startX = centerX - (totalWidth / 2);
        float y = centerY - radius * 0.2f;

        int originalColor = textPaint.getColor();

        for (int i = 0; i < distanceStr.length(); i++) {
            char c = distanceStr.charAt(i);
            if (i == distanceStr.length() - 1) {
                textPaint.setColor(Color.WHITE);
            } else {
                textPaint.setColor(originalColor);
            }
            canvas.drawText(String.valueOf(c), startX, y, textPaint);
            startX += textPaint.measureText(String.valueOf(c));
        }

        textPaint.setColor(originalColor);
    }

    private android.graphics.Path createNeedlePath(double angle, float length) {
        float tipX = (float) (centerX + length * Math.cos(angle));
        float tipY = (float) (centerY + length * Math.sin(angle));
        float baseAngle = (float) (angle + Math.PI / 2);
        float baseX1 = (float) (centerX + needleBaseWidth * Math.cos(baseAngle));
        float baseY1 = (float) (centerY + needleBaseWidth * Math.sin(baseAngle));
        float baseX2 = (float) (centerX - needleBaseWidth * Math.cos(baseAngle));
        float baseY2 = (float) (centerY - needleBaseWidth * Math.sin(baseAngle));
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(tipX, tipY);
        path.lineTo(baseX1, baseY1);
        path.lineTo(baseX2, baseY2);
        path.close();
        return path;
    }

    private android.graphics.Path createHighlightPath(double angle, float length) {
        float highlightLength = length * 0.9f;
        float tipX = (float) (centerX + highlightLength * Math.cos(angle));
        float tipY = (float) (centerY + highlightLength * Math.sin(angle));
        float baseAngle = (float) (angle + Math.PI / 2);
        float baseX1 = (float) (centerX + needleBaseWidth * Math.cos(baseAngle));
        float baseY1 = (float) (centerY + needleBaseWidth * Math.sin(baseAngle));
        float baseX2 = (float) (centerX - needleBaseWidth * Math.cos(baseAngle));
        float baseY2 = (float) (centerY - needleBaseWidth * Math.sin(baseAngle));
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(tipX, tipY);
        path.lineTo((baseX1 + baseX2) / 2, (baseY1 + baseY2) / 2);
        path.lineTo(baseX2, baseY2);
        return path;
    }

    private void updateDimensions(int width, int height) {
        isLandscape = width > height;
        centerX = width / 2f;
        centerY = height / 2f;
        radius = Math.min(width, height) * 0.45f;
        needleBaseWidth = dpToPx(NEEDLE_BASE_WIDTH_DP);
    }

    private void updateGradientShaders() {
        circlePaint.setShader(new LinearGradient(0, 0, 0, getHeight(),
                new int[]{Color.LTGRAY, Color.DKGRAY},
                new float[]{0.2f, 0.8f}, Shader.TileMode.CLAMP));

        needlePaint.setShader(new LinearGradient(0, 0, 0, getHeight(),
                new int[]{
                        ContextCompat.getColor(getContext(), R.color.needle_highlight),
                        ContextCompat.getColor(getContext(), R.color.needle_main),
                        ContextCompat.getColor(getContext(), R.color.needle_shadow)
                }, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));

        centerDotPaint.setShader(new RadialGradient(centerX, centerY, dpToPx(CENTER_DOT_RADIUS_DP),
                new int[]{Color.WHITE, Color.GRAY},
                new float[]{0.8f, 1.0f}, Shader.TileMode.CLAMP));
    }

    private float dpToPx(float dp) {
        return dp * Resources.getSystem().getDisplayMetrics().density;
    }

    public void setSpeed(float speed) {
        this.speed = Math.min(speed, MAX_SPEED);
        invalidate();
    }

    public void setDistance(float distance) {
        this.totalDistance = distance;
        invalidate();
    }

    public float getSpeed() {
        return speed;
    }

    public void reset() {
        speed = 0;
        totalDistance = 0;
        invalidate();
    }
}
