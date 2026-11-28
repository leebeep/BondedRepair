package com.ruoyi.bonded.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.bonded.domain.BrMaterialStock;

/**
 * 物料库存余额 Mapper
 */
public interface BrMaterialStockMapper
{
    /** 原子记账：无行则插入，有行则累加（唯一键行锁保证并发安全） */
    public int upsertQty(@Param("materialId") Long materialId, @Param("markType") String markType,
            @Param("qty") BigDecimal qty);

    /** 记账后取该物料当前结存（同事务内已持有行锁） */
    public BigDecimal selectQty(Long materialId);

    /** 单个物料库存（联查物料基本信息，详情页用） */
    public BrMaterialStock selectByMaterialId(@Param("materialId") Long materialId);

    /** 库存结存汇总（每物料一行，联查物料基本信息，列表页用） */
    public List<BrMaterialStock> selectStockSummary(BrMaterialStock query);

    /** 本月四向流量（台账聚合） */
    public List<java.util.Map<String, Object>> selectMonthStats(@Param("materialId") Long materialId,
            @Param("monthBegin") String monthBegin);
}
