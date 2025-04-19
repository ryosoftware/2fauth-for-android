package com.twofauth.android.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class JSON {
    public static @Nullable List<JSONObject> toListOfJSONObjects(@Nullable final String data) throws JSONException {
        List<JSONObject> list = new ArrayList<JSONObject>();
        if (data != null) {
            final JSONArray array = new JSONArray(data);
            for (int i = 0; i < array.length(); i ++) {
                list.add(array.getJSONObject(i));
            }
        }
        return list;
    }

    public static @Nullable JSONObject toJSONObject(@Nullable final String data) throws JSONException {
        return (data == null) ? null : new JSONObject(data);
    }

    private static @NotNull JsonObject toJsonObject(@NotNull final JSONObject json_object) throws JSONException {
        final JsonObject new_json_object = new JsonObject();
        final Iterator<String> iterator = json_object.keys();
        while (iterator.hasNext()) {
            final String name = iterator.next();
            if (json_object.isNull(name)) {
                new_json_object.add(name, null);
            }
            else {
                final Object object = json_object.get(name);
                if (object instanceof Number) {
                    new_json_object.addProperty(name, (Number) object);
                }
                else if (object instanceof String) {
                    new_json_object.addProperty(name, (String) object);
                }
                else if (object instanceof Boolean) {
                    new_json_object.addProperty(name, (Boolean) object);
                }
                else if (object instanceof Character) {
                    new_json_object.addProperty(name, (Character) object);
                }
            }
        }
        return new_json_object;
    }

    private static @NotNull Gson getGSonBuilder() {
        return (new GsonBuilder()).serializeNulls().create();
    }

    public static @NotNull String toString(@NotNull final JSONObject object) throws JSONException {
        return getGSonBuilder().toJson(toJsonObject(object));
    }

    public static @NotNull String toString(@NotNull final Collection<JSONObject> objects) throws JSONException {
        final List<JsonObject> standardized_objects = new ArrayList<JsonObject>();
        for (JSONObject object : objects) { standardized_objects.add(toJsonObject(object)); }
        return getGSonBuilder().toJson(standardized_objects);
    }
}
