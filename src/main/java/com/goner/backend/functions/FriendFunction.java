package com.goner.backend.functions;

import com.goner.backend.AuthUtil;
import com.goner.backend.FirebaseConfig;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.JsonObject;

import static com.goner.backend.ApiUtil.error;

public class FriendFunction implements HttpFunction {
    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        response.setContentType("application/json; charset=utf-8");

        String uid = AuthUtil.verifyUid(request);
        if(uid==null) {
            error(response,401,"unauthorized");
            return;
        }

        FirebaseConfig.init();
        Firestore db = FirestoreClient.getFirestore();
        String path = request.getPath();
        String method = request.getMethod();

        if(path.equals("/search")&&method.equals("GET")) {
            search(request,response, db, uid);
        } else {
            error(response,404,"not_found");
        }
    }

    private void search(HttpRequest request, HttpResponse response, Firestore db, String uid) throws Exception {
        String code = request.getFirstQueryParameter("code").orElse("");
        if (!code.matches("^[a-z0-9]{4,16}$")) {
            error(response, 400, "invalid_code");
            return;
        }
        DocumentSnapshot codeDoc = db.collection("friendCodes").document(code).get().get();
        if (!codeDoc.exists()) {
            error(response, 404, "user_not_found");
            return;
        }
        String targetUid = codeDoc.getString("uid");
        DocumentSnapshot user = db.collection("users").document(targetUid).get().get();
        if (!user.exists()) {
            error(response, 404, "user_not_found");
            return;
        }

        JsonObject result = new JsonObject();
        result.addProperty("nickname", user.getString("nickname"));
        result.addProperty("friendCode", code);
        result.addProperty("isMe", uid.equals(targetUid));
        response.getWriter().write(result.toString());
    }

}
