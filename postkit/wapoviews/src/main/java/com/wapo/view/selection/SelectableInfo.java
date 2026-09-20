package com.wapo.view.selection;

class SelectableInfo {

    private int start;
    private int end;
    private CharSequence selectedText;
    private CharSequence text;
    private String key;
    private Selectable selectable;

    SelectableInfo(Selectable selectable) {
        this.start = 0;
        this.end = 0;
        this.selectedText = "";
        this.selectable = selectable;
        this.text = selectable.getText();
        this.key = selectable.getKey();
    }

    void removeSelectable() {
        this.selectable.selectText(0, 0);
        this.selectable = null;
    }

    void selectText(int start, int end) {
        if (text == null || text.length() == 0) {
            text = selectable != null ? selectable.getText() : "";
        }
        if (start < 0 || start > text.length()) {
            start = 0;
        }
        if (end < 0 || end > text.length()) {
            end = text.length();
        }
        if (start > end) {
            start = 0;
            end = 0;
        }
        this.start = start;
        this.end = end;
        if (selectable != null) {
            selectable.selectText(start, end);
            selectedText = selectable.getSelectedText();
        } else {
            selectedText = text.toString().substring(start, end);
        }
    }

    void resetSelection() {
        selectText(0, 0);
    }


    public Selectable getSelectable() {
        return selectable;
    }

    public void setSelectable(Selectable selectable) {
        this.selectable = selectable;
        selectText(start, end);
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public CharSequence getSelectedText() {
        return selectedText;
    }


    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public void setSelectedText(CharSequence selectedText) {
        this.selectedText = selectedText;
    }

    public CharSequence getText() {
        if ((text == null || text.length() == 0) && selectable != null && selectable.getText() != null) {
            text = selectable.getText();
        }
        return text;
    }

    public void setText(CharSequence text) {
        this.text = text;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
