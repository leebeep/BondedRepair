-- 模拟客户端回执：EPD101 全部通过
update epd101_data set uuid = concat('EPD101-TEST-', id), result_status = '1', msg = 'OK', result_time = now(), over_time = 0 where result_status is null;
