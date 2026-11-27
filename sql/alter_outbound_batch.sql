ALTER TABLE br_outbound
  ADD COLUMN batch_no VARCHAR(60) NULL COMMENT '申报批次号' AFTER calculating_record_no;
