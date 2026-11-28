-- ============================================================
-- 初始化收尾：系统参数种子 + 基础数据清理
-- 适用：按 br_schema.sql + br_m1/m2/m3_menu.sql 初始化的新库
-- 执行后请重新登录刷新菜单缓存（或清空 Redis cache:sys_menu）
-- ============================================================

-- 1. 业务系统参数（可在 参数设置 中修改；按本脚本顺序展示，勿在中间插入新参数）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '联网自动申报开关', 'br.auto.push.enabled', 'true', 'Y', 'admin', sysdate(), 'true=单据审核/物料保存自动申报（写入镜像库）；false=仅人工变更/作废/补报写入'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'br.auto.push.enabled');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '物料编号自动生成', 'br.material.auto.no', 'true', 'Y', 'admin', sysdate(), 'true=新增物料未填编号时自动生成（WL+日期+4位流水）；false=编号必填手动录入'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'br.material.auto.no');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '收料单号生成前缀', 'br.receive.no.prefix', 'RC', 'Y', 'admin', sysdate(), '新增收料单默认单号前缀（前缀+yyyyMMdd+3位当日流水），可在 参数设置 中修改；留空则用默认 RC'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'br.receive.no.prefix');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '出库单号生成前缀', 'br.outbound.no.prefix', 'OUT', 'Y', 'admin', sysdate(), '新增出库单默认单号前缀（前缀+yyyyMMdd+3位当日流水），可在 参数设置 中修改；留空则用默认 OUT'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'br.outbound.no.prefix');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '入库单号生成前缀', 'br.inbound.no.prefix', 'IN', 'Y', 'admin', sysdate(), '新增入库单默认单号前缀（前缀+yyyyMMdd+3位当日流水），可在 参数设置 中修改；留空则用默认 IN'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'br.inbound.no.prefix');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '发料单号生成前缀', 'br.issue.no.prefix', 'IS', 'Y', 'admin', sysdate(), '新增发料单默认单号前缀（前缀+yyyyMMdd+3位当日流水），可在 参数设置 中修改；留空则用默认 IS'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'br.issue.no.prefix');

-- 企业三项资料（EPD101 料件备案镜像行必填）：以下为演示值，上线前必须在 参数设置 中替换为海关备案真实值
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '企业编号', 'br.enterpCd', '4600AA0001', 'Y', 'admin', sysdate(), '企业海关编号（EPD101 enterp_cd 必填），上线前替换为真实备案编号'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'br.enterpCd');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '企业名称', 'br.enterpNm', '演示保税维修企业', 'Y', 'admin', sysdate(), '企业名称（EPD101 enterp_nm 必填，最长500字符），上线前替换为真实注册名称'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'br.enterpNm');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '主管关区代码', 'br.masterCuscd', '6404', 'Y', 'admin', sysdate(), '主管关区代码（EPD101 master_cuscd 必填）：6404-洋浦经开区 6411-海口高新区'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'br.masterCuscd');

-- 存量库参数键值列加宽（500→4000），否则存不下 RSA 私钥；新库由 ry_20261117.sql 建表即为 4000
ALTER TABLE sys_config MODIFY config_value varchar(4000) DEFAULT '' COMMENT '参数键值';

-- 公服主动查询接口（规范 §2.3.2）：url/appKey/私钥三项都配置后，申报管理页"立即主动查询"自动启用
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '公服查询接口地址', 'br.query.api.url', 'https://psp.singlewindow.hn.cn/gongfu/api/secure/router', 'Y', 'admin', sysdate(), '公服主动查询接口地址（规范 §2.3.2，固定值），未配置时主动查询任务自动跳过'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'br.query.api.url');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '公服查询 appKey', 'br.query.api.appKey', '', 'Y', 'admin', sysdate(), '公服平台企业注册后分配的 x-app-key，与接口地址都配置后主动查询按钮自动启用'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'br.query.api.appKey');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '公服查询 RSA 私钥', 'br.query.api.privateKey', '', 'Y', 'admin', sysdate(), 'PKCS#8 私钥：去掉 -----BEGIN/END----- 行、换行合并为一行后粘贴；application.yml 同名键为兜底'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'br.query.api.privateKey');

-- 2. 移除若依自带的外链菜单（menu_id=4，源自 ry 基础数据）
DELETE FROM sys_role_menu WHERE menu_id = 4;
DELETE FROM sys_menu WHERE menu_id = 4 AND menu_name = '若依官网';
