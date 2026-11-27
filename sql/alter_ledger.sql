alter table br_stock_ledger
  add column goods_source_type varchar(1) null comment '来源单证类型 g/i' after biz_id,
  add column goods_source_no varchar(60) null comment '来源单证编号(入库单号/收料单号)' after goods_source_type;
