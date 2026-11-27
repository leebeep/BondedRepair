package com.ruoyi.bonded.domain;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 工单明细 br_work_order_item
 */
@Data
public class BrWorkOrderItem
{
    private Long id;
    private Long workOrderId;
    /** 待维修货物(br_material, markType=1) */
    private Long materialId;
    /** 物料名称(关联查询) */
    private String materialName;
    private BigDecimal planQty;
}
