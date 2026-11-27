package com.ruoyi.bonded.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 物料主数据对象 br_material
 *
 * 六类标记 mark_type: 1-待修复 2-已修复 3-无法修复 4-维修用料 5-旧件坏件 6-边角料 8-维修服务费
 */
public class BrMaterial extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "物料编号")
    private String materialCd;

    @Excel(name = "版本号")
    private String version;

    @Excel(name = "计量单位")
    private String calcUnit;

    @Excel(name = "物料名称")
    private String materialName;

    /** 六类标记 1-待修复 2-已修复 3-无法修复 4-维修用料 5-旧件坏件 6-边角料 8-维修服务费 */
    @Excel(name = "六类标记")
    private String markType;

    /** I-料件 E-成品 S-半成品 */
    @JsonProperty("gType")
    @Excel(name = "料件成品标志")
    private String gType;

    /** 0-非保税 1-保税 */
    @Excel(name = "是否保税")
    private String isBonded;

    @Excel(name = "HS编码")
    private String codeTs;

    @JsonProperty("gName")
    @Excel(name = "HS商品名称")
    private String gName;

    @JsonProperty("gModel")
    @Excel(name = "规格型号")
    private String gModel;

    private String unit1;
    private String unit2;
    private String dutyMode;
    private String totalGrossWt;
    private String totalNetWt;

    /** 备案状态 0-未申报 1-已申报 2-通过 3-退单 */
    @Excel(name = "备案状态")
    private String declareStatus;

    /** 预录入编号 */
    private String uuid;

    /** 海关状态 customs_status */
    @Excel(name = "海关状态")
    private String customsStatus;

    /** 执行策略 1-备案 2-变更 3-作废 */
    private String executeType;

    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMaterialCd() { return materialCd; }
    public void setMaterialCd(String materialCd) { this.materialCd = materialCd; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getCalcUnit() { return calcUnit; }
    public void setCalcUnit(String calcUnit) { this.calcUnit = calcUnit; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getMarkType() { return markType; }
    public void setMarkType(String markType) { this.markType = markType; }
    public String getgType() { return gType; }
    public void setgType(String gType) { this.gType = gType; }
    public String getIsBonded() { return isBonded; }
    public void setIsBonded(String isBonded) { this.isBonded = isBonded; }
    public String getCodeTs() { return codeTs; }
    public void setCodeTs(String codeTs) { this.codeTs = codeTs; }
    public String getgName() { return gName; }
    public void setgName(String gName) { this.gName = gName; }
    public String getgModel() { return gModel; }
    public void setgModel(String gModel) { this.gModel = gModel; }
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
    public String getDeclareStatus() { return declareStatus; }
    public void setDeclareStatus(String declareStatus) { this.declareStatus = declareStatus; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getCustomsStatus() { return customsStatus; }
    public void setCustomsStatus(String customsStatus) { this.customsStatus = customsStatus; }
    public String getExecuteType() { return executeType; }
    public void setExecuteType(String executeType) { this.executeType = executeType; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
