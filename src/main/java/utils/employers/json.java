package utils.employers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class json {

    private static final Gson gson = new Gson();
    public static List<String> parseJsonArray(String jsonData) {
        List<String> result = new ArrayList<>();
        if (jsonData == null || jsonData.isEmpty() || jsonData.equals("[]")) return result;

        try {
            JsonArray array = gson.fromJson(jsonData, JsonArray.class);
            for (JsonElement el : array) {
                if (el.isJsonPrimitive()) {
                    String val = el.getAsString().trim();
                    if (!val.isEmpty()) result.add(val);
                } else if (el.isJsonObject()) {
                    JsonObject obj = el.getAsJsonObject();
                    for (String key : obj.keySet()) {
                        if (obj.get(key).isJsonPrimitive()) {
                            String val = obj.get(key).getAsString().trim();
                            if (!val.isEmpty()) {
                                result.add(val);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            String cleaned = jsonData.replace("[", "").replace("]", "").replace("\"", "").trim();
            if (!cleaned.isEmpty()) {
                for (String part : cleaned.split(",")) {
                    String val = part.trim();
                    if (!val.isEmpty()) result.add(val);
                }
            }
        }
        return result;
    }

    public static List<String> parseJsonArrayField(String jsonData, String fieldName) {
        List<String> result = new ArrayList<>();
        if (jsonData == null || jsonData.isEmpty() || jsonData.equals("[]")) return result;

        try {
            JsonArray array = gson.fromJson(jsonData, JsonArray.class);
            for (JsonElement el : array) {
                if (el.isJsonObject()) {
                    JsonObject obj = el.getAsJsonObject();
                    if (obj.has(fieldName) && obj.get(fieldName).isJsonPrimitive()) {
                        String val = obj.get(fieldName).getAsString().trim();
                        if (!val.isEmpty()) result.add(val);
                    }
                } else if (el.isJsonPrimitive()) {
                    String val = el.getAsString().trim();
                    if (!val.isEmpty()) result.add(val);
                }
            }
        } catch (Exception e) {
            return parseJsonArray(jsonData);
        }
        return result;
    }
    public static String toJson(List<String> list) {
        return gson.toJson(list);
    }
}