-- 规范符合性复查回归清理：业务单据 + 全部镜像表（保留已备案物料）
TRUNCATE br_work_order; TRUNCATE br_work_order_item;
TRUNCATE br_inbound; TRUNCATE br_inbound_item;
TRUNCATE br_issue; TRUNCATE br_issue_item;
TRUNCATE br_receive; TRUNCATE br_receive_item;
TRUNCATE br_outbound; TRUNCATE br_outbound_item;
TRUNCATE br_stock_ledger; TRUNCATE br_declare_log;
TRUNCATE pto2_data; TRUNCATE rrd2_data; TRUNCATE mso2_data; TRUNCATE sdo2_data;
delete from br_material where material_cd like '%-SVF';
