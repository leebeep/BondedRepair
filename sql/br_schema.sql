-- =====================================================================
-- 保税维修工作台 业务库表 V1（在 bonded_repair 库执行，须先执行 ry/quartz 脚本）
-- 镜像表：严格按《企业客户端数据采集传输规范 v1.4》§3.1.2~§3.5.2 建表脚本
-- 业务表：按 docs/design/保税维修工作台详细设计-V1.md
-- =====================================================================

-- ---------- 镜像表（推送海关，客户端采集；系统只 INSERT / 只读回执字段） ----------
CREATE TABLE `epd101_data` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `execute_type` varchar(1) DEFAULT NULL COMMENT '执行策略(1:备案、2:变更、3:作废)',
  `dcl_typecd` varchar(1) DEFAULT NULL COMMENT '申报类型代码 1-备案/申请、2-变更、3-作废',
  `enterp_cd` varchar(60) DEFAULT NULL COMMENT '企业编号',
  `enterp_nm` varchar(500) DEFAULT NULL COMMENT '企业名称',
  `master_cuscd` varchar(4) DEFAULT NULL COMMENT '主管关区代码',
  `int_product_no` varchar(60) DEFAULT NULL COMMENT '物料/产品编号',
  `int_product_name` varchar(500) DEFAULT NULL COMMENT '物料/产品名称(物料名称原样，不拼接标记)',
  `version` varchar(19) DEFAULT NULL COMMENT '版本号',
  `g_type` varchar(1) DEFAULT NULL COMMENT '料件成品标志 I-料件 E-成品 S-半成品',
  `code_ts` varchar(10) DEFAULT NULL COMMENT 'HS编码',
  `g_name` varchar(255) DEFAULT NULL COMMENT 'HS商品名称',
  `g_model` varchar(255) DEFAULT NULL COMMENT '商品规格型号',
  `calc_unit` varchar(60) DEFAULT NULL COMMENT '计量单位',
  `unit_1` varchar(3) DEFAULT NULL COMMENT '法定计量单位',
  `unit_2` varchar(3) DEFAULT NULL COMMENT '法定第二计量单位',
  `duty_mode` varchar(1) DEFAULT NULL COMMENT '征免方式',
  `total_gross_wt` decimal(19,5) DEFAULT NULL COMMENT '货物毛重',
  `total_net_wt` decimal(19,5) DEFAULT NULL COMMENT '货物净重',
  `appr_max_surp_qty` decimal(19,5) DEFAULT NULL COMMENT '批准最大余数量(预留)',
  `note` varchar(1000) DEFAULT NULL COMMENT '备注',
  `is_bonded` varchar(1) DEFAULT NULL COMMENT '是否保税',
  `uuid` varchar(64) DEFAULT NULL COMMENT '预录入编号(客户端写回)',
  `result_status` varchar(16) DEFAULT NULL COMMENT '回执状态(客户端写回)',
  `msg` varchar(256) DEFAULT NULL COMMENT '回执消息(客户端写回)',
  `result_time` datetime DEFAULT NULL COMMENT '回执时间(客户端写回)',
  `over_time` tinyint(1) DEFAULT NULL COMMENT '重试次数(客户端写回)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='料件成品备案镜像表(EPD101)';

