-- M4 菜单与权限：维修工单（工单先行立项，发料/收料单选择关联）
-- （自本版本起恢复独立工单菜单：工单在工单页创建，发料/收料表单改为下拉选择）
-- 管理员角色(role_id=1)拥有全部权限，无需 sys_role_menu
-- 执行后请重新登录刷新菜单缓存（或清空 Redis cache:sys_menu）

-- 菜单位置：入库管理(order_num=2)之后，其余菜单顺延
UPDATE sys_menu SET order_num = order_num + 1 WHERE parent_id = 2000 AND order_num >= 3 AND order_num < 90 AND path <> 'workorder';

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '维修工单', 2000, 3, 'workorder', 'bonded/workorder/index', '', '', 1, 0, 'C', '0', '0', 'bonded:wo:list', 'checkbox', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE component = 'bonded/workorder/index');
SET @m := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '工单查询', @m, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:wo:query', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'bonded:wo:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '工单新增', @m, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:wo:add', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'bonded:wo:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '工单修改', @m, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:wo:edit', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'bonded:wo:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '工单删除', @m, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:wo:remove', '#', 'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'bonded:wo:remove');

-- 工单号生成前缀参数（前缀+yyyyMMdd+3位流水）
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '工单号生成前缀', 'br.workorder.no.prefix', 'WO', 'Y', 'admin', sysdate(), '新增维修工单默认单号前缀（前缀+yyyyMMdd+3位当日流水），可在 参数设置 中修改；留空则用默认 WO'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'br.workorder.no.prefix');
