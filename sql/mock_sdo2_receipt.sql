-- 模拟客户端回执：SDO2 通过（追加到 mock_biz_receipts 后执行）
update sdo2_data set uuid = concat('SDO2-TEST-', id), result_status = '1', msg = 'OK', result_time = now(), over_time = 0 where result_status is null;
