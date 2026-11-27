package com.ruoyi.bonded.domain;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 出库单明细 br_outbound_item
 */
@Data
public class BrOutboundItem
{
    private Long id;
    private Long outboundId;
    private Long materialId;
    private String materialName;
    private BigDecimal deliveryCnt;
    private BigDecimal price;
    private String currency;
    private String specificationsModels;
    /** g-入库清单 i-工单收料 */
    private String goodsSourceType;
    private String goodsSourceNo;
    /** 出库说明(busiType=99 必填) */
    private String busiDesc;
}
