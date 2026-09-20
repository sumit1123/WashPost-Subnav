package com.wapo.flagship.menu.newmenu;

public interface SectionClickListener {
    void onSectionClick(String bundleName, String sectionName, String sectionType, String id, int position);
    void onSectionLongClick(String bundleName, String sectionName, String sectionType, boolean isChecked);
}
