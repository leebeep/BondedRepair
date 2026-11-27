-- ============================================================
-- 清理失效菜单：维修工单（component bonded/workorder/index 前端页面已移除）
-- 工单号/批次号改由发料单、收料单表单录入自动建档（BrWorkOrderService），
-- 独立"维修工单"菜单不再需要。
-- 委托方档案菜单已由 remove_internal_master.sql 清理，此处无需处理。
-- 执行后请重新登录刷新菜单缓存（或清空 Redis cache:sys_menu）。
-- ============================================================

-- 删除维修工单按钮权限（F 类型，挂在工单菜单下）
DELETE FROM sys_role_menu
WHERE menu_id IN (SELECT menu_id FROM sys_menu WHERE perms LIKE 'bonded:wo:%');

DELETE FROM sys_menu WHERE perms LIKE 'bonded:wo:%' AND menu_type = 'F';

-- 删除维修工单页面菜单（C 类型）
DELETE FROM sys_role_menu
WHERE menu_id IN (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE perms = 'bonded:wo:list' AND menu_type = 'C') t);

DELETE FROM sys_menu WHERE perms = 'bonded:wo:list' AND menu_type = 'C';

-- 校验：应返回 0 行
SELECT menu_id, menu_name, path, perms FROM sys_menu WHERE perms LIKE 'bonded:wo:%' OR path = 'workorder';
