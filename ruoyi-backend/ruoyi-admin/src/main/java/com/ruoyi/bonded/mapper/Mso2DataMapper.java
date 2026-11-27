package com.ruoyi.bonded.mapper;

import com.ruoyi.bonded.domain.Mso2Data;

/**
 * MSO2 镜像表 Mapper（系统只 INSERT / 读回执）
 */
public interface Mso2DataMapper
{
    public int insert(Mso2Data data);

    public Mso2Data selectById(Long id);
}
