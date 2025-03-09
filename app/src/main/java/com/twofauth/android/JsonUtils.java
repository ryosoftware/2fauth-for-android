package com.twofauth.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JsonUtils {
    public static Map<Integer, JSONObject> StringToJsonMap(@Nullable final String data, @NotNull final String id_key) throws Exception {
        final Map<Integer, JSONObject> map = new HashMap<Integer, JSONObject>();
        if (data != null) {
            final JSONArray array = new JSONArray(data);
            for (int i = 0; i < array.length(); i ++) {
                final JSONObject object = array.getJSONObject(i);
                map.put(object.getInt(id_key), object);
            }
        }
        return map;
    }

    private static JsonObject toJsonObject(@NotNull final JSONObject json_object) throws Exception {
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
    public static String JSonObjectsToString(@NotNull final Collection<JSONObject> objects) throws Exception {
        final List<JsonObject> standardized_objects = new ArrayList<JsonObject>();
        for (JSONObject object : objects) {
            standardized_objects.add(toJsonObject(object));
        }
        return new Gson().toJson(standardized_objects);
    }
}
