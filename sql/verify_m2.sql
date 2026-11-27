select 'PTO2' t, count(*) rows_cnt from pto2_data union all
select 'RRD2', count(*) from rrd2_data union all
select 'MSO2', count(*) from mso2_data;
select material_cd, material_desc, storage_cnt, busi_type, goods_source_no, result_status, uuid from pto2_data;
select finish_material_num, work_no, goods_source_no, material_desc, issue_material_qty, result_status from rrd2_data;
select return_material_no, work_no, material_cd, material_desc, material_num, return_type, result_status from mso2_data;
select doc_status, customs_status, last_uuid from br_inbound;
select doc_status, customs_status from br_issue;
select doc_status, customs_status from br_receive;
select order_status from br_work_order where work_no='WO-2026-001';
select material_id, biz_type, goods_source_no, qty, balance from br_stock_ledger order by id;
