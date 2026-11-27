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
    /** g-入库清单 i-工单收料 */
    private String goodsSourceType;
    private String goodsSourceNo;
    private BigDecimal issueQty;
    private BigDecimal price;
}
