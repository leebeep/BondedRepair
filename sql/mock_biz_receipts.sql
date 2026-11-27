-- 模拟客户端回执：PTO2/RRD2/MSO2 全部通过
update pto2_data set uuid = concat('PTO2-TEST-', id), result_status = '1', msg = 'OK', result_time = now(), over_time = 0 where result_status is null;
update rrd2_data set uuid = concat('RRD2-TEST-', id), result_status = '1', msg = 'OK', result_time = now(), over_time = 0 where result_status is null;
update mso2_data set uuid = concat('MSO2-TEST-', id), result_status = '1', msg = 'OK', result_time = now(), over_time = 0 where result_status is null;
