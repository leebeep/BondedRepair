-- =====================================================================
-- 库存 V2：以物料编号为唯一的库存余额表 + 台账流水补充单据编号/索引
-- 依赖：br_schema.sql（br_stock_ledger / br_material）
-- 幂等性：br_material_stock 用 CREATE TABLE IF NOT EXISTS；
--        biz_no 回填与库存初始化可重复执行（结果一致）
-- =====================================================================

-- 1. 物料库存余额表（唯一库存事实，物料为唯一维度）
CREATE TABLE IF NOT EXISTS br_material_stock (
  material_id   BIGINT UNSIGNED NOT NULL COMMENT '物料id(br_material.id)',
  mark_type     CHAR(1) NOT NULL COMMENT '货物类型(冗余,汇总用)',
  qty           DECIMAL(19,6) NOT NULL DEFAULT 0 COMMENT '当前结存',
  last_biz_time DATETIME NULL COMMENT '最近变动时间',
  update_time   DATETIME NULL,
  PRIMARY KEY (material_id),
  KEY idx_mark (mark_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料库存余额';

-- 2. 台账流水补充：单据编号（列表/筛选用）与查询索引
ALTER TABLE br_stock_ledger
  ADD COLUMN biz_no VARCHAR(60) NULL COMMENT '单据编号(入库/发料/收料/出库单号)' AFTER biz_id;

ALTER TABLE br_stock_ledger
  ADD KEY idx_source (material_id, goods_source_type, goods_source_no),
  ADD KEY idx_biz (biz_table, biz_id),
  ADD KEY idx_no (biz_no);

-- 3. 历史流水回填单据编号
UPDATE br_stock_ledger l JOIN br_inbound  i ON l.biz_table = 'br_inbound'  AND l.biz_id = i.id SET l.biz_no = i.inbound_no  WHERE l.biz_no IS NULL;
UPDATE br_stock_ledger l JOIN br_issue    i ON l.biz_table = 'br_issue'    AND l.biz_id = i.id SET l.biz_no = i.issue_no    WHERE l.biz_no IS NULL;
UPDATE br_stock_ledger l JOIN br_receive  i ON l.biz_table = 'br_receive'  AND l.biz_id = i.id SET l.biz_no = i.receive_no  WHERE l.biz_no IS NULL;
UPDATE br_stock_ledger l JOIN br_outbound i ON l.biz_table = 'br_outbound' AND l.biz_id = i.id SET l.biz_no = i.outbound_no WHERE l.biz_no IS NULL;

-- 4. 从流水初始化物料库存余额（可重复执行）
INSERT INTO br_material_stock (material_id, mark_type, qty, last_biz_time, update_time)
SELECT l.material_id, MAX(l.mark_type), SUM(l.qty), MAX(l.create_time), now()
FROM br_stock_ledger l
GROUP BY l.material_id
ON DUPLICATE KEY UPDATE qty = VALUES(qty), last_biz_time = VALUES(last_biz_time), update_time = now();

-- 5. 校验：库存表余额应等于流水合计，差异行返回为空结果集应 0 行
SELECT s.material_id, s.qty AS stock_qty, t.ledger_qty
FROM br_material_stock s
JOIN (SELECT material_id, SUM(qty) AS ledger_qty FROM br_stock_ledger GROUP BY material_id) t ON t.material_id = s.material_id
WHERE s.qty <> t.ledger_qty;
