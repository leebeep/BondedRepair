package com.ruoyi.bonded.domain;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 入库单 br_inbound（PTO2）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BrInbound extends BaseEntity
{
    private Long id;
    /** 入库单号(take_order_cd) */
    private String inboundNo;
    private String relaEntNm;
    /** 04-待维修货物 05-维修用料 */
    private String busiType;
    private String relaCustomsFormType;
    private String relaCustomsFormNum;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date inOutTime;
    /** 01-草稿 02-待审核 03-审核通过 04-已申报 05-申报通过 06-退单 07-已作废 */
    private String docStatus;
    private String customsStatus;
    private String executeType;
    private String lastUuid;
    private String delFlag;
    /** 查询辅助：业务类型/关联单证类型/单据状态/海关状态多选（列表头筛选） */
    private List<String> busiTypeList;
    private List<String> relaCustomsFormTypeList;
    private List<String> docStatusList;
    private List<String> customsStatusList;
    private List<BrInboundItem> items;
}
