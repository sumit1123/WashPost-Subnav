package com.wapo.flagship.data;

import androidx.annotation.NonNull;

public interface ITableDescription {
    @NonNull
    String getTableName();

    String[] getColumns();

    String[] getColumnsTypes();

    String[] getKeys();

    String[] getPostCreationSql();

    String[] getPreDeletionSql();
}
