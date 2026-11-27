alter table br_material add column execute_type char(1) not null default '1' comment '执行策略 1-备案 2-变更 3-作废' after customs_status;
