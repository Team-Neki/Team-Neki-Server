# 어드민 포즈 관리

공통 구성과 계층 규칙은 [어드민 앱 요청 flow](admin-flow.md)를 따릅니다.

## 구현 구조

`pose/api`의 Controller·Dto·Mapper가 요청을 변환하고 `PoseAdminFacade`가 트랜잭션 경계를 잡아 `:domain`의 `PoseService`에 위임합니다. `PoseServiceConfig`는 `PoseService`만 명시적으로 등록하므로 브랜드 관리 기능 없이 기동할 수 있습니다. `pose/infra/persist`는 admin용 `PoseRepository` 구현과 JPA·QueryDSL 조회를 소유합니다.

| Method | Path | 동작 |
| --- | --- | --- |
| GET | `/admin/v1/pose` | 목록 조회 (headCount 필터) |
| POST | `/admin/v1/pose` | 일괄 등록 |
| PATCH | `/admin/v1/pose/{poseId}` | 이미지 교체 |

목록 조회는 `CountedPage`를 사용하며 전체 건수와 전체 페이지 수를 반환합니다. 사용자 컨텍스트(스크랩, 정렬)가 필요한 API 전용 조회는 admin 어댑터에서 지원하지 않습니다.

인증이 없어 업로드한 포즈의 userId는 null로 저장합니다. `TB_POSE.user_id` NULL 허용 마이그레이션은 main의 `V30__alter_pose_user_id_nullable.sql`에 있으므로 중복 마이그레이션을 추가하지 않습니다.

## 이미지 변경

`PATCH /admin/v1/pose/{poseId}`의 body는 `{ "mediaId": n }`이며 mediaId는 필수입니다. `PoseService.updatePoseMedia`가 포즈를 조회하고 `Pose.changeMedia`로 교체합니다. dirty checking과 `@DynamicUpdate`로 media_id 변경을 저장합니다.

이미지 파일은 admin을 거치지 않습니다. 기존 apps:api의 media 경로에서 presigned URL을 발급받고 S3에 업로드한 뒤, admin은 mediaId를 포즈에 연결합니다.

```mermaid
sequenceDiagram
    participant A as 관리자 도구
    participant API as apps:api (media)
    participant S3 as S3
    participant ADM as apps:admin
    participant DB as DB

    A->>API: POST /api/media/upload (mediaType: POSE)
    API->>DB: Media 생성 (INITIATED)
    API-->>A: mediaId + presigned URL
    A->>S3: PUT presigned URL (이미지 바이너리)
    A->>ADM: PATCH /admin/v1/pose/{poseId}
    ADM->>DB: mediaId 존재·POSE 타입·상태 확인
    ADM->>S3: INITIATED면 객체 존재 확인 (HEAD)
    ADM->>DB: Media를 UPLOADED로 전환
    ADM->>DB: pose.changeMedia(mediaId)
    ADM-->>A: 200 OK
```

admin은 mediaId가 없거나 POSE 타입이 아니거나 S3 업로드가 끝나지 않았으면 포즈 변경 전체를 롤백합니다. 이전 mediaId의 TB_MEDIA 레코드와 S3 객체는 정리하지 않습니다. media 소유자와 관리자 계정의 연계 검증은 인증 도입과 함께 완료해야 합니다.

media 발급은 apps:api의 JWT 인증을 요구하므로 인증 없는 admin만으로 업로드 flow를 완결할 수 없습니다. admin 인증 도입 시 media 발급 경로를 함께 정리해야 합니다.
