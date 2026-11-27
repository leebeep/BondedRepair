package com.ruoyi.bonded.mapper;

import java.util.List;
import java.util.Map;

/**
 * 联网同步扫描 Mapper：待补推单据、采集状态看板、主动查询扫描
 */
public interface BrSyncMapper
{
    /** 待同步单据 id（doc_status=03 且 customs_status=00，开关关闭期间滞留的） */
    public List<Long> selectPendingInboundIds();

    public List<Long> selectPendingIssueIds();

    public List<Long> selectPendingReceiveIds();

    public List<Long> selectPendingOutboundIds();

    /** 待同步物料 id（declare_status=0 且 customs_status=00，排除服务费虚物料） */
    public List<Long> selectPendingMaterialIds();

    /** 采集状态看板：五类报文按 customs_status 计数 */
    public List<Map<String, Object>> selectSyncBoard();

    /** 主动查询扫描：已采集/申报中(20/21) 超 30 分钟无回执的 uuid */
    public List<String> selectQueryUuidsInbound();

    public List<String> selectQueryUuidsIssue();

    public List<String> selectQueryUuidsReceive();

    public List<String> selectQueryUuidsOutbound();

    public List<String> selectQueryUuidsMaterial();
}
