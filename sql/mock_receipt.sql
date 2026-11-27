-- 模拟加工增值客户端将海关回执写回镜像表
update epd101_data set uuid = 'EPD101-C4600AA0001-0001', result_status = '1', msg = '审批通过', result_time = now(), over_time = 0 where id = 1;
