-- ============================================================
-- 初始化收尾：系统参数种子 + 基础数据清理
-- 适用：按 br_schema.sql + br_m1/m2/m3_menu.sql 初始化的新库
-- 执行后请重新登录刷新菜单缓存（或清空 Redis cache:sys_menu）
-- ============================================================

-- 1. 业务系统参数（可在 参数设置 中修改）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '联网自动推送开关', 'br.auto.push.enabled', 'true', 'Y', 'admin', sysdate(), 'true=单据审核/物料保存自动写入镜像库；false=仅人工变更/作废/补推写入'
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

-- 2. 移除若依自带的外链菜单（menu_id=4，源自 ry 基础数据）
DELETE FROM sys_role_menu WHERE menu_id = 4;
DELETE FROM sys_menu WHERE menu_id = 4 AND menu_name = '若依官网';
