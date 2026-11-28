package com.ruoyi.bonded.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.ruoyi.common.annotation.Excel;
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
    /** 查询用：物料编号 */
    private String materialCd;
    /** 联查：保税标识 0-非保税 1-保税 */
    @Excel(name = "是否保税")
    private String isBonded;
    /** 联查：规格型号 */
    @Excel(name = "规格型号")
    private String gModel;
    /** 联查：HS编码 */
    @Excel(name = "HS编码")
    private String codeTs;
    /** 计算：在库时间（首次入库距今天数，保留1位小数） */
    @Excel(name = "在库时间(天)")
    private BigDecimal stockDays;
    private String markType;
    /** IN-入库 ISSUE-发料 RCV-收料 OUT-出库 */
    private String bizType;
    private String bizTable;
    private Long bizId;
    /** 单据编号(入库/发料/收料/出库单号) */
    @Excel(name = "单据编号")
    private String bizNo;
    /** 来源单证类型 g/i */
    private String goodsSourceType;
    /** 来源单证编号(入库单号/收料单号) */
    @Excel(name = "来源单证编号")
    private String goodsSourceNo;
    @Excel(name = "工单号")
    private String workNo;
    @Excel(name = "申报批次")
    private String batchNo;
    @Excel(name = "变动数量")
    private BigDecimal qty;
    @Excel(name = "变动后结存")
    private BigDecimal balance;
    @Excel(name = "变动时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 查询用：时间范围 */
    private Date beginTime;
    private Date endTime;

    /** 查询辅助：物料属性/保税标志多选（列表头筛选） */
    private List<String> markTypeList;
    private List<String> isBondedList;

    private String batchNoQuery;
}
