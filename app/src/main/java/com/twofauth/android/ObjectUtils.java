package com.twofauth.android;

import org.jetbrains.annotations.Nullable;

public class ObjectUtils {
    public static boolean equals(@Nullable final Object object1, @Nullable final Object object2) {
        return (object1 == null) ? (object2 == null) : object1.equals(object2);
    }
}
