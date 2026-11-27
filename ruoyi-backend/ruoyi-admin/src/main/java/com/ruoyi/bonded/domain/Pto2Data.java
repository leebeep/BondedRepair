package com.ruoyi.bonded.domain;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * PTO2 入库清单镜像行 pto2_data（规范 v1.4 §3.2；回执字段客户端写回）
 */
@Data
public class Pto2Data
{
    private Long id;
    private String executeType;
    private String takeOrderCd;
    private String relaEntNm;
    private String materialCd;
    private String materialDesc;
    private Integer materialVersion;
    private String specificationsModels;
    private BigDecimal storageCnt;
    private BigDecimal price;
    private String currency;
    private String isBonded;
    private String calcUnit;
    private Date inOutTime;
    private String busiType;
    /** 入库说明（规范脚本字段名 busid_esc） */
    private String busiDesc;
    private String busiFormNum;
    private String relaCustomsFormType;
    private String relaCustomsFormNum;
    private String goodsSourceType;
    private String goodsSourceNo;
    private String brandType;
    private String tradeModeCd;
    private BigDecimal unit1Num;
    private BigDecimal unit2Num;
    private String arrivingCountryCode;
    private String endCountryCode;
    /** 回执字段 */
    private String uuid;
    private String resultStatus;
    private String msg;
    private Date resultTime;
    private Integer overTime;
}
