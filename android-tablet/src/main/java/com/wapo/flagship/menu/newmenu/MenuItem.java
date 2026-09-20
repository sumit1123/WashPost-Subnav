package com.wapo.flagship.menu.newmenu;

import com.wapo.flagship.json.MenuSection;

import java.util.List;

public class MenuItem {
    public final static int TYPE_HEADER = 0;
    public final static int TYPE_ITEM = 1;
    public final static int TYPE_MORE = 2;
    public final static int TYPE_ITEM_LAST = 3;
    public final static int TYPE_CATEGORY_HEADER = 4;
    public final static int TYPE_LESS = 5;


    private String id;
    private String name;
    private String bundleName;
    private int type;
    private String sectionType;
    private List<MenuItem> moreItems;

    public MenuItem(String id, String name, String bundleName, int type, String sectionType) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.bundleName = bundleName;
        this.sectionType = sectionType;
    }

    public MenuItem(MenuSection menuSection, int type) {
        id = menuSection.getDatabaseId();
        name = menuSection.getDisplayName();
        bundleName = menuSection.getBundleName() + ".json";
        this.type = type;
        sectionType = menuSection.getType();

    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getBundleName() {
        return bundleName;
    }

    public String getSectionType() {
        return sectionType;
    }

    public List<MenuItem> getMoreItems() {
        return moreItems;
    }

    public void setMoreItems(List<MenuItem> moreItems) {
        this.moreItems = moreItems;
    }

}
