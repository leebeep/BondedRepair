package com.ruoyi.bonded.domain;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单发料单 br_issue（RRD2）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BrIssue extends BaseEntity
{
    private Long id;
    /** 发料单号(finish_material_num) */
    private String issueNo;
    private Long workOrderId;
    /** 工单号(表单录入，自动建档；列表为关联查询) */
    private String workNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date issueTime;
    private String docStatus;
    private String customsStatus;
    private String executeType;
    private String lastUuid;
    private String delFlag;
    private List<BrIssueItem> items;
}
