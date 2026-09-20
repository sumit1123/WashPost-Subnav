package com.wapo.flagship.features.grid;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wapo.flagship.features.grid.model.CardSegmentType;
import com.wapo.flagship.features.grid.model.DividerLayout;
import com.wapo.flagship.features.grid.model.Dividers;
import com.wapo.flagship.features.grid.model.Grid;
import com.wapo.flagship.features.grid.model.Item;
import com.wapo.flagship.features.grid.model.ScreenSizeLayout;
import com.wapo.flagship.features.grid.model.Separator;
import com.wapo.flagship.features.grid.model.Table;
import com.wapo.flagship.features.newsprint.NewsprintHelper;

import java.util.List;

public class BorderDecorator extends RecyclerView.ItemDecoration {

    private Paint separatorPaint = new Paint();
    private Paint normalDividerPaint = new Paint();
    private Paint boldDividerPaint = new Paint();
    private Grid grid = null;

    public BorderDecorator() {
    }

    public Paint getSeparatorPaint() {
        return separatorPaint;
    }

    public void setSeparatorPaint(Paint paint) {
        this.separatorPaint = paint;
    }

    public Paint getNormalDividerPaint() {
        return normalDividerPaint;
    }

    public void setNormalDividerPaint(Paint paint) {
        this.normalDividerPaint = paint;
    }

    public Paint getBoldDividerPaint() {
        return boldDividerPaint;
    }

