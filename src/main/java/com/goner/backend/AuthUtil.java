package com.goner.backend;
import com.google.cloud.functions.HttpRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

import java.util.List;
import java.util.Map;

public class AuthUtil {
    private AuthUtil() {
    }

    // 토큰(JWT)의 가운데 부분을 열어서 uid(sub)를 꺼냄. 서명은 확인하지 않음
    private static String uidFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            String json = new String(java.util.Base64.getUrlDecoder().decode(parts[1]),
                    java.nio.charset.StandardCharsets.UTF_8);
            return com.google.gson.JsonParser.parseString(json)
                    .getAsJsonObject().get("sub").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    public static String verifyUid(HttpRequest request) {
        String header = null;
        for (Map.Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
            if (entry.getKey().equalsIgnoreCase("Authorization") && !entry.getValue().isEmpty()) {
                header = entry.getValue().get(0);
                break;
            }
        }

        if (header == null || !header.startsWith("Bearer ")) return null;
        String token = header.substring(7);
        if(System.getenv("FIREBASE_AUTH_EMULATOR_HOST")!=null){
            return uidFromToken(token);
        }
        try {
            FirebaseConfig.init();
            return FirebaseAuth.getInstance().verifyIdToken(token).getUid();
        } catch (FirebaseAuthException e) {
            return null;
        }
    }
}
