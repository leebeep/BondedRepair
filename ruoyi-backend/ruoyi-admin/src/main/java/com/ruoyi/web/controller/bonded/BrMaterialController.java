package com.ruoyi.web.controller.bonded;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
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
import com.ruoyi.bonded.domain.BrMaterial;
import com.ruoyi.bonded.service.IBrMaterialService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;

/**
 * 物料备案（EPD101）Controller
 */
@RestController
@RequestMapping("/bonded/material")
public class BrMaterialController extends BaseController
{
    @Autowired
    private IBrMaterialService materialService;

    @PreAuthorize("@ss.hasPermi('bonded:material:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrMaterial material)
    {
        startPage();
        List<BrMaterial> list = materialService.selectBrMaterialList(material);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('bonded:material:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, BrMaterial material)
    {
        List<BrMaterial> list = materialService.selectBrMaterialList(material);
        ExcelUtil<BrMaterial> util = new ExcelUtil<BrMaterial>(BrMaterial.class);
        util.exportExcel(response, list, "物料备案数据");
    }

    @PreAuthorize("@ss.hasPermi('bonded:material:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(materialService.selectBrMaterialById(id));
    }

    @PreAuthorize("@ss.hasPermi('bonded:material:add')")
    @Log(title = "物料备案", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BrMaterial material)
    {
        return toAjax(materialService.insertBrMaterial(material));
    }

    @PreAuthorize("@ss.hasPermi('bonded:material:edit')")
    @Log(title = "物料备案", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BrMaterial material)
    {
        return toAjax(materialService.updateBrMaterial(material));
    }

    @PreAuthorize("@ss.hasPermi('bonded:material:remove')")
    @Log(title = "物料备案", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(materialService.deleteBrMaterialByIds(ids));
    }

    /**
     * 提交 EPD101 申报（1-备案 2-变更 3-作废）
     */
    @PreAuthorize("@ss.hasPermi('bonded:material:declare')")
    @Log(title = "物料备案申报", businessType = BusinessType.EXPORT)
    @PostMapping("/declare/{executeType}/{ids}")
    public AjaxResult declare(@PathVariable("executeType") String executeType, @PathVariable Long[] ids)
    {
        return toAjax(materialService.declareEPD101(ids, executeType));
    }
}
