/*
 * Copyright (c) 2019. The Washington Post. All rights reserved.
 */
package com.wapo.flagship.features.articles.models;

/**
 * Created by Jayesh Elamgodil on 5/31/19.
 */
public class ArticlesRecirculationArticleModelItem {
    public Type type;

    public ArticlesRecirculationArticleModelItem(Type type) {
        this.type = type;
    }

    public static String getSectionName(Type type) {
        switch (type) {
            case MOST_READ: return "Most read";
            default: return "";
        }
    }

    public enum Type {
        MOST_READ,
    }
}
