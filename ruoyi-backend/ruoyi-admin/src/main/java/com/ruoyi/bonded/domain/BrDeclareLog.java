package com.ruoyi.bonded.domain;

import java.util.Date;
import java.util.List;

/**
 * 申报日志 br_declare_log（业务单据 ↔ 镜像表行映射）
 */
public class BrDeclareLog
{
    private Long id;
    /** EPD101/PTO2/RRD2/MSO2/SDO2 */
    private String bizType;
    private String bizTable;
    private Long bizId;
    /** 单据编号（物料编号/入库单号/发料单号/收料单号/出库单号），日志清单展示用 */
    private String bizNo;
    private String mirrorTable;
    private Long mirrorRowId;
    private String executeType;
    private String uuid;
    private String resultStatus;
    private String msg;
    private Date resultTime;
    /** 0-已写镜像待回执 1-已回执 */
    private String syncFlag;
    private Date createTime;

    /** 查询辅助：申报时间范围（不入库） */
    private String beginTime;
    private String endTime;

    /** 查询辅助：报文类型/执行类型/回执状态多选（列表头筛选） */
    private List<String> bizTypeList;
    private List<String> executeTypeList;
    private List<String> resultStatusList;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getBizTable() { return bizTable; }
    public void setBizTable(String bizTable) { this.bizTable = bizTable; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public String getMirrorTable() { return mirrorTable; }
    public void setMirrorTable(String mirrorTable) { this.mirrorTable = mirrorTable; }
    public Long getMirrorRowId() { return mirrorRowId; }
    public void setMirrorRowId(Long mirrorRowId) { this.mirrorRowId = mirrorRowId; }
    public String getExecuteType() { return executeType; }
    public void setExecuteType(String executeType) { this.executeType = executeType; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public Date getResultTime() { return resultTime; }
    public void setResultTime(Date resultTime) { this.resultTime = resultTime; }
    public String getSyncFlag() { return syncFlag; }
    public void setSyncFlag(String syncFlag) { this.syncFlag = syncFlag; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getBeginTime() { return beginTime; }
    public void setBeginTime(String beginTime) { this.beginTime = beginTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public List<String> getBizTypeList() { return bizTypeList; }
    public void setBizTypeList(List<String> bizTypeList) { this.bizTypeList = bizTypeList; }
    public List<String> getExecuteTypeList() { return executeTypeList; }
    public void setExecuteTypeList(List<String> executeTypeList) { this.executeTypeList = executeTypeList; }
    public List<String> getResultStatusList() { return resultStatusList; }
    public void setResultStatusList(List<String> resultStatusList) { this.resultStatusList = resultStatusList; }
}
