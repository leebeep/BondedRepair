package com.ruoyi.bonded.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 维修工单 br_work_order
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BrWorkOrder extends BaseEntity
{
    private Long id;
    /** 工单号(RRD2/MSO2.workNo) */
    private String workNo;
    /** 申报批次号 */
    private String batchNo;
    /** 维修服务费总额 */
    private BigDecimal serviceFee;
    private String currency;
    /** 核销周期 */
    private String verifyCycle;
    /** 01-立项 02-维修中 03-已收齐 04-已出清 05-已结案 */
    private String orderStatus;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date finishDate;
    private String delFlag;
    /** 工单明细 */
    private List<BrWorkOrderItem> items;
    /** 已发料合计（markType=1，列表展示用） */
    private BigDecimal issuedQty;
    /** 已收回合计（returnType 5/a，列表展示用） */
    private BigDecimal receivedQty;
}
