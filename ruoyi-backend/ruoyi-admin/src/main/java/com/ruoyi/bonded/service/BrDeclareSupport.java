package com.ruoyi.bonded.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.bonded.domain.BrDeclareLog;
import com.ruoyi.bonded.mapper.BrDeclareLogMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/**
 * 申报数据支撑服务。
 * "未采集可修改"：单据海关状态为 00 未申报 / 10 待采集（客户端尚未采集推送海关）时，
 * 允许直接修改单据；本服务负责清理该单据尚未回执的镜像行与待回执日志，
 * 修改保存后由审核流程重新写镜像，客户端采集到的即为修正后的数据。
 * 已出现回执（部分或全部）则禁止，走变更/作废申报流程。
 */
@Service
public class BrDeclareSupport
{
    /** 允许按日志记录清理的镜像表白名单（防注入，日志内值理论可控仍校验） */
    private static final Set<String> MIRROR_TABLES = new HashSet<>(Arrays.asList(
            "epd101_data", "pto2_data", "rrd2_data", "mso2_data", "sdo2_data"));

    @Autowired
    private BrDeclareLogMapper declareLogMapper;

    /**
     * 清理单据未回执的镜像行与申报日志。
     * @return 清理的镜像行数
     * @throws ServiceException 任一日志已回执（已被采集）时抛出
     */
    public int purgePending(String bizTable, Long bizId, String docNo)
    {
        List<BrDeclareLog> logs = declareLogMapper.selectLogsByBiz(bizTable, bizId);
        int purged = 0;
        for (BrDeclareLog log : logs)
        {
            if ("1".equals(log.getSyncFlag()) || StringUtils.isNotEmpty(log.getResultStatus()))
            {
                throw new ServiceException("单据【" + docNo + "】的申报数据已被采集/回执，不可直接修改，请走变更申报流程");
            }
            if (!MIRROR_TABLES.contains(log.getMirrorTable()))
            {
                throw new ServiceException("单据【" + docNo + "】申报日志的镜像表异常：" + log.getMirrorTable());
            }
            declareLogMapper.deleteMirrorRow(log.getMirrorTable(), log.getMirrorRowId());
            declareLogMapper.deleteLogById(log.getId());
            purged++;
        }
        return purged;
    }
}
