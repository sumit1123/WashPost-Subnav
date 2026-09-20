package com.wapo.flagship.json;


import java.util.List;

public class MenuSection {
    public static final String BLOG_TYPE = "blog";
    public static final String SECTION_TYPE = "section";
    public static final String COMICS_TYPE = "comics";
    public static final String LABEL_TYPE = "label";
    public static final String WEB_TYPE = "web";
    public static final String SECTION_TYPE_FUSION = "fusion";
    public static final String SECTION_TYPE_AUTHOR = "author";

    private final String title;
    private final String displayName;
    private final String type;
    private final String databaseId;
    private final String bundleName;
    private final MenuSection[] sectionInfo;
    private final int childrenBeforeFold;
    private boolean isUnlisted;

    private final List<String> aliases;
    public MenuSection(String title, String displayName, String type, String databaseId, String bundleName, MenuSection[] sectionInfo, int childrenBeforeFold, List<String> aliases, boolean isUnlisted) {
        this.title = title;
        this.displayName = displayName;
        this.type = type;
        this.databaseId = databaseId;
        this.bundleName = bundleName;
        this.sectionInfo = sectionInfo;
        this.childrenBeforeFold = childrenBeforeFold;
        this.aliases = aliases;
        this.isUnlisted = isUnlisted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuSection that = (MenuSection) o;
        return databaseId != null ? databaseId.equals(that.databaseId) : bundleName != null && bundleName.equals(that.bundleName);
    }
    public String getTitle() {
        return title;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getType() {
        return type;
    }

    public String getDatabaseId() {
        return databaseId;
    }

    public String getBundleName() {
        return bundleName;
    }

    public MenuSection[] getSectionInfo() {
        return sectionInfo;
    }
    
    public boolean isBlog() {
        return BLOG_TYPE.equals(type);
    }

    public boolean isSection() {
        return SECTION_TYPE.equals(type) || SECTION_TYPE_FUSION.equals(type) || COMICS_TYPE.equals(type);
    }

    public int getChildrenBeforeFold() { return childrenBeforeFold; }

    public void setUnlisted(boolean unlisted) {
        isUnlisted = unlisted;
    }

    public boolean isUnlisted() {
        return isUnlisted;
    }

    public List<String> getAliases() {
        return aliases;
    }
}
