package com.ruoyi.bonded.domain;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单收料单 br_receive（MSO2）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BrReceive extends BaseEntity
{
    private Long id;
    /** 收料单号(return_material_no) */
    private String receiveNo;
    private Long workOrderId;
    /** 工单号(表单录入，自动建档；列表为关联查询) */
    private String workNo;
    /** 维修服务费总额(表单录入，写入工单) */
    private java.math.BigDecimal serviceFee;
    /** 服务费币制(表单录入，写入工单) */
    private String currency;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date receiveTime;
    private String docStatus;
    private String customsStatus;
    private String executeType;
    private String lastUuid;
    private String delFlag;
    private List<BrReceiveItem> items;
}
