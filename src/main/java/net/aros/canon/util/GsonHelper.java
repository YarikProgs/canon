package net.aros.canon.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class GsonHelper {
    private static final Gson GSON = new Gson();

    public static JsonElement parse(String json) {
        return GSON.fromJson(json, JsonElement.class);
    }

    public static String toString(JsonElement json) {
        return GSON.toJson(json);
    }
}
