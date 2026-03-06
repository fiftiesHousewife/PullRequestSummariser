package org.fifties.housewife;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

final class JsonFields {

    private JsonFields() {
    }

    static String str(final JsonObject object, final String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    static int num(final JsonObject object, final String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return 0;
        }
        return object.get(key).getAsInt();
    }

    static boolean bool(final JsonObject object, final String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return false;
        }
        return object.get(key).getAsBoolean();
    }

    static JsonArray arr(final JsonObject object, final String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return new JsonArray();
        }
        return object.getAsJsonArray(key);
    }
}
