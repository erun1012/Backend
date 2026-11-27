package com.goner.backend.functions;

import com.goner.backend.AuthUtil;
import com.goner.backend.FirebaseConfig;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class SaveProfileFunction implements HttpFunction{
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z0-9]{4,16}$");
    private static final int NICKNAME_MAX = 10;
    private static final long CODE_LOCK_DAYS=180;

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        response.setContentType("application/json; charset=utf-8");

        if (!"POST".equals(request.getMethod())) {
            error(response, 405, "method_not_allowed");
            return;
        }

        String uid = AuthUtil.verifyUid(request);
        if (uid == null) {
            error(response, 401, "unauthorized");
            return;
        }

        JsonObject body = parseBody(request);
        if (body == null) {
            error(response, 400, "invalid_body");
            return;
        }
        String nickname = getString(body, "nickname");
        String code = getString(body, "friendCode");

        if (nickname.isEmpty() || nickname.length() > NICKNAME_MAX) {
            error(response, 400, "invalid_nickname");
            return;
        }
        if (!CODE_PATTERN.matcher(code).matches()) {
            error(response, 400, "invalid_code");
            return;
        }

        FirebaseConfig.init();
        Firestore db = FirestoreClient.getFirestore();

        String result = db.runTransaction(tx -> {
            DocumentReference codeRef = db.collection("friendCodes").document(code);
            DocumentReference userRef = db.collection("users").document(uid);

            DocumentSnapshot codeDoc = tx.get(codeRef).get();
            DocumentSnapshot userDoc = tx.get(userRef).get();

            if (codeDoc.exists() && !uid.equals(codeDoc.getString("uid"))) {
                return "code_taken";
            }

            String oldCode = userDoc.getString("friendCode");
            boolean changing = oldCode != null && !oldCode.equals(code);
            boolean onboarded = Boolean.TRUE.equals(userDoc.getBoolean("onboardingCompleted"));
            boolean limited = changing && onboarded;   // 6개월 제한 대상

            if (limited) {
                Timestamp changedAt = userDoc.getTimestamp("friendCodeChangedAt");
                if (changedAt != null) {
                    Instant editableAt = Instant.ofEpochSecond(changedAt.getSeconds())
                            .plus(Duration.ofDays(CODE_LOCK_DAYS));
                    if (editableAt.isAfter(Instant.now())) {
                        return "code_change_locked";
                    }
                }
            }

            if (changing) {
                tx.delete(db.collection("friendCodes").document(oldCode));
            }

            Map<String, Object> data = new HashMap<>();
            data.put("nickname", nickname);
            data.put("friendCode", code);
            if (limited) {
                data.put("friendCodeChangedAt", Timestamp.now());
            }

            tx.set(codeRef, Map.of("uid", uid));
            tx.set(userRef, data, SetOptions.merge());
            return "ok";
        }).get();

        if (result.equals("code_taken")) {
            error(response, 409, "code_taken");
            return;
        }
        if (result.equals("code_change_locked")) {
            error(response, 403, "code_change_locked");
            return;
        }
        response.getWriter().write("{\"ok\":true}");
    }

    private static JsonObject parseBody(HttpRequest request) {
        try {
            return JsonParser.parseReader(request.getReader()).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException | IOException e) {
            return null;
        }
    }

    private static String getString(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        return (e == null || !e.isJsonPrimitive()) ? "" : e.getAsString().trim();
    }

    private static void error(HttpResponse response, int status, String message) throws IOException {
        response.setStatusCode(status);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
