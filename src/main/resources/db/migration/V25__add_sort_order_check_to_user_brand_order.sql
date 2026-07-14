-- sort_order 는 0부터 시작하는 인덱스이므로 음수가 될 수 없음을 DB 레벨에서 보장
ALTER TABLE TB_USER_BRAND_ORDER
    ADD CONSTRAINT chk_user_brand_order_sort_order_non_negative CHECK (sort_order >= 0);
