package com.ruoyi.web.controller.bonded;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.bonded.domain.BrIssue;
import com.ruoyi.bonded.service.IBrIssueService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;

/**
 * 工单发料 Controller（RRD2）
 */
@RestController
@RequestMapping("/bonded/issue")
public class BrIssueController extends BaseController
{
    @Autowired
    private IBrIssueService issueService;

    @PreAuthorize("@ss.hasPermi('bonded:issue:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrIssue issue)
    {
        startPage();
        return getDataTable(issueService.selectList(issue));
    }

    @PreAuthorize("@ss.hasPermi('bonded:issue:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(issueService.selectById(id));
    }

    /** 下一张发料单号（默认自动生成，前缀经 参数设置 br.issue.no.prefix 修改） */
    @PreAuthorize("@ss.hasPermi('bonded:issue:add')")
    @GetMapping("/nextNo")
    public AjaxResult nextNo()
    {
        return AjaxResult.success("操作成功", issueService.nextIssueNo());
    }

    @PreAuthorize("@ss.hasPermi('bonded:issue:add')")
    @Log(title = "工单发料", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BrIssue issue)
    {
        String warn = issueService.draftWarning(issue);
        int rows = issueService.insert(issue);
        return com.ruoyi.common.utils.StringUtils.isEmpty(warn) ? toAjax(rows) : AjaxResult.success("保存成功，注意：" + warn);
    }

    @PreAuthorize("@ss.hasPermi('bonded:issue:edit')")
    @Log(title = "工单发料", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BrIssue issue)
    {
        return toAjax(issueService.update(issue));
    }

    @PreAuthorize("@ss.hasPermi('bonded:issue:remove')")
    @Log(title = "工单发料", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(issueService.deleteByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('bonded:issue:audit')")
    @Log(title = "发料单审核", businessType = BusinessType.UPDATE)
    @PostMapping("/audit/{id}")
    public AjaxResult audit(@PathVariable Long id)
    {
        return toAjax(issueService.audit(id));
    }

    @PreAuthorize("@ss.hasPermi('bonded:issue:declare')")
    @Log(title = "发料申报", businessType = BusinessType.EXPORT)
    @PostMapping("/declare/{executeType}/{ids}")
    public AjaxResult declare(@PathVariable("executeType") String executeType, @PathVariable Long[] ids)
    {
        return toAjax(issueService.declare(ids, executeType));
    }
}
