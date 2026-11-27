package com.ruoyi.bonded.mapper;

import com.ruoyi.bonded.domain.Pto2Data;

/**
 * PTO2 镜像表 Mapper（系统只 INSERT / 读回执）
 */
public interface Pto2DataMapper
{
    public int insert(Pto2Data data);

    public Pto2Data selectById(Long id);
}
