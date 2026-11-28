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
import com.ruoyi.bonded.domain.BrOutbound;
import com.ruoyi.bonded.service.IBrOutboundService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;

/**
 * 出库 Controller（SDO2）
 */
@RestController
@RequestMapping("/bonded/outbound")
public class BrOutboundController extends BaseController
{
    @Autowired
    private IBrOutboundService outboundService;

    @PreAuthorize("@ss.hasPermi('bonded:out:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrOutbound outbound)
    {
        startPage();
        return getDataTable(outboundService.selectList(outbound));
    }

    @PreAuthorize("@ss.hasPermi('bonded:out:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(outboundService.selectById(id));
    }

    /** 下一张出库单号（默认自动生成，前缀经 参数设置 br.outbound.no.prefix 修改） */
    @PreAuthorize("@ss.hasPermi('bonded:out:add')")
    @GetMapping("/nextNo")
    public AjaxResult nextNo()
    {
        return AjaxResult.success("操作成功", outboundService.nextOutboundNo());
    }

    @PreAuthorize("@ss.hasPermi('bonded:out:add')")
    @Log(title = "出库单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BrOutbound outbound)
    {
        String warn = outboundService.draftWarning(outbound);
        int rows = outboundService.insert(outbound);
        return com.ruoyi.common.utils.StringUtils.isEmpty(warn) ? toAjax(rows) : AjaxResult.success("保存成功，注意：" + warn);
    }

    @PreAuthorize("@ss.hasPermi('bonded:out:edit')")
    @Log(title = "出库单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BrOutbound outbound)
    {
        return toAjax(outboundService.update(outbound));
    }

    @PreAuthorize("@ss.hasPermi('bonded:out:remove')")
    @Log(title = "出库单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(outboundService.deleteByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('bonded:out:audit')")
    @Log(title = "出库单审核", businessType = BusinessType.UPDATE)
    @PostMapping("/audit/{id}")
    public AjaxResult audit(@PathVariable Long id)
    {
        return toAjax(outboundService.audit(id));
    }

    @PreAuthorize("@ss.hasPermi('bonded:out:declare')")
    @Log(title = "出库申报", businessType = BusinessType.EXPORT)
    @PostMapping("/declare/{executeType}/{ids}")
    public AjaxResult declare(@PathVariable("executeType") String executeType, @PathVariable Long[] ids)
    {
        return toAjax(outboundService.declare(ids, executeType));
    }
}
