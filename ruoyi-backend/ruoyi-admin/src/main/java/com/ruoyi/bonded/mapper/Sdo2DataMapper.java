package com.ruoyi.bonded.mapper;

import com.ruoyi.bonded.domain.Sdo2Data;

/**
 * SDO2 镜像表 Mapper（系统只 INSERT / 读回执）
 */
public interface Sdo2DataMapper
{
    public int insert(Sdo2Data data);

    public Sdo2Data selectById(Long id);
}
