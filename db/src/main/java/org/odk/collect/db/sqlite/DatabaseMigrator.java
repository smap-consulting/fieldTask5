package org.odk.collect.db.sqlite;

import android.database.sqlite.SQLiteDatabase;

public interface DatabaseMigrator {
    void onCreate(SQLiteDatabase db);

    void onUpgrade(SQLiteDatabase db, int oldVersion);

    default void onDowngrade(SQLiteDatabase db, int oldVersion) {}

    default void onOpen(SQLiteDatabase db) {}
}
