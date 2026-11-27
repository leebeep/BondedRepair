select id, execute_type, dcl_typecd, enterp_cd, master_cuscd, int_product_no, int_product_name, version, g_type, code_ts, calc_unit, is_bonded, uuid, result_status from epd101_data\G
select id, biz_type, biz_table, biz_id, mirror_table, mirror_row_id, execute_type, sync_flag from br_declare_log\G
