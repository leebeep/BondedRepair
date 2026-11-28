package com.ruoyi.bonded.task;

import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.bonded.domain.BrDeclareLog;
import com.ruoyi.bonded.domain.Epd101Data;
import com.ruoyi.bonded.domain.Mso2Data;
import com.ruoyi.bonded.domain.Pto2Data;
import com.ruoyi.bonded.domain.Rrd2Data;
import com.ruoyi.bonded.domain.Sdo2Data;
import com.ruoyi.bonded.mapper.BrDeclareLogMapper;
import com.ruoyi.bonded.mapper.BrInboundMapper;
import com.ruoyi.bonded.mapper.BrIssueMapper;
import com.ruoyi.bonded.mapper.BrMaterialMapper;
import com.ruoyi.bonded.mapper.BrOutboundMapper;
import com.ruoyi.bonded.mapper.BrReceiveMapper;
import com.ruoyi.bonded.mapper.Epd101DataMapper;
import com.ruoyi.bonded.mapper.Mso2DataMapper;
import com.ruoyi.bonded.mapper.Pto2DataMapper;
import com.ruoyi.bonded.mapper.Rrd2DataMapper;
import com.ruoyi.bonded.mapper.Sdo2DataMapper;
import com.ruoyi.common.utils.StringUtils;

/**
 * 海关回执轮询任务（兜底通道）
 * 1) 扫描 br_declare_log sync_flag=0，回读镜像表回执字段，回写业务单据状态；
 * 2) 待采集(customs_status=10)超 24 小时无回执 → EPD101 置 90 采集超时。
 */
@Component
public class BrReceiptPollTask
{
    private static final Logger log = LoggerFactory.getLogger(BrReceiptPollTask.class);

    @Autowired private BrDeclareLogMapper declareLogMapper;
    @Autowired private Epd101DataMapper epd101DataMapper;
    @Autowired private Pto2DataMapper pto2DataMapper;
    @Autowired private Rrd2DataMapper rrd2DataMapper;
    @Autowired private Mso2DataMapper mso2DataMapper;
    @Autowired private Sdo2DataMapper sdo2DataMapper;
    @Autowired private BrOutboundMapper outboundMapper;
    @Autowired private BrMaterialMapper materialMapper;
    @Autowired private BrInboundMapper inboundMapper;
    @Autowired private BrIssueMapper issueMapper;
    @Autowired private BrReceiveMapper receiveMapper;
    @Autowired private com.ruoyi.bonded.service.BrLedgerService ledgerService;
    @Autowired private com.ruoyi.bonded.service.BrWorkOrderService workOrderService;
    @Autowired private com.ruoyi.bonded.mapper.BrWorkOrderMapper workOrderMapper;

    /** 默认 60 秒一轮，可用 br.poll.intervalMs 配置 */
    @Scheduled(fixedDelayString = "${br.poll.intervalMs:60000}")
    public void poll()
    {
        List<BrDeclareLog> logs = declareLogMapper.selectPendingLogs(200);
        for (BrDeclareLog decl : logs)
        {
            try
            {
                receiveReceipt(decl);
            }
            catch (Exception e)
            {
                log.error("回执处理失败 logId={}", decl.getId(), e);
            }
        }
        materialMapper.markCollectTimeout(24);
    }

