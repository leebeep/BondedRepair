INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('委托方档案', 2000, 1, 'customer', 'bonded/customer/index', '', '', 1, 0, 'C', '0', '0', 'bonded:customer:list', 'people', 'admin', sysdate());
SET @m := LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('委托方查询', @m, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:customer:query', '#', 'admin', sysdate()),
('委托方新增', @m, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:customer:add', '#', 'admin', sysdate()),
('委托方修改', @m, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:customer:edit', '#', 'admin', sysdate()),
('委托方删除', @m, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:customer:remove', '#', 'admin', sysdate());
