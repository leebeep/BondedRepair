package com.ruoyi.bonded.domain;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 发料单明细 br_issue_item
 */
@Data
public class BrIssueItem
{
    private Long id;
    private Long issueId;
    private Long materialId;
    private String materialName;
    /** 以下为关联物料展示字段 */
    private String materialCd;
    private String version;
    private String calcUnit;
    private String isBonded;
    private String gModel;
    private String markType;
    /** g-入库清单 i-工单收料 */
    private String goodsSourceType;
    private String goodsSourceNo;
    private BigDecimal issueQty;
    private BigDecimal price;
}