    public void setBoldDividerPaint(Paint paint) {
        this.boldDividerPaint = paint;
    }
    public void setGrid(Grid grid) { this.grid = grid; }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(c, parent, state);
        WPGridView wpGridView = (WPGridView) parent;
        int gutterHeight = wpGridView.getGutterHeight();
        int gutterWidth = wpGridView.getGutterWidth();
        int sideMargin = wpGridView.getSideMargin();
        ScreenSizeLayout screenSizeLayout = wpGridView.getScreenSizeLayout();
        boolean isPageCardified = screenSizeLayout == ScreenSizeLayout.XSMALL && grid != null && grid.getCards() != null && grid.getCards().getExtraSmall() != null;
        for (int i = 0; i < wpGridView.getChildCount(); i++) {
            View view = wpGridView.getChildAt(i);
            SpannableGridLayoutManager.LayoutParams layoutParams = (SpannableGridLayoutManager.LayoutParams) view.getLayoutParams();
            Item item = layoutParams.getItem();
            if (item == null) return;
            Table table = layoutParams.getTable();
            if (table == null) return;
            Dividers tableDividers = table.getDividers(screenSizeLayout);
            Dividers chainDividers = layoutParams.getChain().getDividers(screenSizeLayout);
            boolean hasChainSeparatorLine = item instanceof Separator && ((Separator) item).getLine() ? true : false;
            if (tableDividers == null && chainDividers == null && !hasChainSeparatorLine) continue;
            List<DividerLayout> tableVertical = tableDividers == null ? null : tableDividers.getVertical();
            List<DividerLayout> tableHorizontal = tableDividers == null ? null : tableDividers.getHorizontal();
            List<DividerLayout> chainVertical = chainDividers == null ? null : chainDividers.getVertical();
            List<DividerLayout> chainHorizontal = chainDividers == null ? null : chainDividers.getHorizontal();

            int itemColumnStart = item.getResolvedColumn();
            int tableColumnStart = table.getResolvedColumn();
            int itemRowStart = item.getResolvedRow();
            int tableRowStart = table.getResolvedRow();

            DividerLayout verticalBorderOfTable = getVerticalBorder(itemColumnStart, itemRowStart, tableVertical);
            DividerLayout verticalBorderOfChain = getVerticalBorder(tableColumnStart, tableRowStart, chainVertical);
            if (verticalBorderOfTable != null || verticalBorderOfChain != null) {
                float x = view.getLeft() - gutterWidth / 2f;
                int rowHeight = findRowHeight(layoutParams, wpGridView);
                DividerLayout currentDivider = verticalBorderOfTable == null ? verticalBorderOfChain : verticalBorderOfTable;
                boolean isFirst = verticalBorderOfTable != null && currentDivider.getRow() == item.getResolvedRow() || verticalBorderOfChain != null && item.getResolvedRow() == 0;
                boolean isLast = verticalBorderOfTable != null && currentDivider.getRow() + currentDivider.getRowSpan() == item.getResolvedRow() + item.getResolvedRowSpan()
                        || verticalBorderOfChain != null && item.getResolvedRow() == table.getItems().size() - 1;
                int top = isFirst ? view.getTop() : view.getTop() - gutterHeight/2;
                int bottom = isLast ? rowHeight : rowHeight + gutterHeight/2;
                Paint paint = currentDivider.getStyle() == DividerStyle.BOLD ? boldDividerPaint : normalDividerPaint;
                c.drawLine(x, top, x, bottom, paint);
            }

            DividerLayout horizontalBorderOfTable = getHorizontalBorderInTable(itemRowStart, item, tableHorizontal);
            DividerLayout horizontalBorderOfChain = getHorizontalBorderInChain(itemRowStart, table, chainHorizontal);
            boolean isCardifiedDivider = item.getCardSegmentType() == CardSegmentType.MIDDLE_CARD || item.getCardSegmentType() == CardSegmentType.BOTTOM_CARD;

            boolean hasHorizontalBorder = horizontalBorderOfTable != null || horizontalBorderOfChain != null;
            boolean isCardifyExclusion = isPageCardified && (item.getCardSegmentType() == CardSegmentType.TOP_CARD || item.getCardSegmentType() == CardSegmentType.FULL_CARD || item.getCardSegmentType() == CardSegmentType.NO_CARD);
            if (hasHorizontalBorder && !isCardifyExclusion) {
                int top = isCardifiedDivider ? view.getTop() : view.getTop() - gutterHeight / 2;
                DividerLayout currentDivider = horizontalBorderOfTable == null ? horizontalBorderOfChain : horizontalBorderOfTable;
                boolean isFirst = currentDivider.getColumn() == itemColumnStart;
                boolean isLast = currentDivider.getColumn() + currentDivider.getSpan() == itemColumnStart + item.getResolvedColumnSpan();
                int left = isFirst ? item.getForceFullBleed() ? view.getLeft() + sideMargin : view.getLeft() : view.getLeft() - gutterWidth/2;
                int right = isLast ? item.getForceFullBleed() ? view.getRight() - sideMargin : view.getRight() : view.getRight() + gutterWidth/2;
                int dividerPadding = isCardifiedDivider ? wpGridView.getCardDividerPadding() : 0;
                Paint paint = currentDivider.getStyle() == DividerStyle.BOLD ? boldDividerPaint : normalDividerPaint;
                if (item.isNewsprint()) {
                    paint = NewsprintHelper.INSTANCE.getDividerPaint(paint, item);
                }
                c.drawLine(left + dividerPadding, top, right - dividerPadding, top, paint);
            }

            if (hasChainSeparatorLine && !isPageCardified) {
                float top = view.getTop() + view.getMeasuredHeight()/2;
                c.drawLine(view.getLeft(), top, view.getRight(), top, separatorPaint);
            }
        }
    }

    private int findRowHeight(SpannableGridLayoutManager.LayoutParams itemLayoutParams, WPGridView wpGridView) {
        int max = 0;
        int itemRow = itemLayoutParams.getItem().getResolvedRow();
        int itemRowSpan = itemLayoutParams.getItem().getResolvedRowSpan();
        Table table = itemLayoutParams.getTable();
        for (int i = 0; i < wpGridView.getChildCount(); i++) {
            View view = wpGridView.getChildAt(i);
            SpannableGridLayoutManager.LayoutParams layoutParams = (SpannableGridLayoutManager.LayoutParams) view.getLayoutParams();
            if (layoutParams.getItem() != null && table.equals(layoutParams.getTable()) && (layoutParams.getItem().getResolvedRow() == itemRow && layoutParams.getItem().getResolvedRowSpan() <= itemRowSpan
                    || (layoutParams.getItem().getResolvedRow() + layoutParams.getItem().getResolvedRowSpan() <= itemRow + itemRowSpan))) {
                max = Math.max(max, view.getBottom());
            }
        }
        return max;
    }
    
    private DividerLayout getHorizontalBorderInTable(int rowStart, Item item, List<DividerLayout> tableDividers) {
        if (tableDividers != null) {
            for (DividerLayout divider : tableDividers) {
                if (divider.getRow() == rowStart && item.getResolvedColumn() >= divider.getColumn() && item.getResolvedColumn() < divider.getColumn() + divider.getSpan()) {
                    return divider;
                }
            }
        }
        return null;
    }

    private DividerLayout getHorizontalBorderInChain(int itemRowStart, Table table, List<DividerLayout> chainDividers) {

        if(chainDividers != null) {
            for (DividerLayout divider : chainDividers) {
                if (divider.getRow() == table.getResolvedRow() && itemRowStart == 0 && table.getResolvedColumn() >= divider.getColumn()  && table.getResolvedColumn() < divider.getColumn() + divider.getSpan()) {
                    return divider;
                }
            }
        }
        return null;
    }

    private DividerLayout getVerticalBorder(int columnStart, int itemRowStart, List<DividerLayout> dividerLayouts) {
        if (dividerLayouts != null) {
            for (DividerLayout divider : dividerLayouts) {
                if (divider.getColumn() == columnStart && itemRowStart >= divider.getRow() && itemRowStart < divider.getRow() + divider.getRowSpan()) {
                    return divider;
                }
            }
        }
        return null;
    }
}