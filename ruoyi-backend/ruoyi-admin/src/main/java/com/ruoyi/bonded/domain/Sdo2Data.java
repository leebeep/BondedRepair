package com.ruoyi.bonded.domain;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * SDO2 出库清单镜像行 sdo2_data（规范 v1.4 §3.5）
 */
@Data
public class Sdo2Data
{
    private Long id;
    private String executeType;
    private String relaEntNm;
    private String releaseno;
    private String materialCd;
    private String materialDesc;
    private Integer version;
    private String specificationsModels;
    private BigDecimal deliveryCnt;
    private BigDecimal price;
    private String calcUnit;
    private String currency;
    private String isBonded;
    private Date inOutTime;
    private String busiType;
    private String busiDesc;
    private String busiFormNum;
    private String relaCustomsFormType;
    private String relaCustomsFormNum;
    private String calculatingRecordNo;
    private String goodsSourceType;
    private String goodsSourceNo;
    /** 回执字段 */
    private String uuid;
    private String resultStatus;
    private String msg;
    private Date resultTime;
    private Integer overTime;
}
