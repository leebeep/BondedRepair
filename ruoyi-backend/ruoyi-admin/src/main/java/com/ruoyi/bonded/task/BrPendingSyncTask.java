package com.ruoyi.bonded.task;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.ruoyi.bonded.mapper.BrSyncMapper;
import com.ruoyi.bonded.service.BrPushSwitch;
import com.ruoyi.bonded.service.IBrInboundService;
import com.ruoyi.bonded.service.IBrIssueService;
import com.ruoyi.bonded.service.IBrMaterialService;
import com.ruoyi.bonded.service.IBrOutboundService;
import com.ruoyi.bonded.service.IBrReceiveService;
import com.ruoyi.common.exception.ServiceException;

/**
 * 待同步补推任务：开关重新打开后，自动批量补推滞留单据（doc_status=03/customs_status=00 与物料 0/00），保证不丢。
 */
@Component
public class BrPendingSyncTask
{
    private static final Logger log = LoggerFactory.getLogger(BrPendingSyncTask.class);

    @Autowired private BrPushSwitch pushSwitch;
    @Autowired private BrSyncMapper syncMapper;
    @Autowired private IBrInboundService inboundService;
    @Autowired private IBrIssueService issueService;
    @Autowired private IBrReceiveService receiveService;
    @Autowired private IBrOutboundService outboundService;
    @Autowired private IBrMaterialService materialService;

    @Scheduled(fixedDelayString = "${br.sync.intervalMs:60000}")
    public void scheduled()
    {
        pushPending();
    }

    /** 可由联网日志模块手动触发；返回本次补推单据数 */
    public int pushPending()
    {
        if (!pushSwitch.isEnabled())
        {
            return 0;
        }
        int n = 0;
        n += pushDocs("PTO2", syncMapper.selectPendingInboundIds());
        n += pushDocs("RRD2", syncMapper.selectPendingIssueIds());
        n += pushDocs("MSO2", syncMapper.selectPendingReceiveIds());
        n += pushDocs("SDO2", syncMapper.selectPendingOutboundIds());
        n += pushMaterials();
        if (n > 0)
        {
            log.info("补推完成，共 {} 单", n);
        }
        return n;
    }

    private int pushDocs(String bizType, List<Long> ids)
    {
        if (ids == null || ids.isEmpty())
        {
            return 0;
        }
        try
        {
            Long[] arr = ids.toArray(new Long[0]);
            switch (bizType)
            {
                case "PTO2": inboundService.declare(arr, "A"); break;
                case "RRD2": issueService.declare(arr, "A"); break;
                case "MSO2": receiveService.declare(arr, "A"); break;
                case "SDO2": outboundService.declare(arr, "A"); break;
                default: break;
            }
            log.info("{} 补推 {} 单", bizType, ids.size());
            return ids.size();
        }
        catch (Exception e)
        {
            log.warn("{} 批量补推失败，改为逐单重试: {}", bizType, e.getMessage());
            int ok = 0;
            for (Long id : ids)
            {
                try
                {
                    Long[] one = { id };
                    switch (bizType)
                    {
                        case "PTO2": inboundService.declare(one, "A"); break;
                        case "RRD2": issueService.declare(one, "A"); break;
                        case "MSO2": receiveService.declare(one, "A"); break;
                        case "SDO2": outboundService.declare(one, "A"); break;
                        default: break;
                    }
                    ok++;
                }
                catch (Exception ex)
                {
                    log.warn("{} 单 {} 补推失败: {}", bizType, id, ex.getMessage());
                }
            }
            return ok;
        }
    }

    private int pushMaterials()
    {
        List<Long> ids = syncMapper.selectPendingMaterialIds();
        if (ids == null || ids.isEmpty())
        {
            return 0;
        }
        int ok = 0;
        for (Long id : ids)
        {
            try
            {
                Long[] one = { id };
                materialService.declareEPD101(one, "1");
                ok++;
            }
            catch (ServiceException e)
            {
                // 校验类失败（如数据不全）保持滞留，人工修正后自动重试
                log.warn("物料 {} 备案补推失败: {}", id, e.getMessage());
            }
            catch (Exception e)
            {
                log.error("物料 {} 备案补推异常", id, e);
            }
        }
        if (ok > 0)
        {
            log.info("EPD101 补推 {} 单", ok);
        }
        return ok;
    }
}
