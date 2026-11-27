package com.ruoyi.bonded.domain;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * RRD2 工单发料镜像行 rrd2_data（规范 v1.4 §3.3）
 */
@Data
public class Rrd2Data
{
    private Long id;
    private String executeType;
    private String finishMaterialNum;
    private Date finishMaterialTm;
    private String workNo;
    private String goodsSourceType;
    private String goodsSourceNo;
    private BigDecimal price;
    private String materialCd;
    private String materialDesc;
    private Integer materialVersion;
    private BigDecimal issueMaterialQty;
    private String calcUnit;
    private String isBonded;
    /** 回执字段 */
    private String uuid;
    private String resultStatus;
    private String msg;
    private Date resultTime;
    private Integer overTime;
}
