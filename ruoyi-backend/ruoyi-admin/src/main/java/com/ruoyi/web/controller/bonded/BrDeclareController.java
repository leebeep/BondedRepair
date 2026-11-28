package com.ruoyi.web.controller.bonded;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.bonded.domain.BrDeclareLog;
import com.ruoyi.bonded.mapper.BrDeclareLogMapper;
import com.ruoyi.bonded.mapper.BrSyncMapper;
import com.ruoyi.bonded.service.BrPushSwitch;
import com.ruoyi.bonded.task.BrPendingSyncTask;
import com.ruoyi.bonded.task.CustomsQueryService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.annotation.Log;

/**
 * 联网日志 Controller（原申报管理）：自动申报开关 + 采集看板 + 同步/查询日志
 */
@RestController
@RequestMapping("/bonded/declare")
public class BrDeclareController extends BaseController
{
    @Autowired
    private BrDeclareLogMapper declareLogMapper;

    @Autowired private BrPushSwitch pushSwitch;
    @Autowired private BrSyncMapper syncMapper;
    @Autowired private BrPendingSyncTask pendingSyncTask;
    @Autowired private CustomsQueryService queryService;

    /** 自动申报开关：查询 */
    @PreAuthorize("@ss.hasPermi('bonded:declare:list')")
    @GetMapping("/push-switch")
    public AjaxResult pushSwitchGet()
    {
        return success(pushSwitch.isEnabled());
    }

    /** 自动申报开关：切换 */
    @PreAuthorize("@ss.hasPermi('bonded:declare:edit')")
    @Log(title = "联网自动申报开关", businessType = BusinessType.UPDATE)
    @PutMapping("/push-switch/{enabled}")
    public AjaxResult pushSwitchSet(@PathVariable boolean enabled)
    {
        pushSwitch.setEnabled(enabled);
        return success(enabled);
    }

    /** 采集状态看板：五类报文按海关状态计数 */
    @PreAuthorize("@ss.hasPermi('bonded:declare:list')")
    @GetMapping("/sync-board")
    public AjaxResult syncBoard()
    {
        return success(syncMapper.selectSyncBoard());
    }

    /** 手动补报：立即申报开关关闭期间滞留的待申报单据（03/00、物料 0/00） */
    @PreAuthorize("@ss.hasPermi('bonded:declare:edit')")
    @Log(title = "联网日志补报", businessType = BusinessType.OTHER)
    @PostMapping("/push-pending")
    public AjaxResult pushPending()
    {
        int n = pendingSyncTask.pushPending();
        return success("本次补报 " + n + " 单");
    }

    /** 手动主动查询：立即调公服查询接口覆盖回执（写回镜像表，轮询任务回读业务单据） */
    @PreAuthorize("@ss.hasPermi('bonded:declare:edit')")
    @Log(title = "联网日志主动查询", businessType = BusinessType.OTHER)
    @PostMapping("/query-receipt")
    public AjaxResult queryReceipt()
    {
        int n = queryService.queryOnce();
        return success("本次查询 " + n + " 条");
    }

    /** 回执结果看板：五类报文按回执状态计数 */
    @PreAuthorize("@ss.hasPermi('bonded:declare:list')")
    @GetMapping("/board")
    public AjaxResult board()
    {
        return success(declareLogMapper.selectBoard());
    }

    /** 同步日志明细（分页） */
    @PreAuthorize("@ss.hasPermi('bonded:declare:list')")
    @GetMapping("/log")
    public TableDataInfo log(BrDeclareLog query)
    {
        startPage();
        List<BrDeclareLog> list = declareLogMapper.selectLogList(query);
        return getDataTable(list);
    }
}
