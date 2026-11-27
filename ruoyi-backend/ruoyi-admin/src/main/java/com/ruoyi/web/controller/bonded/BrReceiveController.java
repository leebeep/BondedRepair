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
import com.ruoyi.bonded.domain.BrReceive;
import com.ruoyi.bonded.service.IBrReceiveService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;

/**
 * 工单收料 Controller（MSO2）
 */
@RestController
@RequestMapping("/bonded/receive")
public class BrReceiveController extends BaseController
{
    @Autowired
    private IBrReceiveService receiveService;

    @PreAuthorize("@ss.hasPermi('bonded:rcv:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrReceive receive)
    {
        startPage();
        return getDataTable(receiveService.selectList(receive));
    }

    @PreAuthorize("@ss.hasPermi('bonded:rcv:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(receiveService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('bonded:rcv:add')")
    @Log(title = "工单收料", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BrReceive receive)
    {
        return toAjax(receiveService.insert(receive));
    }

    @PreAuthorize("@ss.hasPermi('bonded:rcv:edit')")
    @Log(title = "工单收料", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BrReceive receive)
    {
        return toAjax(receiveService.update(receive));
    }

    @PreAuthorize("@ss.hasPermi('bonded:rcv:remove')")
    @Log(title = "工单收料", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(receiveService.deleteByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('bonded:rcv:audit')")
    @Log(title = "收料单审核", businessType = BusinessType.UPDATE)
    @PostMapping("/audit/{id}")
    public AjaxResult audit(@PathVariable Long id)
    {
        return toAjax(receiveService.audit(id));
    }

    @PreAuthorize("@ss.hasPermi('bonded:rcv:declare')")
    @Log(title = "收料申报", businessType = BusinessType.EXPORT)
    @PostMapping("/declare/{executeType}/{ids}")
    public AjaxResult declare(@PathVariable("executeType") String executeType, @PathVariable Long[] ids)
    {
        return toAjax(receiveService.declare(ids, executeType));
    }
}
