package com.ruoyi.bonded.domain;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * MSO2 工单收料镜像行 mso2_data（规范 v1.4 §3.4；materialVersion 列为 decimal(28,6) 规范脚本口径）
 */
@Data
public class Mso2Data
{
    private Long id;
    private String executeType;
    private String returnMaterialNo;
    private Date returnTime;
    private String workNo;
    private String materialCd;
    private String materialDesc;
    private BigDecimal materialVersion;
    private BigDecimal materialNum;
    private String calcUnit;
    private String isBonded;
    private String returnType;
    /** 回执字段 */
    private String uuid;
    private String resultStatus;
    private String msg;
    private Date resultTime;
    private Integer overTime;
}
