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
import com.ruoyi.bonded.domain.BrWorkOrder;
import com.ruoyi.bonded.service.BrWorkOrderService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;

/**
 * 维修工单 Controller（工单先行立项，发料/收料选择关联）
 */
@RestController
@RequestMapping("/bonded/workorder")
public class BrWorkOrderController extends BaseController
{
    @Autowired
    private BrWorkOrderService workOrderService;

    @PreAuthorize("@ss.hasPermi('bonded:wo:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrWorkOrder workOrder)
    {
        startPage();
        return getDataTable(workOrderService.selectList(workOrder));
    }

    /** 未结案工单下拉（01/02/03），供发料/收料表单选择 */
    @PreAuthorize("@ss.hasPermi('bonded:wo:list')")
    @GetMapping("/options")
    public AjaxResult options()
    {
        return AjaxResult.success("操作成功", workOrderService.selectOpenList());
    }

    /** 工单详情：工单+投入明细+关联发料/收料/出库单+勾稽汇总 */
    @PreAuthorize("@ss.hasPermi('bonded:wo:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(workOrderService.selectDetail(id));
    }

    /** 下一张工单号（默认自动生成，前缀经 参数设置 br.workorder.no.prefix 修改） */
    @PreAuthorize("@ss.hasPermi('bonded:wo:add')")
    @GetMapping("/nextNo")
    public AjaxResult nextNo()
    {
        return AjaxResult.success("操作成功", workOrderService.nextWorkOrderNo());
    }

    @PreAuthorize("@ss.hasPermi('bonded:wo:add')")
    @Log(title = "维修工单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BrWorkOrder workOrder)
    {
        return toAjax(workOrderService.insertWorkOrder(workOrder));
    }

    @PreAuthorize("@ss.hasPermi('bonded:wo:edit')")
    @Log(title = "维修工单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BrWorkOrder workOrder)
    {
        return toAjax(workOrderService.updateWorkOrder(workOrder));
    }

    @PreAuthorize("@ss.hasPermi('bonded:wo:remove')")
    @Log(title = "维修工单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(workOrderService.deleteByIds(ids));
    }

    /** 结案：仅已出清(04)工单可结案 → 05 */
    @PreAuthorize("@ss.hasPermi('bonded:wo:edit')")
    @Log(title = "维修工单结案", businessType = BusinessType.UPDATE)
    @PostMapping("/close/{id}")
    public AjaxResult close(@PathVariable Long id)
    {
        return toAjax(workOrderService.closeWorkOrder(id));
    }
}
