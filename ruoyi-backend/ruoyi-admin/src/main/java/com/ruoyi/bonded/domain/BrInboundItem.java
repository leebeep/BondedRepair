package com.ruoyi.bonded.domain;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 入库单明细 br_inbound_item
 */
@Data
public class BrInboundItem
{
    private Long id;
    private Long inboundId;
    private Long materialId;
    /** 物料名称(关联查询) */
    private String materialName;
    private BigDecimal storageCnt;
    private BigDecimal price;
    private String currency;
    private BigDecimal unit1Num;
    private BigDecimal unit2Num;
    private String brandType;
    private String tradeModeCd;
    private String arrivingCountryCode;
    private String endCountryCode;
    private String specificationsModels;
    /** g-入库清单 i-工单收料 h-出库清单 空-国内采购 */
    private String goodsSourceType;
    private String goodsSourceNo;
    private String busiDesc;
    private String busiFormNum;
}