    @Transactional(rollbackFor = Exception.class)
    public void receiveReceipt(BrDeclareLog decl)
    {
        String bizType = decl.getBizType();
        String resultStatus = null, uuid = null, msg = null;
        Date resultTime = null;

        if ("EPD101".equals(bizType))
        {
            Epd101Data row = epd101DataMapper.selectEpd101DataById(decl.getMirrorRowId());
            if (row == null || StringUtils.isEmpty(row.getResultStatus())) { return; }
            resultStatus = row.getResultStatus(); uuid = row.getUuid(); msg = row.getMsg(); resultTime = row.getResultTime();
            if ("3".equals(decl.getExecuteType()))
            {
                // 作废回执：成功→物料回到未申报(0)/海关29已作废；失败→保持通过(2)，海关状态取回执码
                boolean ok = "Y".equals(resultStatus) || "1".equals(resultStatus);
                materialMapper.updateDeclareResult(decl.getBizId(), uuid,
                        ok ? "0" : "2", ok ? "29" : mapCustomsStatus(resultStatus), decl.getExecuteType());
            }
            else
            {
                materialMapper.updateDeclareResult(decl.getBizId(), uuid, mapDeclareStatus(resultStatus),
                        mapCustomsStatus(resultStatus), decl.getExecuteType());
            }
        }
        else if ("PTO2".equals(bizType))
        {
            Pto2Data row = pto2DataMapper.selectById(decl.getMirrorRowId());
            if (row == null || StringUtils.isEmpty(row.getResultStatus())) { return; }
            resultStatus = row.getResultStatus(); uuid = row.getUuid(); msg = row.getMsg(); resultTime = row.getResultTime();
            String ds = docStatusFor(resultStatus, decl.getExecuteType());
            if (ds != null)
            {
                inboundMapper.updateDeclareResult(decl.getBizId(), uuid, ds,
                        customsFor(resultStatus, decl.getExecuteType()), decl.getExecuteType());
                if ("07".equals(ds)) { ledgerService.reverseDoc("br_inbound", decl.getBizId()); }
            }
        }
        else if ("RRD2".equals(bizType))
        {
            Rrd2Data row = rrd2DataMapper.selectById(decl.getMirrorRowId());
            if (row == null || StringUtils.isEmpty(row.getResultStatus())) { return; }
            resultStatus = row.getResultStatus(); uuid = row.getUuid(); msg = row.getMsg(); resultTime = row.getResultTime();
            String ds = docStatusFor(resultStatus, decl.getExecuteType());
            if (ds != null)
            {
                issueMapper.updateDeclareResult(decl.getBizId(), uuid, ds,
                        customsFor(resultStatus, decl.getExecuteType()), decl.getExecuteType());
                if ("07".equals(ds))
                {
                    ledgerService.reverseDoc("br_issue", decl.getBizId());
                    com.ruoyi.bonded.domain.BrIssue ih = issueMapper.selectById(decl.getBizId());
                    if (ih != null && ih.getWorkOrderId() != null) { workOrderService.refreshWorkOrderStatus(ih.getWorkOrderId()); }
                }
            }
        }
        else if ("MSO2".equals(bizType))
        {
            Mso2Data row = mso2DataMapper.selectById(decl.getMirrorRowId());
            if (row == null || StringUtils.isEmpty(row.getResultStatus())) { return; }
            resultStatus = row.getResultStatus(); uuid = row.getUuid(); msg = row.getMsg(); resultTime = row.getResultTime();
            String ds = docStatusFor(resultStatus, decl.getExecuteType());
            if (ds != null)
            {
                receiveMapper.updateDeclareResult(decl.getBizId(), uuid, ds,
                        customsFor(resultStatus, decl.getExecuteType()), decl.getExecuteType());
                if ("07".equals(ds))
                {
                    ledgerService.reverseDoc("br_receive", decl.getBizId());
                    com.ruoyi.bonded.domain.BrReceive rh = receiveMapper.selectById(decl.getBizId());
                    if (rh != null && rh.getWorkOrderId() != null) { workOrderService.refreshWorkOrderStatus(rh.getWorkOrderId()); }
                }
            }
        }
        else if ("SDO2".equals(bizType))
        {
            Sdo2Data row = sdo2DataMapper.selectById(decl.getMirrorRowId());
            if (row == null || StringUtils.isEmpty(row.getResultStatus())) { return; }
            resultStatus = row.getResultStatus(); uuid = row.getUuid(); msg = row.getMsg(); resultTime = row.getResultTime();
            String ds = docStatusFor(resultStatus, decl.getExecuteType());
            if (ds != null)
            {
                outboundMapper.updateDeclareResult(decl.getBizId(), uuid, ds,
                        customsFor(resultStatus, decl.getExecuteType()), decl.getExecuteType());
                if ("07".equals(ds))
                {
                    ledgerService.reverseDoc("br_outbound", decl.getBizId());
                    java.util.Set<Long> woIds = new java.util.HashSet<>();
                    for (com.ruoyi.bonded.domain.BrOutboundItem item : outboundMapper.selectItemsByOutboundId(decl.getBizId()))
                    {
                        if ("i".equals(item.getGoodsSourceType()) && StringUtils.isNotEmpty(item.getGoodsSourceNo()))
                        {
                            Long woId = workOrderMapper.selectWorkOrderIdByReceiveNo(item.getGoodsSourceNo());
                            if (woId != null) { woIds.add(woId); }
                        }
                    }
                    for (Long woId : woIds) { workOrderService.refreshOutClearStatus(woId); }
                }
            }
        }
        else
        {
            log.warn("业务类型 {} 回执处理尚未接入，logId={}", bizType, decl.getId());
            return;
        }

        decl.setUuid(uuid);
        decl.setResultStatus(resultStatus);
        decl.setMsg(msg);
        decl.setResultTime(resultTime != null ? resultTime : new Date());
        declareLogMapper.updateReceipt(decl);
        log.info("{} 回执: bizId={} result={} customs={} msg={}", bizType, decl.getBizId(), resultStatus,
                mapCustomsStatus(resultStatus), msg);
    }

    /** manageResult → customs_status（PRD §4.2） */
    private String mapCustomsStatus(String result)
    {
        switch (result)
        {
            case "Y": return "2Y";
            case "N": return "2N";
            case "1": return "22";
            case "2": return "23";
            case "3": return "24";
            default: return "21";
        }
    }

    /** manageResult → EPD101 declare_status（0-未申报 1-已申报 2-通过 3-退单） */
    private String mapDeclareStatus(String result)
    {
        switch (result)
        {
            case "Y":
            case "1": return "2";
            case "N":
            case "3": return "3";
            default: return "1";
        }
    }

    /** 单据回执映射：作废回执成功→07 已作废；作废失败→06 退单（可改后重新审核申报）；其余走常规映射 */
    private String docStatusFor(String result, String execType)
    {
        if ("3".equals(execType))
        {
            return ("Y".equals(result) || "1".equals(result)) ? "07" : "06";
        }
        return mapDocStatus(result);
    }

    private String customsFor(String result, String execType)
    {
        if ("3".equals(execType))
        {
            return ("Y".equals(result) || "1".equals(result)) ? "29" : null;
        }
        return mapCustomsStatus(result);
    }

    /** manageResult → 单据 doc_status（Y/1→05 申报通过，N/3→06 退单，其余 03 申报中） */
    private String mapDocStatus(String result)
    {
        switch (result)
        {
            case "Y":
            case "1": return "05";
            case "N":
            case "3": return "06";
            default: return "03";
        }
    }
}
