package com.goner.backend.functions;

import com.goner.backend.AuthUtil;
import com.goner.backend.FirebaseConfig;
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
import java.util.Map;
import java.util.regex.Pattern;

public class SaveProfileFunction implements HttpFunction{
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z0-9]{4,16}$");
    private static final int NICKNAME_MAX = 10;

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

        Boolean saved = db.runTransaction(tx -> {
            DocumentReference codeRef = db.collection("friendCodes").document(code);
            DocumentReference userRef = db.collection("users").document(uid);

            DocumentSnapshot codeDoc = tx.get(codeRef).get();
            DocumentSnapshot userDoc = tx.get(userRef).get();

            if (codeDoc.exists() && !uid.equals(codeDoc.getString("uid"))) {
                return false;
            }

            String oldCode = userDoc.getString("friendCode");
            if (oldCode != null && !oldCode.equals(code)) {
                tx.delete(db.collection("friendCodes").document(oldCode));
            }

            tx.set(codeRef, Map.of("uid", uid));
            tx.set(userRef, Map.of("nickname", nickname, "friendCode", code), SetOptions.merge());
            return true;
        }).get();

        if (!saved) {
            error(response, 409, "code_taken");
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
