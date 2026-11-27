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
import com.ruoyi.bonded.domain.BrInbound;
import com.ruoyi.bonded.service.IBrInboundService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;

/**
 * 入库管理 Controller（PTO2）
 */
@RestController
@RequestMapping("/bonded/inbound")
public class BrInboundController extends BaseController
{
    @Autowired
    private IBrInboundService inboundService;

    @PreAuthorize("@ss.hasPermi('bonded:in:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrInbound inbound)
    {
        startPage();
        return getDataTable(inboundService.selectList(inbound));
    }

    @PreAuthorize("@ss.hasPermi('bonded:in:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(inboundService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('bonded:in:add')")
    @Log(title = "入库单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BrInbound inbound)
    {
        return toAjax(inboundService.insert(inbound));
    }

    @PreAuthorize("@ss.hasPermi('bonded:in:edit')")
    @Log(title = "入库单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BrInbound inbound)
    {
        return toAjax(inboundService.update(inbound));
    }

    @PreAuthorize("@ss.hasPermi('bonded:in:remove')")
    @Log(title = "入库单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(inboundService.deleteByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('bonded:in:audit')")
    @Log(title = "入库单审核", businessType = BusinessType.UPDATE)
    @PostMapping("/audit/{id}")
    public AjaxResult audit(@PathVariable Long id)
    {
        return toAjax(inboundService.audit(id));
    }

    @PreAuthorize("@ss.hasPermi('bonded:in:declare')")
    @Log(title = "入库申报", businessType = BusinessType.EXPORT)
    @PostMapping("/declare/{executeType}/{ids}")
    public AjaxResult declare(@PathVariable("executeType") String executeType, @PathVariable Long[] ids)
    {
        return toAjax(inboundService.declare(ids, executeType));
    }
}
