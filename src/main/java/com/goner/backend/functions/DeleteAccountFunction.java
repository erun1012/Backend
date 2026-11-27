package com.goner.backend.functions;

import com.goner.backend.AuthUtil;
import com.goner.backend.FirebaseConfig;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.cloud.FirestoreClient;

import java.io.IOException;

public class DeleteAccountFunction implements HttpFunction {

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

        FirebaseConfig.init();
        Firestore db = FirestoreClient.getFirestore();

        // 내 친구코드 문서도 같이 지우기 위해 코드를 먼저 읽어둠
        DocumentSnapshot user = db.collection("users").document(uid).get().get();
        String code = user.exists() ? user.getString("friendCode") : null;

        WriteBatch batch = db.batch();
        if (code != null) {
            batch.delete(db.collection("friendCodes").document(code));
        }
        batch.delete(db.collection("users").document(uid));
        batch.commit().get();

        // 로그인 계정(Auth) 삭제. 에뮬레이터에는 실제 구글 계정이 없어서 건너뜀
        boolean emulator = System.getenv("FIREBASE_AUTH_EMULATOR_HOST") != null;
        if (!emulator) {
            try {
                FirebaseAuth.getInstance().deleteUser(uid);
            } catch (FirebaseAuthException e) {
                error(response, 500, "auth_delete_failed");
                return;
            }
        }

        response.getWriter().write("{\"ok\":true}");
    }

    private static void error(HttpResponse response, int status, String message) throws IOException {
        response.setStatusCode(status);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}