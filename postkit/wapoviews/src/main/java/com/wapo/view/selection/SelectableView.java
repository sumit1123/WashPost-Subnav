package com.wapo.view.selection;

public interface SelectableView {
    void copyTextToClipboard();
    String getSelectedText();
    boolean isSelectionActive();
    void resetSelection();
}
