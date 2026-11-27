package com.ruoyi.bonded.mapper;

import com.ruoyi.bonded.domain.Epd101Data;

/**
 * EPD101 镜像表 Mapper（系统只 INSERT / 读回执）
 */
public interface Epd101DataMapper
{
    /** 插入镜像行并回填自增 id */
    public int insertEpd101Data(Epd101Data data);

    public Epd101Data selectEpd101DataById(Long id);
}
