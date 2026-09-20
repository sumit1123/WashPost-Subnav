package com.wapo.view.selection;

public interface SelectionCallback {
    void startSelection(SelectableView selectableView);
    void stopSelection(SelectableView selectableView);
}
