-- BACKEND-222: 지점 마스터의 LEGACY(기존 카카오 지점) 행을 정리한다.
-- stores-sync 가 같은 지점을 COLLECTED / MANUAL 로 새로 만들었으므로, 찜(TB_FAVORITE_MAP)을 새 지점 id 로
-- 옮긴 뒤 LEGACY 를 지운다. 찜은 ON DELETE CASCADE 라 옮기지 않으면 같이 사라진다.
-- LEGACY 는 원천 키가 없어 같은 브랜드의 좌표와 지점명으로 짝을 찾는다.
-- 이 마이그레이션은 stores-sync 가 그 환경의 지점 마스터를 채운 뒤에 실행돼야 한다. 먼저 돌면 짝이 없어
-- 찜을 옮기지 못하고 LEGACY 와 찜이 모두 지워진다.

-- 1. LEGACY 마다 같은 브랜드 COLLECTED 중 지점명이 맞는 것, 없으면 가장 가까운 것을 짝으로 고른다.
--    지점명은 공백과 끝의 '점' 을 떼고 COLLECTED 이름에 포함되는지로 본다 (COLLECTED 는 브랜드 접두가 붙어 옴).
--    거리는 후보를 거르지 않고 정렬에만 쓴다. 포함 비교라 지점명이 여러 COLLECTED 에 맞을 수 있고
--    (예: '강남' → '강남역점', '강남대로점'), 맞는 게 없을 때도 DISTINCT ON 이 한 행을 골라야 하므로
--    distance_m 정렬이 없으면 짝이 임의로 정해진다.
CREATE TEMP TABLE legacy_match AS
SELECT DISTINCT ON (lg.id)
       lg.id AS legacy_id,
       c.id  AS collected_id,
       ST_DistanceSphere(lg.location, c.location) AS distance_m,
       lg.branch_name <> '' AND strpos(replace(c.branch_name, ' ', ''),
                                       regexp_replace(replace(lg.branch_name, ' ', ''), '점$', '')) > 0 AS name_match
FROM TB_PHOTO_BOOTH_LOCATION lg
JOIN TB_PHOTO_BOOTH_LOCATION c
  ON c.source_type = 'COLLECTED'
 AND c.brand_id = lg.brand_id
WHERE lg.source_type = 'LEGACY'
ORDER BY lg.id, name_match DESC, distance_m;

-- 2. 짝을 찾은 LEGACY 의 찜을 COLLECTED 로 옮긴다. 이미 둘 다 찜했으면 더 이른 created_at 을 남긴다.
INSERT INTO TB_FAVORITE_MAP (user_id, location_id, created_at)
SELECT f.user_id, m.collected_id, f.created_at
FROM TB_FAVORITE_MAP f
JOIN legacy_match m ON m.legacy_id = f.location_id
ON CONFLICT (user_id, location_id) DO UPDATE
SET created_at = LEAST(TB_FAVORITE_MAP.created_at, EXCLUDED.created_at);

-- 3. 짝이 없는 LEGACY 의 찜은 같은 브랜드, 같은 지점명의 MANUAL(관리자 등록 지점)로 옮긴다.
--    수집 사이트에 없는 지점은 TB_PHOTO_BOOTH_MANUAL 에 같은 이름으로 넣어 stores-sync 가 MANUAL 로 동기화한다.
INSERT INTO TB_FAVORITE_MAP (user_id, location_id, created_at)
SELECT f.user_id, m.id, f.created_at
FROM TB_FAVORITE_MAP f
JOIN TB_PHOTO_BOOTH_LOCATION lg ON lg.id = f.location_id AND lg.source_type = 'LEGACY'
JOIN TB_PHOTO_BOOTH_LOCATION m
  ON m.source_type = 'MANUAL'
 AND m.brand_id = lg.brand_id
 AND m.branch_name = lg.branch_name
WHERE lg.id NOT IN (SELECT legacy_id FROM legacy_match)
ON CONFLICT (user_id, location_id) DO UPDATE
SET created_at = LEAST(TB_FAVORITE_MAP.created_at, EXCLUDED.created_at);

-- 4. LEGACY 를 전부 지운다. 옮긴 뒤 남은 LEGACY 찜은 CASCADE 로 같이 지워진다.
DELETE FROM TB_PHOTO_BOOTH_LOCATION
WHERE source_type = 'LEGACY';

DROP TABLE legacy_match;
