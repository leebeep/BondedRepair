-- 移除非海关单证的内部档案：委托方（表/菜单）、维修工单（菜单，数据表保留作内部支撑）
-- 工单号/批次号/服务费改由发料单、收料单表单录入，按工单号自动建档（BrWorkOrderService）

ALTER TABLE br_work_order DROP COLUMN customer_id;
DROP TABLE IF EXISTS br_customer;

-- 删除菜单（含按钮权限）
DELETE FROM sys_menu WHERE perms LIKE 'bonded:customer:%';
DELETE FROM sys_menu WHERE menu_name = '委托方档案';
DELETE FROM sys_menu WHERE perms LIKE 'bonded:wo:%';
DELETE FROM sys_menu WHERE menu_name = '维修工单' AND menu_type = 'C';

-- 菜单排序：物料清单 → 入库 → 发料 → 收料 → 出库 → 库存台账 → 申报管理
UPDATE sys_menu SET order_num = 2 WHERE parent_id = 2000 AND menu_name = '入库管理';
UPDATE sys_menu SET order_num = 3 WHERE parent_id = 2000 AND menu_name = '工单发料';
UPDATE sys_menu SET order_num = 4 WHERE parent_id = 2000 AND menu_name = '工单收料';
UPDATE sys_menu SET order_num = 5 WHERE parent_id = 2000 AND menu_name = '出库管理';
UPDATE sys_menu SET order_num = 6 WHERE parent_id = 2000 AND menu_name = '库存台账';
UPDATE sys_menu SET order_num = 7 WHERE parent_id = 2000 AND menu_name = '申报管理';