CREATE TABLE `pto2_data` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `execute_type` varchar(1) DEFAULT NULL COMMENT '执行策略(1:备案、2:变更、3:作废)',
  `take_order_cd` varchar(60) DEFAULT NULL COMMENT '入库单编号',
  `rela_ent_nm` varchar(500) DEFAULT NULL COMMENT '关联企业名称',
  `material_cd` varchar(60) DEFAULT NULL COMMENT '物料编号',
  `material_desc` varchar(500) DEFAULT NULL COMMENT '物料名称',
  `material_version` int DEFAULT NULL COMMENT '物料版本号',
  `specifications_models` varchar(255) DEFAULT NULL COMMENT '规格型号',
  `storage_cnt` decimal(28,6) DEFAULT NULL COMMENT '入库数量',
  `price` decimal(19,6) DEFAULT NULL COMMENT '单价',
  `currency` varchar(30) DEFAULT NULL COMMENT '币制',
  `is_bonded` varchar(1) DEFAULT NULL COMMENT '是否保税 0-非保税 1-保税',
  `calc_unit` varchar(60) DEFAULT NULL COMMENT '计量单位',
  `in_out_time` datetime DEFAULT NULL COMMENT '入库时间',
  `busi_type` varchar(2) DEFAULT NULL COMMENT '入库业务类型 01-采购 02-退货 03-抽检退库 04-待维修货物 05-维修用料 99-其他',
  `busid_esc` varchar(256) DEFAULT NULL COMMENT '入库说明(规范脚本字段名)',
  `busi_form_num` varchar(60) DEFAULT NULL COMMENT '入库业务单证编号',
  `rela_customs_form_type` varchar(2) DEFAULT NULL COMMENT '关联申报单证类型',
  `rela_customs_form_num` varchar(60) DEFAULT NULL COMMENT '关联申报单证编号',
  `goods_source_type` varchar(1) DEFAULT NULL COMMENT '货物来源单证类型 g/i/h',
  `goods_source_no` varchar(60) DEFAULT NULL COMMENT '货物来源单证编号',
  `brand_type` varchar(1) DEFAULT NULL COMMENT '品牌类型',
  `trade_mode_cd` varchar(4) DEFAULT NULL COMMENT '监管方式',
  `unit1_num` decimal(19,5) DEFAULT NULL COMMENT '第一法定数量',
  `unit2_num` decimal(19,5) DEFAULT NULL COMMENT '第二法定数量',
  `arriving_country_code` varchar(3) DEFAULT NULL COMMENT '启抵国(地区)',
  `end_country_code` varchar(3) DEFAULT NULL COMMENT '原产国(地区)',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `uuid` varchar(64) DEFAULT NULL COMMENT '预录入编号(客户端写回)',
  `result_status` varchar(16) DEFAULT NULL COMMENT '回执状态(客户端写回)',
  `msg` varchar(256) DEFAULT NULL COMMENT '回执消息(客户端写回)',
  `result_time` datetime DEFAULT NULL COMMENT '回执时间(客户端写回)',
  `over_time` tinyint(1) DEFAULT NULL COMMENT '重试次数(客户端写回)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP入库清单镜像表(PTO2)';

CREATE TABLE `rrd2_data` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `execute_type` varchar(1) DEFAULT NULL COMMENT '执行策略(1:备案、2:变更、3:作废)',
  `finish_material_num` varchar(60) DEFAULT NULL COMMENT '发料单号',
  `finish_material_tm` datetime DEFAULT NULL COMMENT '发料日期',
  `work_no` varchar(60) DEFAULT NULL COMMENT '工单单号',
  `goods_source_type` varchar(1) DEFAULT NULL COMMENT '货物来源单证类型 g/i',
  `goods_source_no` varchar(60) DEFAULT NULL COMMENT '货物来源单证编号',
  `price` decimal(19,6) DEFAULT NULL COMMENT '单价',
  `material_cd` varchar(60) DEFAULT NULL COMMENT '料件编号',
  `material_desc` varchar(500) DEFAULT NULL COMMENT '料件名称',
  `material_version` int DEFAULT NULL COMMENT '物料版本号',
  `issue_material_qty` decimal(28,6) DEFAULT NULL COMMENT '发料数量',
  `calc_unit` varchar(60) DEFAULT NULL COMMENT '计量单位',
  `is_bonded` varchar(1) DEFAULT NULL COMMENT '是否保税 0-非保税 1-保税',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `uuid` varchar(64) DEFAULT NULL COMMENT '预录入编号(客户端写回)',
  `result_status` varchar(16) DEFAULT NULL COMMENT '回执状态(客户端写回)',
  `msg` varchar(256) DEFAULT NULL COMMENT '回执消息(客户端写回)',
  `result_time` datetime DEFAULT NULL COMMENT '回执时间(客户端写回)',
  `over_time` tinyint(1) DEFAULT NULL COMMENT '重试次数(客户端写回)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP工单发料镜像表(RRD2)';

