---
name: presigned-url-flow
description: Load when working with S3 presigned URL uploads, media/image upload flow, photo registration, or media entity lifecycle management.
---

# Presigned URL 이미지 업로드 Flow

## Actors (참여자)

| Actor          | 역할                                    |
|----------------|---------------------------------------|
| **Client**     | 모바일 앱 (iOS/Android)                   |
| **API Server** | Spring Boot 백엔드 (Neki)                |
| **Database**   | PostgreSQL (TB_MEDIA, TB_PHOTO_IMAGE) |
| **S3**         | AWS S3 Object Storage                 |

---

## Sequence Flow

### Phase 1: Presigned URL 발급

```
1. Client -> API Server : POST /api/media/upload (JWT 인증)
   - Body: { items: [{ filename, contentType, mediaType, width, height, size }] }

2. API Server -> Database : Media 엔티티 생성 (status = INITIATED)
   - storageKey: "{mediaType.prefix}/{UUID}.{extension}"
   - 예: "photo-booth/550e8400-...-446655440000.jpg"

3. API Server -> S3 : PutObject Presign 요청
   - bucket, key(storageKey), contentType
   - signatureDuration: 10분

4. S3 -> API Server : Presigned URL 반환

5. API Server -> Client : 응답
   - { method: "PUT", expiresIn, items: [{ mediaId, uploadTicket(presigned URL), contentType }] }
```

### Phase 2: S3 직접 업로드

```
6. Client -> S3 : PUT {presigned URL} (서버 경유하지 않음)
   - Header: Content-Type: image/jpeg
   - Body: [바이너리 이미지 데이터]

7. S3 -> Client : 200 OK (업로드 성공)
```

### Phase 3: 업로드 확인 및 메타데이터 저장

```
8. Client -> API Server : POST /api/photos (JWT 인증)
   - Body: { folderId, uploads: [{ mediaId, memo }], favorite }

9. API Server -> Database : mediaId로 Media 엔티티 조회
   - ownerId 검증 (본인 소유 확인)

10. API Server -> S3 : 파일 존재 여부 확인 (HEAD Object)
    - key: media.storageKey

11. S3 -> API Server : 존재 확인 응답

12. API Server -> Database : Media 상태 업데이트 (INITIATED -> UPLOADED)

13. API Server -> Database : PhotoImage 엔티티 생성
    - { userId, mediaId, folderId, memo }

14. API Server -> Database : (favorite=true인 경우) FavoriteImage 추가

15. API Server -> Client : 200 OK (성공)
```

---

## State Diagram: Media Status Lifecycle

```
[INITIATED] ---(S3 업로드 확인)---> [UPLOADED]
[INITIATED] ---(만료/실패)-------> [FAILED]
[UPLOADED]  ---(삭제 요청)-------> [DELETE_REQUESTED]
[DELETE_REQUESTED] ---(S3 삭제 완료)---> [DELETED]
```

---

## Data Model Relationships

```
TB_MEDIA (미디어 메타데이터)
├── id (PK)
├── storage_key (S3 object key, unique)
├── owner_id (FK -> TB_USER)
├── media_type (PHOTO_BOOTH | USER_PROFILE | ATTACHMENT | LOGO | POSE | TEMP)
├── status (INITIATED | UPLOADED | FAILED | DELETE_REQUESTED | DELETED)
├── content_type (image/jpeg, image/png 등)
├── width, height, size
└── created_at, updated_at

TB_PHOTO_IMAGE (사진-미디어 연결)
├── id (PK)
├── user_id (FK -> TB_USER)
├── media_id (FK -> TB_MEDIA)
├── folder_id (FK -> TB_FOLDER, nullable)
├── memo (nullable)
└── deleted_at (soft delete)
```

---

## S3 Bucket 디렉터리 구조

```
{bucket}/
├── user-profiles/    (프로필 이미지, TTL 24h 캐시)
├── photo-booth/      (포토부스 사진, 캐시 없음)
├── attachments/      (첨부파일, 캐시 없음)
├── logo/             (로고, TTL 24h 캐시)
├── pose/             (포즈 이미지, TTL 24h 캐시)
└── temp/             (임시 파일, 캐시 없음)
```

---

## Key Design Decisions

| 설계 결정                  | 이유                   |
|------------------------|----------------------|
| 클라이언트가 S3에 직접 업로드      | 서버 부하 최소화, 대용량 파일 처리 |
| Presigned URL 만료시간 10분 | 보안과 사용성의 균형          |
| 2단계 확인 (업로드 → 확인)      | 업로드 실패 시 고아 레코드 방지   |
| UUID 기반 storage key    | 파일명 충돌 방지, URL 예측 불가 |
| Media/PhotoImage 분리    | 미디어 재사용 가능, 도메인 분리   |

---

## Component Diagram

```
┌─────────┐     ┌──────────────────────────────────────────────┐     ┌─────┐
│         │     │              API Server                      │     │     │
│         │ 1.  │  ┌─────────────────┐  ┌────────────────────┐ │     │     │
│         │────>│  │ MediaController  │->│ GenerateUploadTicket│─│──>  │     │
│         │     │  │ POST /api/media  │  │ UseCase            │ │ 3.  │     │
│         │<────│  │   /upload        │  │                    │<│──   │     │
│         │ 5.  │  └─────────────────┘  └────────────────────┘ │     │     │
│         │     │                                              │     │     │
│ Client  │ 6.  │                                              │     │ S3  │
│         │────────────────────────────────────────────────────────>  │     │
│         │<───────────────────────────────────────────────────────   │     │
│         │ 7.  │                                              │     │     │
│         │     │  ┌─────────────────┐  ┌────────────────────┐ │     │     │
│         │ 8.  │  │ PhotoController  │->│ UploadPhotosUseCase│ │     │     │
│         │────>│  │ POST /api/photos │  │                   │─│──>  │     │
│         │     │  │                  │  │  ┌───────────────┐│ │ 10. │     │
│         │<────│  │                  │  │  │ConfirmMedia   ││<│──   │     │
│         │ 15. │  │                  │  │  │UploadedUseCase││ │     │     │
│         │     │  └─────────────────┘  │  └───────────────┘│ │     │     │
│         │     │                       └────────────────────┘ │     │     │
└─────────┘     │              │                               │     └─────┘
                └──────────────│───────────────────────────────┘
                               │ 2, 9, 12, 13, 14
                               v
                        ┌──────────────┐
                        │  PostgreSQL   │
                        │  TB_MEDIA     │
                        │  TB_PHOTO_IMAGE│
                        └──────────────┘
```

---

## API Spec Summary

### POST /api/media/upload

**Request:**

```json
{
  "items": [
    {
      "filename": "photo1.jpg",
      "contentType": "image/jpeg",
      "mediaType": "PHOTO_BOOTH",
      "width": 1920,
      "height": 1080,
      "size": 2048576
    }
  ]
}
```

**Response:**

```json
{
  "resultCode": "SUCCESS",
  "data": {
    "method": "PUT",
    "expiresIn": "2026-02-25T07:35:00Z",
    "items": [
      {
        "mediaId": 1,
        "uploadTicket": "https://{bucket}.s3.{region}.amazonaws.com/{key}?X-Amz-Algorithm=...",
        "contentType": "image/jpeg"
      }
    ]
  }
}
```

### POST /api/photos

**Request:**

```json
{
  "folderId": 123,
  "uploads": [
    {
      "mediaId": 1,
      "memo": "Family photo"
    }
  ],
  "favorite": true
}
```

**Response:**

```json
{
  "resultCode": "SUCCESS",
  "data": null
}
```
