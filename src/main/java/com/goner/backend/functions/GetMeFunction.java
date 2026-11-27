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
import com.google.cloud.Timestamp;
import java.time.Instant;
import java.time.Duration;

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

        boolean friendCodeLocked=false;
        String friendCodeEditableAt = null;
        Timestamp changedAt = user.exists() ? user.getTimestamp("friendCodeChangedAt") : null;
        if(!onboardingCompleted && changedAt !=null) {
            Instant editableAt = Instant.ofEpochSecond(changedAt.getSeconds()).plus(Duration.ofDays(SaveProfileFunction.CODE_LOCK_DAYS));
            if(editableAt.isAfter(Instant.now())) {
                friendCodeLocked=true;
                friendCodeEditableAt = editableAt.toString();
            }
        }
        result.addProperty("friendCodeLocked",friendCodeLocked);
        result.addProperty("friendCodeEditableAt",friendCodeEditableAt);

        response.getWriter().write(result.toString());
    }

}