-- 规范符合性审计修正（2026-11-26）：出库明细补 busi_desc（SDO2 busiType=99 必填）
ALTER TABLE br_outbound_item ADD COLUMN busi_desc VARCHAR(256) NULL COMMENT '出库说明(busiType=99 必填)' AFTER goods_source_no;
