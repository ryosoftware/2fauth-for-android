package com.twofauth.android.database;

import android.content.ContentValues;
import android.content.Context;

import com.twofauth.android.DatabaseHelper;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DatabaseAtomicOperationsHelper {
    private final DatabaseHelper mDatabaseHelper;

    private final TwoFactorAccountsHelper mTwoFactorAccountOperationsHelper;
    private final TwoFactorGroupsHelper mTwoFactorGroupOperationsHelper;
    private final TwoFactorIconsHelper mTwoFactorIconOperationsHelper;

    public DatabaseAtomicOperationsHelper(@NotNull final DatabaseHelper database_helper, @NotNull final TwoFactorAccountsHelper two_factor_account_operations_helper, @NotNull final TwoFactorGroupsHelper two_factor_group_operations_helper, @NotNull final TwoFactorIconsHelper two_factor_icon_operations_helper) {
        mDatabaseHelper = database_helper;
        mTwoFactorAccountOperationsHelper = two_factor_account_operations_helper;
        mTwoFactorGroupOperationsHelper = two_factor_group_operations_helper;
        mTwoFactorIconOperationsHelper = two_factor_icon_operations_helper;
    }

    public boolean clear(@NotNull final SQLiteDatabase database, @NotNull final Context context) throws Exception {
        mTwoFactorAccountOperationsHelper.delete(database);
        mTwoFactorGroupOperationsHelper.delete(database);
        mTwoFactorIconOperationsHelper.delete(database, context);
        return true;
    }

    public boolean clear(@NotNull final Context context) throws Exception {
        final SQLiteDatabase database = mDatabaseHelper.open(true);
        if (database != null) {
            try {
                if (mDatabaseHelper.beginTransaction(database)) {
                    boolean commit_transaction = false;
                    try {
                        commit_transaction = clear(database, context);
                        return commit_transaction;
                    }
                    finally {
                        mDatabaseHelper.endTransaction(database, commit_transaction);
                    }
                }
            }
            finally {
                mDatabaseHelper.close(database);
            }
        }
        return false;
    }

    public boolean add(@NotNull final SQLiteDatabase database, @NotNull final Context context, @Nullable final List<TwoFactorAccount> accounts, @Nullable final List<TwoFactorGroup> groups) throws Exception {
        if (groups != null) {
            for (final TwoFactorGroup group : groups) {
                group.save(database, context);
            }
        }
        if (accounts != null) {
            for (final TwoFactorAccount account : accounts) {
                account.save(database, context);
            }
        }
        return true;
    }

    public boolean add(@NotNull final Context context, @Nullable final List<TwoFactorAccount> accounts, @Nullable final List<TwoFactorGroup> groups) throws Exception {
        final SQLiteDatabase database = mDatabaseHelper.open(true);
        if (database != null) {
            try {
                if (mDatabaseHelper.beginTransaction(database)) {
                    boolean commit_transaction = false;
                    try {
                        commit_transaction = add(database, context, accounts, groups);
                        return commit_transaction;
                    }
                    finally {
                        mDatabaseHelper.endTransaction(database, commit_transaction);
                    }
                }
            }
            finally {
                mDatabaseHelper.close(database);
            }
        }
        return false;
    }
}
