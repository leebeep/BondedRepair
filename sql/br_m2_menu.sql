-- M2 菜单与权限：维修工单/入库管理/工单发料/工单收料/库存台账
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('维修工单', 2000, 2, 'workorder', 'bonded/workorder/index', '', '', 1, 0, 'C', '0', '0', 'bonded:wo:list', 'documentation', 'admin', sysdate());
SET @m := LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('工单查询', @m, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:wo:query', '#', 'admin', sysdate()),
('工单新增', @m, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:wo:add', '#', 'admin', sysdate()),
('工单修改', @m, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:wo:edit', '#', 'admin', sysdate()),
('工单删除', @m, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:wo:remove', '#', 'admin', sysdate());

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('入库管理', 2000, 3, 'inbound', 'bonded/inbound/index', '', '', 1, 0, 'C', '0', '0', 'bonded:in:list', 'upload', 'admin', sysdate());
SET @m := LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('入库查询', @m, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:in:query', '#', 'admin', sysdate()),
('入库新增', @m, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:in:add', '#', 'admin', sysdate()),
('入库修改', @m, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:in:edit', '#', 'admin', sysdate()),
('入库删除', @m, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:in:remove', '#', 'admin', sysdate()),
('入库审核', @m, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:in:audit', '#', 'admin', sysdate()),
('入库申报', @m, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:in:declare', '#', 'admin', sysdate());

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('工单发料', 2000, 4, 'issue', 'bonded/issue/index', '', '', 1, 0, 'C', '0', '0', 'bonded:issue:list', 'component', 'admin', sysdate());
SET @m := LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('发料查询', @m, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:issue:query', '#', 'admin', sysdate()),
('发料新增', @m, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:issue:add', '#', 'admin', sysdate()),
('发料修改', @m, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:issue:edit', '#', 'admin', sysdate()),
('发料删除', @m, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:issue:remove', '#', 'admin', sysdate()),
('发料审核', @m, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:issue:audit', '#', 'admin', sysdate()),
('发料申报', @m, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:issue:declare', '#', 'admin', sysdate());

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('工单收料', 2000, 5, 'receive', 'bonded/receive/index', '', '', 1, 0, 'C', '0', '0', 'bonded:rcv:list', 'goods-in', 'admin', sysdate());
SET @m := LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('收料查询', @m, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:rcv:query', '#', 'admin', sysdate()),
('收料新增', @m, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:rcv:add', '#', 'admin', sysdate()),
('收料修改', @m, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:rcv:edit', '#', 'admin', sysdate()),
('收料删除', @m, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:rcv:remove', '#', 'admin', sysdate()),
('收料审核', @m, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:rcv:audit', '#', 'admin', sysdate()),
('收料申报', @m, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:rcv:declare', '#', 'admin', sysdate());

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('库存台账', 2000, 6, 'stock', 'bonded/stock/index', '', '', 1, 0, 'C', '0', '0', 'bonded:stock:list', 'chart', 'admin', sysdate());
