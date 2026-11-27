package com.ruoyi.bonded.domain;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 收料单明细 br_receive_item
 */
@Data
public class BrReceiveItem
{
    private Long id;
    private Long receiveId;
    private Long materialId;
    private String materialName;
    /** 收料数量(服务费行固定0.1) */
    private BigDecimal materialNum;
    /** 1-未使用料件 5-已维修货物 6-维修用料件 7-坏损零部件 8-维修服务费 9-边角料 a-残次品 */
    private String returnType;
}
