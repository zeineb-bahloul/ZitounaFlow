package com.example.zitounaproject.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.zitounaproject.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Histogramme simple dessiné sur un Canvas (aucune bibliothèque externe). */
public class BarChartView extends View {

    private final List<String> labels = new ArrayList<>();
    private final List<Double> valeurs = new ArrayList<>();
    private String unite = "";

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint maxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public BarChartView(Context context) {
        this(context, null);
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        barPaint.setColor(ContextCompat.getColor(context, R.color.olive_500));
        maxPaint.setColor(ContextCompat.getColor(context, R.color.gold));
        trackPaint.setColor(ContextCompat.getColor(context, R.color.olive_100));
        labelPaint.setColor(ContextCompat.getColor(context, R.color.muted));
        labelPaint.setTextSize(sp(10));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setColor(ContextCompat.getColor(context, R.color.olive_900));
        valuePaint.setTextSize(sp(11));
        valuePaint.setFakeBoldText(true);
        valuePaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setDonnees(List<String> labels, List<Double> valeurs, String unite) {
        this.labels.clear();
        this.labels.addAll(labels);
        this.valeurs.clear();
        this.valeurs.addAll(valeurs);
        this.unite = unite;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = resolveSize((int) dp(180), heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = valeurs.size();
        if (n == 0) return;

        float labelZone = dp(18);
        float valueZone = dp(18);
        float left = getPaddingLeft();
        float right = getWidth() - getPaddingRight();
        float top = getPaddingTop() + valueZone;
        float bottom = getHeight() - getPaddingBottom() - labelZone;
        float slot = (right - left) / n;
        float barWidth = Math.max(dp(4), Math.min(dp(28), slot * 0.6f));
        float radius = Math.min(barWidth / 2, dp(6));

        double max = 0;
        int indexMax = -1;
        for (int i = 0; i < n; i++) {
            if (valeurs.get(i) > max) {
                max = valeurs.get(i);
                indexMax = i;
            }
        }
        // N'afficher qu'une étiquette sur k pour rester lisible
        int pas = Math.max(1, (int) Math.ceil(n / 8.0));

        for (int i = 0; i < n; i++) {
            float cx = left + slot * i + slot / 2;
            rect.set(cx - barWidth / 2, top, cx + barWidth / 2, bottom);
            canvas.drawRoundRect(rect, radius, radius, trackPaint);

            double v = valeurs.get(i);
            if (max > 0 && v > 0) {
                float h = (float) (v / max) * (bottom - top);
                rect.set(cx - barWidth / 2, bottom - h, cx + barWidth / 2, bottom);
                canvas.drawRoundRect(rect, radius, radius, i == indexMax ? maxPaint : barPaint);
            }
            if (i % pas == 0 || i == n - 1) {
                canvas.drawText(labels.get(i), cx, getHeight() - getPaddingBottom() - dp(4), labelPaint);
            }
        }
        if (indexMax >= 0 && max > 0) {
            float cx = left + slot * indexMax + slot / 2;
            String texte = String.format(Locale.FRENCH, "%,.0f %s", max, unite).trim();
            canvas.drawText(texte, Math.max(left + dp(24), Math.min(right - dp(24), cx)), top - dp(5), valuePaint);
        }
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
    }
}
