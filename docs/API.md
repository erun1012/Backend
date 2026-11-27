# GONER API

## 공통
- 인증: 모든 API에 `Authorization: Bearer <Firebase ID 토큰>` 헤더 필요
- 형식: JSON (UTF-8)
- 에러 응답: `{"error":"에러코드"}`
- 공통 에러: 401 `unauthorized`

| 함수 | 메서드 | 로컬 포트 |
|---|---|---|
| CheckFriendCode | GET | 8081 |
| SaveProfile | POST | 8082 |
| CompleteOnboarding | POST | 8083 |
| GetMe | GET | 8084 |
| DeleteAccount | POST | 8085 |

## CheckFriendCode: 친구코드 중복확인
요청: `GET /?code=gahee12` (영문 소문자·숫자 4~16자)

응답: 내가 이미 쓰는 코드도 `true`
```json
{"available": true}
```
에러: 400 `invalid_code`

## SaveProfile: 프로필 저장
요청: 닉네임 1~10자
```json
{"nickname": "가희", "friendCode": "gahee12"}
```
응답
```json
{"ok": true}
```
온보딩을 마친 뒤 친구코드를 바꾸면 180일 동안 다시 바꿀 수 없음

| 상태 | 에러코드 | 의미 |
|---|---|---|
| 400 | `invalid_body` | 본문이 JSON 객체가 아님 |
| 400 | `invalid_nickname` | 닉네임 길이 오류 |
| 400 | `invalid_code` | 친구코드 형식 오류 |
| 403 | `code_change_locked` | 친구코드 변경 잠금 |
| 405 | `method_not_allowed` | POST가 아님 |
| 409 | `code_taken` | 다른 사람이 사용 중 |

## CompleteOnboarding: 온보딩 완료
요청: 값이 없으면 `false`
```json
{"notificationEnabled": true}
```
응답
```json
{"ok": true}
```
| 상태 | 에러코드 | 의미 |
|---|---|---|
| 400 | `invalid_body` | 본문이 JSON 객체가 아님 |
| 400 | `profile_required` | 프로필 저장 전 |
| 405 | `method_not_allowed` | POST가 아님 |

## GetMe: 내 정보 조회
요청: 본문 없음

응답
```json
{
  "onboardingCompleted": true,
  "notificationEnabled": false,
  "nickname": "가희",
  "friendCode": "gahee12",
  "friendCodeLocked": true,
  "friendCodeEditableAt": "2027-05-26T11:00:00Z"
}
```
- 저장 전이면 `nickname`, `friendCode`는 `null`
- 잠금이 아니면 `friendCodeEditableAt`은 `null`
- 시각은 UTC

에러: 405 `method_not_allowed`

## DeleteAccount: 회원탈퇴
요청: 본문 없음

응답
```json
{"ok": true}
```
- 사용자 정보, 친구코드, 로그인 계정 삭제 (에뮬레이터에서는 로그인 계정 삭제 생략)

| 상태 | 에러코드 | 의미 |
|---|---|---|
| 405 | `method_not_allowed` | POST가 아님 |
| 500 | `auth_delete_failed` | 로그인 계정 삭제 실패 (Firestore 정보는 이미 삭제됨) |

## Friend (로컬 포트 8086)

### GET /search?code=친구코드
응답: `{"nickname":"bob","friendCode":"bob12","isMe":false}`
에러: 400 `invalid_code`, 404 `user_not_found`

### POST /request
요청: `{"friendCode":"bob12"}`
응답: `{"ok":true}` (요청은 7일 뒤 만료)
에러: 400 `invalid_body` `invalid_code` `cannot_add_self`, 404 `user_not_found`,
409 `already_friends` `already_requested` `request_received`

### GET /requests
응답: `{"received":[{"nickname":"alice","friendCode":"alice1","createdAt":"..."}],"sent":[]}`
- 만료된 요청은 제외, 시각은 UTC

### POST /accept, /reject, /cancel
요청: `{"friendCode":"상대 친구코드"}`
응답: `{"ok":true}`
- accept·reject는 받은 요청, cancel은 보낸 요청에 사용
  에러: 400 `invalid_body` `invalid_code` `cannot_add_self`,
  404 `user_not_found` `request_not_found`

### GET /list
응답: `{"friends":[{"nickname":"bob","friendCode":"bob12"}]}`

### POST /remove
요청: `{"friendCode":"상대 친구코드"}`
응답: `{"ok":true}`
에러: 400 `invalid_body` `invalid_code` `cannot_add_self`,
404 `user_not_found` `not_friend`