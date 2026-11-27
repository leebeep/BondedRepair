package com.ruoyi.bonded.service;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.bonded.domain.BrStockLedger;
import com.ruoyi.bonded.mapper.BrStockLedgerMapper;

/**
 * 库存台账登记组件（余额 = 上笔余额 + 本次变动）
 */
@Component
public class BrLedgerService
{
    @Autowired
    private BrStockLedgerMapper ledgerMapper;

    public void record(Long materialId, String markType, String bizType, String bizTable, Long bizId,
                       String goodsSourceType, String goodsSourceNo, String workNo, String batchNo, BigDecimal qty)
    {
        BigDecimal balance = selectBalance(materialId).add(qty);
        BrStockLedger l = new BrStockLedger();
        l.setMaterialId(materialId);
        l.setMarkType(markType);
        l.setBizType(bizType);
        l.setBizTable(bizTable);
        l.setBizId(bizId);
        l.setGoodsSourceType(goodsSourceType);
        l.setGoodsSourceNo(goodsSourceNo);
        l.setWorkNo(workNo);
        l.setBatchNo(batchNo);
        l.setQty(qty);
        l.setBalance(balance);
        ledgerMapper.insert(l);
    }

    public BigDecimal selectBalance(Long materialId)
    {
        return ledgerMapper.selectBalance(materialId);
    }

    /** 来源单据剩余可用量（IN/RCV - ISSUE） */
    public BigDecimal sourceBalance(Long materialId, String goodsSourceType, String goodsSourceNo)
    {
        return ledgerMapper.selectSourceBalance(materialId, goodsSourceType, goodsSourceNo);
    }
}