CREATE TABLE `mso2_data` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `execute_type` varchar(1) DEFAULT NULL COMMENT '执行策略 1:备案、2:变更、3:作废',
  `return_material_no` varchar(36) DEFAULT NULL COMMENT '收料单号',
  `return_time` datetime DEFAULT NULL COMMENT '收料日期',
  `work_no` varchar(60) DEFAULT NULL COMMENT '工单单号',
  `material_cd` varchar(60) DEFAULT NULL COMMENT '料件编号',
  `material_desc` varchar(500) DEFAULT NULL COMMENT '料件名称',
  `material_version` decimal(28,6) DEFAULT NULL COMMENT '物料版本号(规范脚本口径)',
  `material_num` decimal(28,6) DEFAULT NULL COMMENT '收料数量',
  `calc_unit` varchar(60) DEFAULT NULL COMMENT '计量单位',
  `is_bonded` varchar(2) DEFAULT NULL COMMENT '是否保税 0-非保税 1-保税',
  `return_type` varchar(1) DEFAULT NULL COMMENT '收料类型 1-未使用料件 5-已维修货物 6-维修用料件 7-坏损零部件 8-维修服务费 9-边角料 a-残次品',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `uuid` varchar(64) DEFAULT NULL COMMENT '预录入编号(客户端写回)',
  `result_status` varchar(16) DEFAULT NULL COMMENT '回执状态(客户端写回)',
  `msg` varchar(256) DEFAULT NULL COMMENT '回执消息(客户端写回)',
  `result_time` datetime DEFAULT NULL COMMENT '回执时间(客户端写回)',
  `over_time` tinyint(1) DEFAULT NULL COMMENT '重试次数(客户端写回)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP工单收料镜像表(MSO2)';

CREATE TABLE `sdo2_data` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `execute_type` varchar(1) DEFAULT NULL COMMENT '执行策略(1:备案、2:变更、3:作废)',
  `rela_ent_nm` varchar(500) DEFAULT NULL COMMENT '关联企业名称',
  `releaseno` varchar(60) DEFAULT NULL COMMENT '出库单编号',
  `material_cd` varchar(60) DEFAULT NULL COMMENT '物料编号',
  `material_desc` varchar(500) DEFAULT NULL COMMENT '物料名称',
  `version` int DEFAULT NULL COMMENT '物料版本号',
  `specifications_models` varchar(255) DEFAULT NULL COMMENT '规格型号',
  `delivery_cnt` decimal(28,6) DEFAULT NULL COMMENT '出库数量(负值=出库退回)',
  `price` decimal(19,6) DEFAULT NULL COMMENT '单价',
  `calc_unit` varchar(60) DEFAULT NULL COMMENT '计量单位',
  `currency` varchar(30) DEFAULT NULL COMMENT '币制',
  `is_bonded` varchar(1) DEFAULT NULL COMMENT '是否保税 0-非保税 1-保税',
  `in_out_time` datetime DEFAULT NULL COMMENT '出库时间',
  `busi_type` varchar(2) DEFAULT NULL COMMENT '出库业务类型 01-销售 02-抽检 03-销毁 04-复运出境 05-复运回境内区外 99-其他',
  `busi_desc` varchar(256) DEFAULT NULL COMMENT '出库说明',
  `busi_form_num` varchar(60) DEFAULT NULL COMMENT '出库业务单证编号',
  `rela_customs_form_type` varchar(2) DEFAULT NULL COMMENT '关联申报单证类型',
  `rela_customs_form_num` varchar(60) DEFAULT NULL COMMENT '关联申报单证编号',
  `calculating_record_no` varchar(60) DEFAULT NULL COMMENT '加工增值核算备案编号',
  `goods_source_type` varchar(1) DEFAULT NULL COMMENT '货物来源单证类型 g/i',
  `goods_source_no` varchar(60) DEFAULT NULL COMMENT '货物来源单证编号',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `uuid` varchar(64) DEFAULT NULL COMMENT '预录入编号(客户端写回)',
  `result_status` varchar(16) DEFAULT NULL COMMENT '回执状态(客户端写回)',
  `msg` varchar(256) DEFAULT NULL COMMENT '回执消息(客户端写回)',
  `result_time` datetime DEFAULT NULL COMMENT '回执时间(客户端写回)',
  `over_time` tinyint(1) DEFAULT NULL COMMENT '重试次数(客户端写回)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP出库清单镜像表(SDO2)';

