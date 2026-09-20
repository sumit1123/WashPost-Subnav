package com.wapo.flagship.menu.newmenu;

import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

public class MenuDividerDecoration extends RecyclerView.ItemDecoration {

    private Paint paint;

    public MenuDividerDecoration(int dividerColor) {
        paint = new Paint();
        paint.setColor(dividerColor);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View childAt = parent.getChildAt(i);
            int type = parent.getChildViewHolder(childAt).getItemViewType();
            if (MenuItem.TYPE_HEADER == type || MenuItem.TYPE_ITEM == type || MenuItem.TYPE_CATEGORY_HEADER == type) continue;
            float bottom = childAt.getBottom() + childAt.getTranslationY();
            c.drawRect(0, bottom, c.getWidth(), bottom + 1, paint);
        }
    }
}
