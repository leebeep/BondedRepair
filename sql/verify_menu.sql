select menu_id, menu_name, parent_id, menu_type, perms from sys_menu where perms like 'bonded%' or (parent_id = 0 and path = 'bonded');
