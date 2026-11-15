package com.goner.backend;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.IOException;

/**
 * Firebase Admin SDK 초기화 (함수 인스턴스당 1번).
 * Cloud Run 환경에서는 기본 서비스 계정 자격증명을 자동 사용한다.
 * 로컬에서는 GOOGLE_APPLICATION_CREDENTIALS 환경변수로 키 경로를 지정할 것 (키 파일은 커밋 금지).
 */
public final class FirebaseConfig {
    private FirebaseConfig() {}

    public static synchronized FirebaseApp init() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        try {
            FirebaseOptions.Builder builder = FirebaseOptions.builder();
            if(System.getenv("FIREBASE_AUTH_EMULATOR_HOST") != null) {
                builder.setCredentials(com.google.auth.oauth2.GoogleCredentials.newBuilder().build())
                        .setProjectId(System.getenv("GCLOUD_PROJECT"));
            } else {
                builder.setCredentials(com.google.auth.oauth2.GoogleCredentials.getApplicationDefault());
            }

            return FirebaseApp.initializeApp(builder.build());
        } catch (IOException e) {
            throw new IllegalStateException("Firebase 초기화 실패", e);
        }
    }
}
