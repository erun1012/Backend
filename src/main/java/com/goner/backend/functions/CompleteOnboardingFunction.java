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
import static com.goner.backend.ApiUtil.error;
import static com.goner.backend.ApiUtil.parseBody;

import java.util.Map;

public class CompleteOnboardingFunction implements HttpFunction {

    @Override
    public void service(HttpRequest request, HttpResponse response)throws Exception {
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
        // 값이 없거나 true/false가 아니면 off로 처리 (기본값 off)
        JsonElement e = body.get("notificationEnabled");
        boolean notificationEnabled = e != null && e.isJsonPrimitive()
                && e.getAsJsonPrimitive().isBoolean() && e.getAsBoolean();

        FirebaseConfig.init();
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference userRef = db.collection("users").document(uid);

        // 프로필(친구 코드)을 저장한 유저만 온보딩 완료 가능
        DocumentSnapshot user = userRef.get().get();
        if (!user.exists() || user.getString("friendCode") == null) {
            error(response, 400, "profile_required");
            return;
        }

        userRef.set(Map.of(
                "onboardingCompleted", true,
                "notificationEnabled", notificationEnabled
        ), SetOptions.merge()).get();

        response.getWriter().write("{\"ok\":true}");
    }



}
