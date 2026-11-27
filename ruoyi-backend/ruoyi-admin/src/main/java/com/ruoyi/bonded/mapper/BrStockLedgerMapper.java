package com.ruoyi.bonded.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.bonded.domain.BrStockLedger;
import com.ruoyi.bonded.domain.StockReconcileRow;

/**
 * 库存台账 Mapper
 */
public interface BrStockLedgerMapper
{
    public int insert(BrStockLedger ledger);

    /** 物料当前余额（最后一条流水 balance，无则 0） */
    public BigDecimal selectBalance(@Param("materialId") Long materialId);

    public List<BrStockLedger> selectList(BrStockLedger query);

    /** 指定来源单据某物料的净流水（入库+收料-发料，用于余额校验） */
    public BigDecimal selectSourceBalance(@Param("materialId") Long materialId,
            @Param("goodsSourceType") String goodsSourceType, @Param("goodsSourceNo") String goodsSourceNo);

    /** 按货物类型（六类标记）聚合流水（对账公式四项），batchNo/workNo 可选 */
    public List<StockReconcileRow> selectMarkTypeSummary(@Param("batchNo") String batchNo, @Param("workNo") String workNo);
}
