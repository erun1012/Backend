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
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(com.google.auth.oauth2.GoogleCredentials.getApplicationDefault())
                    // TODO: RTDB URL 확정되면 환경변수로 주입
                    // .setDatabaseUrl(System.getenv("FIREBASE_DATABASE_URL"))
                    .build();
            return FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            throw new IllegalStateException("Firebase 초기화 실패", e);
        }
    }
}
