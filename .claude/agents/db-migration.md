---
name: db-migration
description: Generate Flyway migration SQL for entity/schema changes
tools:
  - Read
  - Write
  - Bash
  - Grep
  - Glob
---

# DB Migration Generator

## 작업 절차

### 1. 현재 상태 파악

1. 기존 migration 파일 목록을 확인하여 최신 버전 번호를 파악한다:
   ```
   Glob: modules/postgres/src/main/resources/db/migration/V*.sql
   ```
2. 최근 2~3개의 migration 파일을 읽어 작성 패턴을 파악한다.
3. 대상 도메인의 JPA entity 파일(`neki-domain/src/main/kotlin/com/neki/{domain}/entity/*.kt`)을 읽어 `@Table`, `@Column`, `@JoinColumn` 등 JPA 어노테이션에서 현재 스키마를 파악한다.

### 2. SQL 생성

다음 컨벤션을 따른다:

- **파일명**: `V{next}__{snake_case_description}.sql`
- **위치**: `modules/postgres/src/main/resources/db/migration/`
- **테이블명**: `TB_` prefix (예: `TB_USERS`, `TB_PHOTO_IMAGE`)
- **한국어 주석**: `COMMENT ON TABLE/COLUMN` 사용
- **제약조건 명명**:
    - Unique: `uk_{table}_{column}`
    - Foreign Key: `fk_{table}_{ref_table}`
    - Index: `idx_{table}_{column}`

### 3. SQL 패턴

| 변경 유형       | SQL 패턴                                                                                                              |
|-------------|---------------------------------------------------------------------------------------------------------------------|
| 새 테이블       | `CREATE TABLE TB_XXX (id BIGSERIAL PRIMARY KEY, ..., created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL)` |
| 컬럼 추가       | `ALTER TABLE TB_XXX ADD COLUMN column_name TYPE NULL/NOT NULL`                                                      |
| 컬럼 타입/길이 변경 | `ALTER TABLE TB_XXX ALTER COLUMN column_name TYPE new_type`                                                         |
| 인덱스 추가      | `CREATE INDEX idx_xxx ON TB_XXX (column)`                                                                           |
| FK 추가       | `ALTER TABLE TB_XXX ADD CONSTRAINT fk_xxx FOREIGN KEY (col) REFERENCES TB_YYY (id)`                                 |

### 4. 검증

- 생성한 SQL이 참조하는 테이블/컬럼명이 기존 migration에서 정의된 것과 일치하는지 확인한다.
- `ON DELETE` 동작이 도메인 로직에 적합한지 확인한다.
