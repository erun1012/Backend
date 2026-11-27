package com.goner.backend;

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;

public class ApiUtil {
    private ApiUtil() {
    }

    // 에러
    public static void error(HttpResponse response, int status, String message) throws IOException {
        response.setStatusCode(status);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    // 요청 본문을 JSON으로 읽기
    public static JsonObject parseBody(HttpRequest request) {
        try {
            return JsonParser.parseReader(request.getReader()).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException | IOException e) {
            return null;
        }
    }

    // 문자열 값 꺼내기, 공백 제거
    public static String getString(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        return (e == null || !e.isJsonPrimitive()) ? "" : e.getAsString().trim();
    }
}