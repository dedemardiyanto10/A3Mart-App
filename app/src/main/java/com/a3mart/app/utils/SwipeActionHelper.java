package com.a3mart.app.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.a3mart.app.R;

public abstract class SwipeActionHelper extends ItemTouchHelper.SimpleCallback {
    private final Paint p = new Paint();

    public SwipeActionHelper() {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
    }

    @Override
    public boolean onMove(
            @NonNull RecyclerView rv,
            @NonNull RecyclerView.ViewHolder vh,
            @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onChildDraw(
            @NonNull Canvas c,
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder,
            float dX,
            float dY,
            int actionState,
            boolean isCurrentlyActive) {

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            View itemView = viewHolder.itemView;
            float radius = 42f;
            float marginSamping = 0f;

            if (isCurrentlyActive) {
                recyclerView.getParent().requestDisallowInterceptTouchEvent(true);
            }

            p.reset();
            p.setAntiAlias(true);

            if (dX > 0) {
                p.setColor(Color.parseColor("#4CAF50"));

                float rightBound =
                        Math.min(itemView.getLeft() + dX, itemView.getRight() - marginSamping);

                RectF background =
                        new RectF(
                                (float) itemView.getLeft() + marginSamping,
                                (float) itemView.getTop(),
                                rightBound,
                                (float) itemView.getBottom());

                c.drawRoundRect(background, radius, radius, p);

                if (dX > 50) {
                    drawIconAndText(
                            c, recyclerView, itemView, dX, "LUNAS", R.drawable.ic_check, true);
                }

            } else if (dX < 0) {
                p.setColor(Color.parseColor("#F44336"));

                float leftBound =
                        Math.max(itemView.getRight() + dX, itemView.getLeft() + marginSamping);

                RectF background =
                        new RectF(
                                leftBound,
                                (float) itemView.getTop(),
                                (float) itemView.getRight() - marginSamping,
                                (float) itemView.getBottom());

                c.drawRoundRect(background, radius, radius, p);

                if (dX < -50) {
                    drawIconAndText(
                            c, recyclerView, itemView, dX, "HAPUS", R.drawable.ic_delete, false);
                }
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void drawIconAndText(
            Canvas c,
            RecyclerView rv,
            View itemView,
            float dX,
            String text,
            int iconRes,
            boolean isRight) {
        float itemHeight = (float) itemView.getBottom() - (float) itemView.getTop();
        int margin = (int) (itemHeight * 0.2);
        int iconSize = (int) (itemHeight * 0.4);
        int top = itemView.getTop() + (int) ((itemHeight - iconSize) / 2);

        Drawable rawIcon = ContextCompat.getDrawable(rv.getContext(), iconRes);
        if (rawIcon != null) {
            Drawable icon = DrawableCompat.wrap(rawIcon).mutate();
            DrawableCompat.setTint(icon, Color.WHITE);

            if (isRight) {
                icon.setBounds(
                        itemView.getLeft() + margin,
                        top,
                        itemView.getLeft() + margin + iconSize,
                        top + iconSize);
            } else {
                icon.setBounds(
                        itemView.getRight() - margin - iconSize,
                        top,
                        itemView.getRight() - margin,
                        top + iconSize);
            }
            icon.draw(c);
        }

        p.setColor(Color.WHITE);
        p.setTextSize(36f);
        p.setFakeBoldText(true);

        if (isRight) {
            p.setTextAlign(Paint.Align.LEFT);
            c.drawText(
                    text, itemView.getLeft() + margin + iconSize + 20, top + (iconSize * 0.75f), p);
        } else {
            p.setTextAlign(Paint.Align.RIGHT);
            c.drawText(
                    text,
                    itemView.getRight() - margin - iconSize - 20,
                    top + (iconSize * 0.75f),
                    p);
        }
    }
}
