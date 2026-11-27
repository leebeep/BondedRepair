-- 库存管理菜单调整：
-- 1) 原"库存台账"菜单改名"库存管理"（页面内含 物料库存/库存清单/库存对账 三个页签）
-- 2) 新增隐藏菜单"物料库存详情"（详情独立页面，点击列表"单据清单"进入）
-- 幂等性：详情菜单按 component 判断是否已存在

UPDATE sys_menu SET menu_name = '库存管理'
WHERE component = 'bonded/stock/index' AND menu_name = '库存台账';

-- 隐藏详情页菜单（visible='1' 不显示在侧边栏，但路由可达）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '物料库存详情', m.parent_id, 99, 'stockDetail', 'bonded/stock/detail', '', '', 1, 0, 'C', '1', '0', 'bonded:stock:list', '#', 'admin', sysdate()
FROM sys_menu m
WHERE m.component = 'bonded/stock/index'
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.component = 'bonded/stock/detail');
