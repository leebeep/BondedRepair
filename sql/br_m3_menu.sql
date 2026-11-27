-- M3 菜单与权限：出库管理 / 申报管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('出库管理', 2000, 7, 'outbound', 'bonded/outbound/index', '', '', 1, 0, 'C', '0', '0', 'bonded:out:list', 'goods-out', 'admin', sysdate());
SET @m := LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('出库查询', @m, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:out:query', '#', 'admin', sysdate()),
('出库新增', @m, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:out:add', '#', 'admin', sysdate()),
('出库修改', @m, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:out:edit', '#', 'admin', sysdate()),
('出库删除', @m, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:out:remove', '#', 'admin', sysdate()),
('出库审核', @m, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:out:audit', '#', 'admin', sysdate()),
('出库申报', @m, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:out:declare', '#', 'admin', sysdate());

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('联网日志', 2000, 8, 'declare', 'bonded/declare/index', '', '', 1, 0, 'C', '0', '0', 'bonded:declare:list', 'email', 'admin', sysdate());

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('联网参数配置', @dir, 1, '', NULL, '', '', 1, 0, 'F', '0', '0', 'bonded:declare:edit', '#', 'admin', sysdate(), '自动推送开关/补推/主动查询');
