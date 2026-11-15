package com.goner.backend;
import com.google.cloud.functions.HttpRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

import java.util.List;
import java.util.Map;

public class AuthUtil {
    private AuthUtil() {
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
        try {
            FirebaseConfig.init();
            return FirebaseAuth.getInstance().verifyIdToken(header.substring(7)).getUid();
        } catch (FirebaseAuthException e) {
            return null;
        }
    }
}
