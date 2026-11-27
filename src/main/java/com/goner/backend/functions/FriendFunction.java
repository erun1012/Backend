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

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;

import java.sql.Time;
import java.util.Map;

import static com.goner.backend.ApiUtil.error;
import static com.goner.backend.ApiUtil.getString;
import static com.goner.backend.ApiUtil.parseBody;

public class FriendFunction implements HttpFunction {
    private static final long REQUEST_EXPIRE_SECONDS = 7*24*60*60;

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
        } else if(path.equals("/request") && method.equals("POST")) {
            sendRequest(request, response, db, uid);
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

    private void sendRequest(HttpRequest request, HttpResponse response, Firestore db, String uid) throws Exception {
        JsonObject body = parseBody(request);
        if(body==null) {
            error(response,400,"invalid_body");
            return;
        }
        String code = getString(body,"friendCode");
        if(!code.matches("^[a-z0-9]{4,16}$")) {
            error(response,400,"invalid_code");
            return;
        }

        DocumentSnapshot codeDoc = db.collection("friendCodes").document(code).get().get();
        if(!codeDoc.exists()) {
            error(response,404,"user_not_found");
            return;
        }
        String targetUid = codeDoc.getString("uid");
        if(uid.equals(targetUid)) {
            error(response,400,"cannot_add_self");
            return;
        }

        String result = db.runTransaction(tx -> {
            DocumentReference friendRef = db.collection("users").document(uid).collection("friends").document(targetUid);
            DocumentReference sentRef = db.collection("friendRequests").document(uid+"_"+targetUid);
            DocumentReference receivedRef = db.collection("friendRequests").document(targetUid+"_"+uid);

            DocumentSnapshot friend = tx.get(friendRef).get();
            DocumentSnapshot sent = tx.get(sentRef).get();
            DocumentSnapshot received = tx.get(receivedRef).get();

            if(friend.exists()) return "already_friends";
            if(isActive(sent)) return "already_requested";
            if(isActive(received)) return "request_received";

            Timestamp now = Timestamp.now();
            Timestamp expireAt = Timestamp.ofTimeSecondsAndNanos(now.getSeconds() + REQUEST_EXPIRE_SECONDS,0);

            tx.set(sentRef,Map.of(
                    "fromUid", uid,
                    "toUid", targetUid,
                    "createdAt", now,
                    "expiresAt", expireAt
            ));
            return "ok";

        }).get();

        if(!result.equals("ok")) {
            error(response,409,result);
            return;
        }
        response.getWriter().write("{\"ok\":true}");

    }

    private static boolean isActive(DocumentSnapshot doc) {
        return doc.exists() && doc.getTimestamp("expiresAt").compareTo(Timestamp.now()) > 0;
    }

}
