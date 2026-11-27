package com.ruoyi.bonded.mapper;

import com.ruoyi.bonded.domain.Rrd2Data;

/**
 * RRD2 镜像表 Mapper（系统只 INSERT / 读回执）
 */
public interface Rrd2DataMapper
{
    public int insert(Rrd2Data data);

    public Rrd2Data selectById(Long id);
}
