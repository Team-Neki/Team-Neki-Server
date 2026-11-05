-- expose_to_map was added in V28 with DEFAULT false, while the public map did not yet read it.
-- Keep every existing active brand visible when the map starts honoring this flag.
UPDATE TB_BRAND SET expose_to_map = true WHERE deleted_at IS NULL;
