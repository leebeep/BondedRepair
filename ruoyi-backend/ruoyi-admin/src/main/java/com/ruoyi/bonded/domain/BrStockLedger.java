package com.ruoyi.bonded.domain;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 库存台账流水 br_stock_ledger（qty: 收+ 发- 出-；balance 变动后余额）
 */
@Data
public class BrStockLedger
{
    private Long id;
    private Long materialId;
    private String materialName;
    private String markType;
    /** IN-入库 ISSUE-发料 RCV-收料 OUT-出库 */
    private String bizType;
    private String bizTable;
    private Long bizId;
    /** 来源单证类型 g/i */
    private String goodsSourceType;
    /** 来源单证编号(入库单号/收料单号) */
    private String goodsSourceNo;
    private String workNo;
    private String batchNo;
    private BigDecimal qty;
    private BigDecimal balance;
    private Date createTime;

    private String batchNoQuery;
}
