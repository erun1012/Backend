# GONER Backend

약속 · 위치 공유 앱 **GONER**의 백엔드.
Firebase (Auth · Firestore · RTDB · Storage · FCM) + Cloud Run functions (Java 17).

## 구조

```
Backend/
├─ build.gradle / settings.gradle
├─ src/main/java/com/goner/backend/
│  ├─ FirebaseConfig.java        # Firebase Admin 초기화
│  └─ functions/                 # Cloud Run functions (HTTP)
├─ firestore.rules               # Firestore 보안 규칙
├─ database.rules.json           # Realtime DB 보안 규칙
├─ storage.rules                 # Storage 보안 규칙
├─ firebase.json                 # Firebase / Emulator 설정
└─ docs/BACKEND_TASKS.md         # 작업 목록 (대·중·소)
```

## 로컬 실행

```bash
./gradlew runFunction        # http://localhost:8080 (HelloFunction)
```

다른 함수 실행:

```bash
./gradlew runFunction -PrunFunction.target=com.goner.backend.functions.함수클래스명
```

Firebase 서비스 접근이 필요하면 서비스 계정 키를 **레포 밖** 경로에 두고 환경변수로 지정:

```bash
# Windows (PowerShell)
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\keys\serviceAccountKey.json"
```

## 규칙

- 서비스 계정 키, API 키(카카오 등)는 **절대 커밋하지 않는다** (`.gitignore` 참고).
- 브랜치: `main`은 PR로만 합친다. 작업은 `feature/기능명` 브랜치에서.
- 작업 목록은 `docs/BACKEND_TASKS.md` 기준으로 진행.
