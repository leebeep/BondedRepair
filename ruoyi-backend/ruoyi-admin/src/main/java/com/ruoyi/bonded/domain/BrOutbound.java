package com.ruoyi.bonded.domain;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 出库单 br_outbound（SDO2）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BrOutbound extends BaseEntity
{
    private Long id;
    /** 出库单号(releaseno) */
    private String outboundNo;
    private String relaEntNm;
    /** 03-销毁 04-复运出境 05-复运回境内区外 99-其他 */
    private String busiType;
    private String busiFormNum;
    private String relaCustomsFormType;
    private String relaCustomsFormNum;
    private String calculatingRecordNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date inOutTime;
    private String docStatus;
    private String customsStatus;
    private String executeType;
    private String lastUuid;
    private String delFlag;
    /** 查询辅助：业务类型/单据状态/海关状态多选（列表头筛选） */
    private List<String> busiTypeList;
    private List<String> docStatusList;
    private List<String> customsStatusList;
    private List<BrOutboundItem> items;
}