-- ---------- 业务表 ----------
CREATE TABLE br_material (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  material_cd   VARCHAR(60)  NOT NULL COMMENT '物料编号',
  version       VARCHAR(19)  NOT NULL DEFAULT '1' COMMENT '版本号',
  calc_unit     VARCHAR(60)  NOT NULL COMMENT '计量单位(字典 br_jldw)',
  material_name VARCHAR(500) NOT NULL COMMENT '物料名称(字面全名，按录入原样；六类标记 markType 为独立字段)',
  mark_type     CHAR(1)      NOT NULL COMMENT '货物类型(六类标记) 1-待修复 2-已修复 3-无法修复 4-维修用料 5-旧件坏件 6-边角料 8-维修服务费',
  g_type        CHAR(1)      NOT NULL COMMENT 'I-料件 E-成品 S-半成品',
  is_bonded     CHAR(1)      NOT NULL DEFAULT '1' COMMENT '0-非保税 1-保税',
  code_ts       VARCHAR(10)  NULL COMMENT 'HS编码(保税必填)',
  g_name        VARCHAR(255) NULL COMMENT 'HS商品名称',
  g_model       VARCHAR(255) NULL COMMENT '规格型号',
  unit1         VARCHAR(3)   NULL COMMENT '法定计量单位',
  unit2         VARCHAR(3)   NULL COMMENT '法定第二计量单位',
  duty_mode     VARCHAR(1)   NULL COMMENT '征免方式',
  total_gross_wt VARCHAR(25) NULL COMMENT '货物毛重',
  total_net_wt  VARCHAR(25)  NULL COMMENT '货物净重',
  declare_status VARCHAR(16) NULL DEFAULT '0' COMMENT '备案状态 0-未申报 1-已申报 2-通过 3-退单',
  uuid          VARCHAR(64)  NULL COMMENT '预录入编号',
  customs_status VARCHAR(2)  NULL DEFAULT '00' COMMENT '海关状态 00-未申报 10-待采集 20-已采集 90-采集超时 21-申报中 2Y-入库成功 2N-入库失败 22-通过 23-转人工 24-退单 29-已作废',
  execute_type CHAR(1) NOT NULL DEFAULT '1' COMMENT '执行策略 1-备案 2-变更 3-作废',
  create_by VARCHAR(64) DEFAULT '', create_time DATETIME DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT '', update_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL, del_flag CHAR(1) DEFAULT '0',
  UNIQUE KEY uk_biz (material_cd, version, calc_unit),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料主数据';

-- 委托方档案已移除（非海关单证）：单据上的关联企业名称(rela_ent_nm)由用户直接录入

CREATE TABLE br_work_order (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  work_no       VARCHAR(60) NOT NULL COMMENT '工单号(RRD2/MSO2.workNo，由发料/收料单录入自动建档)',
  batch_no      VARCHAR(60) NULL COMMENT '申报批次号,如 B202611-001',
  service_fee   DECIMAL(19,2) DEFAULT 0 COMMENT '维修服务费总额',
  currency      VARCHAR(30) NULL COMMENT '服务费币制',
  verify_cycle  VARCHAR(60) NULL COMMENT '核销周期(合同号/期限)',
  order_status  VARCHAR(2) DEFAULT '01' COMMENT '01-立项 02-维修中 03-已收齐 04-已出清 05-已结案',
  finish_date   DATETIME NULL COMMENT '计划完工日期',
  create_by VARCHAR(64) DEFAULT '', create_time DATETIME NULL,
  update_by VARCHAR(64) DEFAULT '', update_time DATETIME NULL,
  remark VARCHAR(500) NULL, del_flag CHAR(1) DEFAULT '0',
  UNIQUE KEY uk_work_no (work_no), PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维修工单';

CREATE TABLE br_work_order_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  work_order_id BIGINT UNSIGNED NOT NULL,
  material_id   BIGINT UNSIGNED NOT NULL COMMENT '待维修货物(br_material, mark_type=1)',
  plan_qty      DECIMAL(19,6) NOT NULL COMMENT '计划维修数量',
  PRIMARY KEY (id), KEY idx_wo (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单明细';

CREATE TABLE br_inbound (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  inbound_no    VARCHAR(60) NOT NULL COMMENT '入库单号(take_order_cd)',
  rela_ent_nm   VARCHAR(500) NULL COMMENT '关联企业名称',
  busi_type     VARCHAR(2)  NOT NULL COMMENT '04-待维修货物 05-维修用料',
  rela_customs_form_type VARCHAR(2) NOT NULL COMMENT '01/02/03/05/99',
  rela_customs_form_num  VARCHAR(60) NULL,
  in_out_time   DATETIME    NOT NULL COMMENT '入库时间',
  doc_status    VARCHAR(2)  NOT NULL DEFAULT '01' COMMENT '01-草稿 02-待审核 03-审核通过 04-已申报 05-申报通过 06-退单 07-已作废',
  customs_status VARCHAR(2) NULL DEFAULT '00',
  execute_type  CHAR(1)     NOT NULL DEFAULT '1' COMMENT '1-备案 2-变更 3-作废',
  last_uuid     VARCHAR(64) NULL COMMENT '最近预录入编号',
  create_by VARCHAR(64) DEFAULT '', create_time DATETIME NULL,
  update_by VARCHAR(64) DEFAULT '', update_time DATETIME NULL,
  remark VARCHAR(500) NULL, del_flag CHAR(1) DEFAULT '0',
  UNIQUE KEY uk_no (inbound_no), PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单';

CREATE TABLE br_inbound_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  inbound_id   BIGINT UNSIGNED NOT NULL,
  material_id  BIGINT UNSIGNED NOT NULL,
  storage_cnt  DECIMAL(19,6) NOT NULL COMMENT '入库数量',
  price        DECIMAL(19,6) NOT NULL COMMENT '单价',
  currency     VARCHAR(30) NULL,
  unit1_num    DECIMAL(19,5) NULL, unit2_num DECIMAL(19,5) NULL,
  brand_type   VARCHAR(1) NULL, trade_mode_cd VARCHAR(4) NULL,
  arriving_country_code VARCHAR(3) NULL, end_country_code VARCHAR(3) NULL,
  specifications_models VARCHAR(255) NULL,
  goods_source_type VARCHAR(1) NULL COMMENT 'g/i/h/空',
  goods_source_no   VARCHAR(60) NULL,
  busi_desc    VARCHAR(256) NULL, busi_form_num VARCHAR(60) NULL,
  PRIMARY KEY (id), KEY idx_in (inbound_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单明细';

CREATE TABLE br_issue (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  issue_no     VARCHAR(60) NOT NULL COMMENT '发料单号(finish_material_num)',
  work_order_id BIGINT UNSIGNED NOT NULL,
  issue_time   DATETIME NOT NULL COMMENT '发料日期(finish_material_tm)',
  doc_status   VARCHAR(2) NOT NULL DEFAULT '01',
  customs_status VARCHAR(2) NULL DEFAULT '00',
  execute_type CHAR(1)    NOT NULL DEFAULT '1',
  last_uuid    VARCHAR(64) NULL,
  create_by VARCHAR(64) DEFAULT '', create_time DATETIME NULL,
  update_by VARCHAR(64) DEFAULT '', update_time DATETIME NULL,
  remark VARCHAR(500) NULL, del_flag CHAR(1) DEFAULT '0',
  UNIQUE KEY uk_no (issue_no), PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单发料单';

CREATE TABLE br_issue_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  issue_id     BIGINT UNSIGNED NOT NULL,
  material_id  BIGINT UNSIGNED NOT NULL,
  goods_source_type VARCHAR(1) NOT NULL COMMENT 'g-入库清单 i-工单收料',
  goods_source_no   VARCHAR(60) NOT NULL,
  issue_qty    DECIMAL(19,6) NOT NULL COMMENT '发料数量',
  price        DECIMAL(19,6) NULL,
  PRIMARY KEY (id), KEY idx_issue (issue_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发料单明细';

CREATE TABLE br_receive (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  receive_no   VARCHAR(36) NOT NULL COMMENT '收料单号(return_material_no)',
  work_order_id BIGINT UNSIGNED NOT NULL,
  receive_time DATETIME NOT NULL COMMENT '收料日期(return_time)',
  doc_status   VARCHAR(2) NOT NULL DEFAULT '01',
  customs_status VARCHAR(2) NULL DEFAULT '00',
  execute_type CHAR(1)    NOT NULL DEFAULT '1',
  last_uuid    VARCHAR(64) NULL,
  create_by VARCHAR(64) DEFAULT '', create_time DATETIME NULL,
  update_by VARCHAR(64) DEFAULT '', update_time DATETIME NULL,
  remark VARCHAR(500) NULL, del_flag CHAR(1) DEFAULT '0',
  UNIQUE KEY uk_no (receive_no), PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单收料单';

CREATE TABLE br_receive_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  receive_id   BIGINT UNSIGNED NOT NULL,
  material_id  BIGINT UNSIGNED NOT NULL COMMENT 'returnType=8 时为服务费虚物料',
  material_num DECIMAL(19,6) NOT NULL COMMENT '收料数量(服务费行固定0.1)',
  return_type  CHAR(1) NOT NULL COMMENT '1/5/6/7/8/9/a',
  PRIMARY KEY (id), KEY idx_rcv (receive_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收料单明细';

CREATE TABLE br_outbound (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  outbound_no   VARCHAR(60) NOT NULL COMMENT '出库单号(releaseno)',
  rela_ent_nm   VARCHAR(500) NULL,
  busi_type     VARCHAR(2) NOT NULL COMMENT '03-销毁 04-复运出境 05-复运回境内区外 99-其他',
  busi_form_num VARCHAR(60) NOT NULL COMMENT '出库业务单证编号',
  rela_customs_form_type VARCHAR(2) NOT NULL COMMENT '03/04/05/06/99',
  rela_customs_form_num  VARCHAR(60) NULL,
  calculating_record_no  VARCHAR(60) NULL,
  batch_no      VARCHAR(60) NULL COMMENT '申报批次号',
  in_out_time   DATETIME NOT NULL,
  doc_status    VARCHAR(2) NOT NULL DEFAULT '01',
  customs_status VARCHAR(2) NULL DEFAULT '00',
  execute_type  CHAR(1)    NOT NULL DEFAULT '1',
  last_uuid     VARCHAR(64) NULL,
  create_by VARCHAR(64) DEFAULT '', create_time DATETIME NULL,
  update_by VARCHAR(64) DEFAULT '', update_time DATETIME NULL,
  remark VARCHAR(500) NULL, del_flag CHAR(1) DEFAULT '0',
  UNIQUE KEY uk_no (outbound_no), PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单';

CREATE TABLE br_outbound_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  outbound_id  BIGINT UNSIGNED NOT NULL,
  material_id  BIGINT UNSIGNED NOT NULL,
  delivery_cnt DECIMAL(19,6) NOT NULL COMMENT '出库数量(负值=出库退回)',
  price        DECIMAL(19,6) NOT NULL,
  currency     VARCHAR(30) NULL,
  specifications_models VARCHAR(255) NULL,
  goods_source_type VARCHAR(1) NULL COMMENT 'g/i',
  goods_source_no   VARCHAR(60) NULL,
  busi_desc VARCHAR(256) NULL COMMENT '出库说明(busiType=99 必填)',
  PRIMARY KEY (id), KEY idx_out (outbound_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单明细';

CREATE TABLE br_stock_ledger (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  material_id  BIGINT UNSIGNED NOT NULL,
  mark_type    CHAR(1) NOT NULL COMMENT '货物类型(六类标记,冗余)',
  biz_type     VARCHAR(6) NOT NULL COMMENT 'IN-入库 ISSUE-发料 RCV-收料 OUT-出库',
  biz_table    VARCHAR(32) NOT NULL, biz_id BIGINT UNSIGNED NOT NULL,
  biz_no       VARCHAR(60) NULL COMMENT '单据编号(物料编号/入库单号/发料单号/收料单号/出库单号)',
  goods_source_type VARCHAR(1) NULL COMMENT '来源单证类型 g/i',
  goods_source_no   VARCHAR(60) NULL COMMENT '来源单证编号(入库单号/发料单号/收料单号/出库单号)',
  work_no      VARCHAR(60) NULL, batch_no VARCHAR(60) NULL,
  qty          DECIMAL(19,6) NOT NULL COMMENT '方向数量: 收+ 发- 出-',
  balance      DECIMAL(19,6) NOT NULL COMMENT '变动后余额',
  create_time DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_mat (material_id, batch_no),
  KEY idx_source (material_id, goods_source_type, goods_source_no),
  KEY idx_biz (biz_table, biz_id),
  KEY idx_no (biz_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存台账流水';

-- 物料库存余额（唯一库存事实，物料为唯一维度；流水仅作审计）
CREATE TABLE br_material_stock (
  material_id   BIGINT UNSIGNED NOT NULL COMMENT '物料id(br_material.id)',
  mark_type     CHAR(1) NOT NULL COMMENT '货物类型(冗余,汇总用)',
  qty           DECIMAL(19,6) NOT NULL DEFAULT 0 COMMENT '当前结存',
  last_biz_time DATETIME NULL COMMENT '最近变动时间',
  update_time   DATETIME NULL,
  PRIMARY KEY (material_id),
  KEY idx_mark (mark_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料库存余额';

CREATE TABLE br_declare_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  biz_type     VARCHAR(10) NOT NULL COMMENT 'EPD101/PTO2/RRD2/MSO2/SDO2',
  biz_table    VARCHAR(32) NOT NULL COMMENT '业务表名',
  biz_id       BIGINT UNSIGNED NOT NULL COMMENT '业务行id(表头)',
  mirror_table VARCHAR(32) NOT NULL, mirror_row_id BIGINT UNSIGNED NOT NULL,
  execute_type CHAR(1) NOT NULL,
  uuid         VARCHAR(64) NULL,
  result_status VARCHAR(16) NULL COMMENT 'Y/N/1/2/3',
  msg          VARCHAR(512) NULL, result_time DATETIME NULL,
  sync_flag    VARCHAR(1) DEFAULT '0' COMMENT '0-已写镜像待回执 1-已回执',
  create_time DATETIME NULL,
  PRIMARY KEY (id), KEY idx_biz (biz_table, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申报日志';

CREATE TABLE br_customs_dict (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dict_type   VARCHAR(20) NOT NULL COMMENT 'jldw-计量单位 bz-币制 gbdq-国别 myfs-监管方式 zmfs-征免方式',
  dict_code   VARCHAR(30) NOT NULL, dict_name VARCHAR(200) NOT NULL,
  enabled     CHAR(1) DEFAULT '1',
  PRIMARY KEY (id), UNIQUE KEY uk (dict_type, dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='海关通关字典';

-- ---------- 种子数据 ----------
-- 计量单位(规范 §5.1 常用值)
INSERT INTO br_customs_dict (dict_type, dict_code, dict_name) VALUES
('jldw','001','台'),('jldw','004','个'),('jldw','006','套'),('jldw','007','米'),
('jldw','011','件'),('jldw','017','块'),('jldw','020','片'),('jldw','030','平方米'),
('jldw','035','千克'),('jldw','070','吨'),('jldw','140','盒');
-- 币制(通关参数常用值)
INSERT INTO br_customs_dict (dict_type, dict_code, dict_name) VALUES
('bz','142','人民币'),('bz','502','美元'),('bz','300','欧元'),('bz','116','日元'),('bz','110','港币');
-- 征免方式
INSERT INTO br_customs_dict (dict_type, dict_code, dict_name) VALUES
('zmfs','1','照章征税'),('zmfs','2','折半征税'),('zmfs','3','全免'),('zmfs','4','特案'),('zmfs','5','征免性质'),('zmfs','6','保证金'),('zmfs','7','保函'),('zmfs','8','折半补税'),('zmfs','9','全额退税');
-- 监管方式(保税维修常用)
INSERT INTO br_customs_dict (dict_type, dict_code, dict_name) VALUES
('myfs','0130','易货贸易'),('myfs','0214','来料加工'),('myfs','0615','进料对口'),('myfs','1371','保税维修'),('myfs','9600','其他');
