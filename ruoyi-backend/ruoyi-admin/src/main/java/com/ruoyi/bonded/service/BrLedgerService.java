package com.ruoyi.bonded.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.bonded.domain.BrStockLedger;
import com.ruoyi.bonded.mapper.BrMaterialStockMapper;
import com.ruoyi.bonded.mapper.BrStockLedgerMapper;
import com.ruoyi.common.exception.ServiceException;

/**
 * 库存记账组件（库存以物料为唯一维度）：
 * 余额的唯一事实来源是 br_material_stock（每物料一行，唯一键行锁保证并发安全）；
 * br_stock_ledger 仅作流水/审计，balance 字段为记账后的物料结存。
 */
@Component
public class BrLedgerService
{
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Autowired
    private BrStockLedgerMapper ledgerMapper;

    @Autowired
    private BrMaterialStockMapper stockMapper;

    /**
     * 记一笔流水并原子更新物料库存。
     * 先 upsert 库存（唯一键行锁），再读回结存作为流水 balance；结存为负则抛错回滚（防负库存总闸）。
     */
    public void record(Long materialId, String markType, String bizType, String bizTable, Long bizId, String bizNo,
                       String goodsSourceType, String goodsSourceNo, String workNo, String batchNo, BigDecimal qty)
    {
        stockMapper.upsertQty(materialId, markType, qty);
        BigDecimal balance = stockMapper.selectQty(materialId);
        if (balance == null || balance.signum() < 0)
        {
            // 抛出后整个事务回滚，库存 upsert 一并撤销
            throw new ServiceException("物料库存不足：materialId=" + materialId + " 本次变动 " + qty + "，结存将为 " + balance);
        }
        BrStockLedger l = new BrStockLedger();
        l.setMaterialId(materialId);
        l.setMarkType(markType);
        l.setBizType(bizType);
        l.setBizTable(bizTable);
        l.setBizId(bizId);
        l.setBizNo(bizNo);
        l.setGoodsSourceType(goodsSourceType);
        l.setGoodsSourceNo(goodsSourceNo);
        l.setWorkNo(workNo);
        l.setBatchNo(batchNo);
        l.setQty(qty);
        l.setBalance(balance);
        ledgerMapper.insert(l);
    }

    /** 物料当前结存（库存余额表，O(1)） */
    public BigDecimal selectBalance(Long materialId)
    {
        BigDecimal qty = stockMapper.selectQty(materialId);
        return qty == null ? ZERO : qty;
    }

    /** 来源单据剩余可用量（IN/RCV - ISSUE） */
    public BigDecimal sourceBalance(Long materialId, String goodsSourceType, String goodsSourceNo)
    {
        return ledgerMapper.selectSourceBalance(materialId, goodsSourceType, goodsSourceNo);
    }

    /**
     * 冲销指定单据已记账的全部流水（各笔数量取反，bizNo 加后缀 R 标识）。
     * 用于出库退单(06)重审：先冲销原扣账，再按新明细记账，避免重复扣减。
     */
    public void reverseDoc(String bizTable, Long bizId)
    {
        List<BrStockLedger> rows = ledgerMapper.selectByBiz(bizTable, bizId);
        for (BrStockLedger r : rows)
        {
            record(r.getMaterialId(), r.getMarkType(), r.getBizType(), bizTable, bizId,
                    r.getBizNo() + "-R", r.getGoodsSourceType(), r.getGoodsSourceNo(),
                    r.getWorkNo(), r.getBatchNo(), r.getQty().negate());
        }
    }
}
