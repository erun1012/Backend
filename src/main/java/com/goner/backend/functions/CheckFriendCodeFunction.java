package com.goner.backend.functions;
import com.goner.backend.AuthUtil;
import com.goner.backend.FirebaseConfig;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpResponse;
import com.google.firebase.cloud.FirestoreClient;

import java.util.regex.Pattern;

public class CheckFriendCodeFunction implements HttpFunction{
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z0-9]{4,16}$");

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception{
        response.setContentType("application/json; charset=utf-8");

        String uid = AuthUtil.verifyUid(request);
        if (uid == null) {
            response.setStatusCode(401);
            response.getWriter().write("{\"error\":\"unauthorized\"}");
            return;
        }

        String code = request.getFirstQueryParameter("code").orElse("");
        if (!CODE_PATTERN.matcher(code).matches()) {
            response.setStatusCode(400);
            response.getWriter().write("{\"error\":\"invalid_code\"}");
            return;
        }

        FirebaseConfig.init();
        DocumentSnapshot doc = FirestoreClient.getFirestore()
                .collection("friendCodes").document(code).get().get();

        boolean available = !doc.exists() || uid.equals(doc.getString("uid"));
        response.getWriter().write("{\"available\":" + available + "}");
    }
}

