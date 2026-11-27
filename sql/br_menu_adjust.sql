-- ============================================================
-- 菜单调整：目录改名置顶 + 移除若依官网 + 补充收料/出库图标
-- 适用：已按 br_m1/m2/m3_menu.sql 及 ry_20261117.sql 初始化的库
-- 执行后请重新登录刷新菜单缓存（或清空 Redis cache:sys_menu）
-- ============================================================

-- 1. 目录「保税维修工作台」→「维修管理」，并置顶（默认目录 order_num 1/2/3/4）
UPDATE sys_menu
SET menu_name = '维修管理', order_num = 0, remark = '保税维修业务目录（顶级，排第一）'
WHERE parent_id = 0 AND path = 'bonded' AND menu_type = 'M';

-- 1.1 菜单「物料备案」→「物料清单」
UPDATE sys_menu SET menu_name = '物料清单' WHERE parent_id = 2000 AND path = 'material' AND menu_type = 'C';

-- 2. 移除「若依官网」外链菜单（menu_id=4，源自 ry_20261117.sql）
DELETE FROM sys_role_menu WHERE menu_id = 4;
DELETE FROM sys_menu WHERE menu_id = 4 AND menu_name = '若依官网';

-- 3. 补充图标：工单收料 → goods-in（收料进箱），出库管理 → goods-out（出库）
--    （两个新图标已随前端源码提交：ruoyi-frontend/src/assets/icons/svg/goods-in.svg、goods-out.svg）
UPDATE sys_menu SET icon = 'goods-in' WHERE parent_id = 2000 AND path = 'receive' AND menu_type = 'C';
UPDATE sys_menu SET icon = 'goods-out' WHERE parent_id = 2000 AND path = 'outbound' AND menu_type = 'C';

-- 校验
SELECT menu_id, menu_name, order_num, path, icon FROM sys_menu
WHERE parent_id = 0 OR parent_id = 2000
ORDER BY parent_id, order_num;
