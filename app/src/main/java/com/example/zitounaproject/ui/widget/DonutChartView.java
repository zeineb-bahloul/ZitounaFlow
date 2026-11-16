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

/** Graphique en anneau (répartition de la qualité). */
public class DonutChartView extends View {

    private final List<Float> valeurs = new ArrayList<>();
    private final List<Integer> couleurs = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public DonutChartView(Context context) {
        this(context, null);
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float epaisseur = dp(18);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(epaisseur);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(epaisseur);
        trackPaint.setColor(ContextCompat.getColor(context, R.color.olive_100));
    }

    /** @param couleurs couleurs ARGB déjà résolues */
    public void setDonnees(List<Float> valeurs, List<Integer> couleurs) {
        this.valeurs.clear();
        this.valeurs.addAll(valeurs);
        this.couleurs.clear();
        this.couleurs.addAll(couleurs);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        float inset = paint.getStrokeWidth() / 2 + dp(2);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        rect.set(cx - size / 2 + inset, cy - size / 2 + inset, cx + size / 2 - inset, cy + size / 2 - inset);
        canvas.drawArc(rect, 0, 360, false, trackPaint);

        float total = 0;
        for (Float v : valeurs) total += v;
        if (total <= 0) return;
        float angle = -90;
        for (int i = 0; i < valeurs.size(); i++) {
            float sweep = valeurs.get(i) / total * 360f;
            if (sweep <= 0) continue;
            paint.setColor(couleurs.get(i));
            canvas.drawArc(rect, angle, Math.max(0.5f, sweep - (valeurs.size() > 1 ? 2 : 0)), false, paint);
            angle += sweep;
        }
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
