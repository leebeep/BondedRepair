-- M2 测试数据清理（重置业务与镜像表）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE br_customer; TRUNCATE br_work_order; TRUNCATE br_work_order_item;
TRUNCATE br_inbound; TRUNCATE br_inbound_item;
TRUNCATE br_issue; TRUNCATE br_issue_item;
TRUNCATE br_receive; TRUNCATE br_receive_item;
TRUNCATE br_stock_ledger; TRUNCATE br_declare_log;
TRUNCATE br_material; TRUNCATE epd101_data; TRUNCATE pto2_data; TRUNCATE rrd2_data; TRUNCATE mso2_data;
SET FOREIGN_KEY_CHECKS = 1;
