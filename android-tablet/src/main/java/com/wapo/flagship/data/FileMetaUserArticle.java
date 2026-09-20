package com.wapo.flagship.data;

import androidx.annotation.NonNull;
import java.util.Locale;

@Deprecated
public class FileMetaUserArticle {
    public static final String TableName = "FileMetaUserArticle";
    public static final String ArticleUrlColumn = "articleUrl";
    public static final String HeadlineColumn = "headline";
    public static final String SummaryColumn = "summary";
    public static final String ByLineColumn = "byLine";
    public static final String ArticleStatusColumn = "articleStatus";
    public static final String ActivityDateColumn = "activityDate";

    public static final String[] Columns = new String[]{
            ArticleUrlColumn,
            HeadlineColumn,
            SummaryColumn,
            ByLineColumn,
            ArticleStatusColumn,
            ActivityDateColumn
    };

    private static final String[] ColumnsTypes = new String[]{
            "TEXT NOT NULL",
            "TEXT NOT NULL",
            "TEXT NOT NULL",
            "TEXT NOT NULL",
            "INTEGER NOT NULL",
            "INTEGER NOT NULL"
    };

    static ITableDescription getTableDescription() {
        return new ITableDescription() {
            @NonNull
            @Override
            public String getTableName() {
                return TableName;
            }

            @Override
            public String[] getColumns() {
                return Columns;
            }

            @Override
            public String[] getColumnsTypes() {
                return ColumnsTypes;
            }

            @Override
            public String[] getKeys() {
                return new String[]{
                        String.format(Locale.US, "PRIMARY KEY (%1$s, %2$s)", ArticleUrlColumn, ArticleStatusColumn)
                };
            }

            @Override
            public String[] getPostCreationSql() {
                return new String[]{
                        String.format(Locale.US, "CREATE INDEX %1$s_%2$s_Index ON %1$s ( %2$s );", TableName, ArticleStatusColumn),
                        String.format(Locale.US, "CREATE INDEX %1$s_%2$s_Index ON %1$s ( %2$s );", TableName, ActivityDateColumn),
                };
            }

            @Override
            public String[] getPreDeletionSql() {
                return new String[]{
                        String.format(Locale.US, "DROP INDEX IF EXISTS %s_%s_Index;", TableName, ArticleStatusColumn),
                        String.format(Locale.US, "DROP INDEX IF EXISTS %s_%s_Index;", TableName, ActivityDateColumn),
                };
            }
        };
    }
}