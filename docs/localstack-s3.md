# LocalStack S3 로컬 환경

이 문서는 로컬 개발 환경에서 S3를 대체하는 LocalStack 구성에 대해 다룹니다.

## 구성 요소

LocalStack은 AWS 서비스를 로컬에서 흉내 내는 컨테이너입니다. 이 프로젝트는 그중 S3만 사용합니다.

```text
docker-compose.yaml          localstack 컨테이너 (SERVICES: s3, 포트 4566)
infra/localstack/init-s3.sh  컨테이너 기동 후 yapp-local 버킷 생성
  -> /etc/localstack/init/ready.d 로 마운트되어 자동 실행

modules/aws/.../application-s3.yaml   local 프로파일 접속 정보
modules/aws/.../S3MediaStorageConfig  endpoint override + path-style 적용
neki-application/.../S3InMemoryBucketInitializer   버킷 확인 및 CORS 설정
neki-application/.../MediaTestController           수동 테스트용 엔드포인트
```

## 설정값

멀티모듈 전환 이후 S3 설정은 `modules/aws/src/main/resources/application-s3.yaml` 한 곳에서만 관리합니다. `neki-application/src/main/resources/application.yaml` 에도 `aws.s3` 블록이 있었으나, 프로파일 문서가 항상 이겨서 실제로는 읽히지 않는 죽은 설정이었기 때문에 제거했습니다.

local 프로파일의 값은 docker-compose 및 초기화 스크립트와 짝을 이룹니다.

- `bucket : yapp-local` — `init-s3.sh` 가 생성하는 버킷명과 동일해야 함
- `endpoint : http://localhost:4566` — docker-compose 의 포트 매핑과 동일해야 함
- `access-key / secret-key : test` — docker-compose 환경변수와 동일해야 함
- `base-url : http://localhost:4566/yapp-local` — 브라우저에서 객체를 직접 열어볼 URL
- `region : ap-northeast-2` — 프로파일 무관 공통값
- `presigned-url-expiration : 10m` — 프로파일 무관 공통값

## LocalStack 전용 분기는 어디에 있나?

`S3Properties.endpoint` 가 **LocalStack 여부를 판별하는 유일한 신호**입니다. staging과 prod 프로파일에는 `endpoint` 가 없어 `null` 이 되고, 실제 AWS 엔드포인트로 붙습니다.

`S3MediaStorageConfig` 는 `endpoint` 가 있을 때만 두 가지를 추가합니다.

- `endpointOverride` — 요청 대상을 LocalStack 으로 돌림
- path-style 접근 강제 — LocalStack 은 virtual-hosted style 을 지원하지 않으므로 필수

`S3InMemoryBucketInitializer` 도 같은 신호를 씁니다. `@Profile("local")` 인 동시에 `endpoint == null` 이면 아무 일도 하지 않고 종료합니다. 이 빈이 하는 일은 버킷 존재 확인과 CORS 설정 두 가지입니다. 허용 origin 은 `app.cors.allowed-origins` 를 그대로 사용하므로, 프론트 개발 서버 주소를 추가할 때는 그쪽 한 곳만 고치면 됩니다.

## base-url 은 왜 local 에만 있나?

staging 과 prod 프로파일에는 `base-url` 이 없습니다. 누락이 아니라 **필요가 없기 때문**입니다.

`base-url` 을 참조하는 코드는 `S3MediaStorageAdapter` 의 `findByKey` 와 `findAll` 두 메서드뿐이고, 이 둘의 유일한 호출처가 `MediaTestController` 입니다. 그리고 이 컨트롤러는 `@Profile("local")` 이라 staging 과 prod 에서는 빈으로 등록조차 되지 않습니다.

프로덕션 경로가 실제로 쓰는 메서드는 전부 `base-url` 과 무관합니다.

| 호출처 | 메서드 | 동작 |
|---|---|---|
| `GenerateUploadTicketUseCase` | `generateUploadTicket` | presigned URL 발급 |
| `ConfirmMediaUploadedUseCase` | `exists` | `bucket` + headObject |
| `GetImageByKeyUseCase` | `fetchBinaryByKey` | `bucket` + getObject |
| `GetMediasUseCase` | `fetchBinaryByKey` | `bucket` + getObject |

즉, 업로드는 presigned URL 로 나가고 조회는 바이너리를 직접 읽어옵니다. **staging 과 prod 에서 `base-url` 이 비어 있어도 정상입니다.** 나중에 CDN 을 붙이는 등 URL 을 직접 조립할 일이 생기면 그때 추가하면 됩니다.

## 실행 절차

```bash
docker compose up -d          # db, db-init, localstack, redis 기동
make run                      # SPRING_PROFILES 기본값이 local
```

컨테이너가 ready 상태가 되면 `init-s3.sh` 가 자동으로 실행되어 `yapp-local` 버킷을 만듭니다. 이후 애플리케이션이 뜨면서 `S3InMemoryBucketInitializer` 가 버킷 존재를 확인하고 CORS 를 설정합니다. 버킷이 없으면 `NoSuchBucketException` 을 로그에 남기고 넘어가므로, 초기화 실패는 기동 실패가 아니라 로그로만 드러납니다.

버킷 데이터는 `./localstack_data` 에 보존됩니다. 초기 상태로 되돌리려면 이 디렉토리를 지우고 컨테이너를 다시 올려야 합니다.

## 수동 테스트 엔드포인트

`MediaTestController` 는 `/api/media/test` 하위에 네 개를 제공합니다. local 전용입니다.

- `GET /api/media/test?prefix=temp/` — prefix 로 객체 목록 조회
- `POST /api/media/test/presigned?filename=&contentType=` — presigned 업로드 URL 발급
- `GET /api/media/test/object/{key}` — 객체 공개 URL 조회 (`base-url` 기반)
- `DELETE /api/media/test/object/{key}` — 객체 삭제

presigned URL 로 업로드할 때 CORS 가 걸리면 `app.cors.allowed-origins` 에 origin 이 빠져 있을 가능성을 의심해볼 수 있습니다. 값을 고친 뒤에는 애플리케이션을 재기동해야 `S3InMemoryBucketInitializer` 가 버킷 CORS 를 다시 적용합니다.

## 테스트 프로파일과의 관계

`test` 프로파일에서는 LocalStack 을 쓰지 않습니다. `S3MediaStorageConfig` 가 `@Profile("!test")` 라 S3 관련 빈이 아예 만들어지지 않고, 대신 `FakeMediaStorageConfig` 의 인메모리 구현이 `MediaStoragePort` 를 대신합니다. 따라서 E2E 테스트를 돌릴 때 docker compose 가 떠 있을 필요가 없습니다.

`neki-application/src/test/resources/application-test.yml` 에도 `aws.s3` 블록이 있지만, 이는 `S3Properties` 바인딩을 통과시키기 위한 더미값입니다.

## 정리

LocalStack 관련 설정은 `modules/aws` 에 모여 있고, LocalStack 이냐 실제 AWS 냐는 `aws.s3.endpoint` 의 존재 여부 하나로 갈립니다. 값을 바꿀 일이 생기면 docker-compose, `init-s3.sh`, `application-s3.yaml` 세 곳이 서로 맞는지 함께 확인해야 합니다. 특히 버킷명은 스크립트와 설정 양쪽에 하드코딩되어 있어 한쪽만 고치면 기동 시 CORS 설정이 조용히 실패합니다.
