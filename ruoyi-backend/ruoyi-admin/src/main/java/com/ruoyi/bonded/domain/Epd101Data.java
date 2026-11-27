package com.ruoyi.bonded.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * EPD101 料件成品备案镜像行 epd101_data
 * 字段与《企业客户端数据采集传输规范 v1.4》§3.1 一致；回执字段由客户端写回，系统只读。
 */
public class Epd101Data
{
    private Long id;
    private String executeType;
    private String dclTypecd;
    private String enterpCd;
    private String enterpNm;
    private String masterCuscd;
    private String intProductNo;
    private String intProductName;
    private String version;
    @JsonProperty("gType")
    private String gType;
    private String codeTs;
    @JsonProperty("gName")
    private String gName;
    @JsonProperty("gModel")
    private String gModel;
    private String calcUnit;
    private String unit1;
    private String unit2;
    private String dutyMode;
    private String totalGrossWt;
    private String totalNetWt;
    private String note;
    private String isBonded;
    /** 以下为客户端写回的回执字段 */
    private String uuid;
    private String resultStatus;
    private String msg;
    private Date resultTime;
    private Integer overTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getExecuteType() { return executeType; }
    public void setExecuteType(String executeType) { this.executeType = executeType; }
    public String getDclTypecd() { return dclTypecd; }
    public void setDclTypecd(String dclTypecd) { this.dclTypecd = dclTypecd; }
    public String getEnterpCd() { return enterpCd; }
    public void setEnterpCd(String enterpCd) { this.enterpCd = enterpCd; }
    public String getEnterpNm() { return enterpNm; }
    public void setEnterpNm(String enterpNm) { this.enterpNm = enterpNm; }
    public String getMasterCuscd() { return masterCuscd; }
    public void setMasterCuscd(String masterCuscd) { this.masterCuscd = masterCuscd; }
    public String getIntProductNo() { return intProductNo; }
    public void setIntProductNo(String intProductNo) { this.intProductNo = intProductNo; }
    public String getIntProductName() { return intProductName; }
    public void setIntProductName(String intProductName) { this.intProductName = intProductName; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getgType() { return gType; }
    public void setgType(String gType) { this.gType = gType; }
    public String getCodeTs() { return codeTs; }
    public void setCodeTs(String codeTs) { this.codeTs = codeTs; }
    public String getgName() { return gName; }
    public void setgName(String gName) { this.gName = gName; }
    public String getgModel() { return gModel; }
    public void setgModel(String gModel) { this.gModel = gModel; }
    public String getCalcUnit() { return calcUnit; }
    public void setCalcUnit(String calcUnit) { this.calcUnit = calcUnit; }
    public String getUnit1() { return unit1; }
    public void setUnit1(String unit1) { this.unit1 = unit1; }
    public String getUnit2() { return unit2; }
    public void setUnit2(String unit2) { this.unit2 = unit2; }
    public String getDutyMode() { return dutyMode; }
    public void setDutyMode(String dutyMode) { this.dutyMode = dutyMode; }
    public String getTotalGrossWt() { return totalGrossWt; }
    public void setTotalGrossWt(String totalGrossWt) { this.totalGrossWt = totalGrossWt; }
    public String getTotalNetWt() { return totalNetWt; }
    public void setTotalNetWt(String totalNetWt) { this.totalNetWt = totalNetWt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getIsBonded() { return isBonded; }
    public void setIsBonded(String isBonded) { this.isBonded = isBonded; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public Date getResultTime() { return resultTime; }
    public void setResultTime(Date resultTime) { this.resultTime = resultTime; }
    public Integer getOverTime() { return overTime; }
    public void setOverTime(Integer overTime) { this.overTime = overTime; }
}
