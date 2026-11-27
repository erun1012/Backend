package com.goner.backend.functions;

import com.goner.backend.AuthUtil;
import com.goner.backend.FirebaseConfig;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.JsonObject;
import static com.goner.backend.ApiUtil.error;
import static com.goner.backend.ApiUtil.getString;
import static com.goner.backend.ApiUtil.parseBody;
import java.io.IOException;

public class GetMeFunction implements HttpFunction {

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        response.setContentType("application/json; charset=utf-8");

        if (!"GET".equals(request.getMethod())) {
            error(response, 405, "method_not_allowed");
            return;
        }

        String uid = AuthUtil.verifyUid(request);
        if (uid == null) {
            error(response, 401, "unauthorized");
            return;
        }

        FirebaseConfig.init();
        DocumentSnapshot user = FirestoreClient.getFirestore()
                .collection("users").document(uid).get().get();

        // 문서가 없거나 필드가 없으면 온보딩 전으로 처리
        boolean onboardingCompleted = user.exists()
                && Boolean.TRUE.equals(user.getBoolean("onboardingCompleted"));
        boolean notificationEnabled = user.exists()
                && Boolean.TRUE.equals(user.getBoolean("notificationEnabled"));

        JsonObject result = new JsonObject();
        result.addProperty("onboardingCompleted", onboardingCompleted);
        result.addProperty("notificationEnabled", notificationEnabled);
        result.addProperty("nickname", user.exists() ? user.getString("nickname") : null);
        result.addProperty("friendCode", user.exists() ? user.getString("friendCode") : null);

        response.getWriter().write(result.toString());
    }

    private static void error(HttpResponse response, int status, String message) throws IOException {
        response.setStatusCode(status);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}