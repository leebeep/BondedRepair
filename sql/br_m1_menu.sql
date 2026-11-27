-- M1 菜单与权限：保税维修工作台 > 物料备案（EPD101）
-- 管理员角色(role_id=1)拥有全部权限，无需 sys_role_menu

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('保税维修工作台', 0, 5, 'bonded', NULL, '', '', 1, 0, 'M', '0', '0', '', 'shopping', 'admin', sysdate(), '保税维修业务目录');

SET @dir := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('物料备案', @dir, 1, 'material', 'bonded/material/index', '', '', 1, 0, 'C', '0', '0', 'bonded:material:list', 'form', 'admin', sysdate(), '料件成品备案 EPD101');

SET @menu := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES
('物料查询', @menu, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:material:query',  '#', 'admin', sysdate()),
('物料新增', @menu, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:material:add',    '#', 'admin', sysdate()),
('物料修改', @menu, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:material:edit',   '#', 'admin', sysdate()),
('物料删除', @menu, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:material:remove', '#', 'admin', sysdate()),
('物料导出', @menu, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:material:export', '#', 'admin', sysdate()),
('备案申报', @menu, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'bonded:material:declare','#', 'admin', sysdate());
