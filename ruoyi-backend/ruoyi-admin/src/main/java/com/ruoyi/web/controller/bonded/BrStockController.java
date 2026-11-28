package com.ruoyi.web.controller.bonded;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.bonded.domain.BrMaterialStock;
import com.ruoyi.bonded.domain.BrStockLedger;
import com.ruoyi.bonded.mapper.BrMaterialStockMapper;
import com.ruoyi.bonded.mapper.BrStockLedgerMapper;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 库存管理 Controller（列表=每物料一行的结存汇总；详情=物料台账流水下钻）
 */
@RestController
@RequestMapping("/bonded/stock")
public class BrStockController extends BaseController
{
    @Autowired
    private BrMaterialStockMapper stockMapper;

    @Autowired
    private BrStockLedgerMapper ledgerMapper;

    /** 库存列表：按物料一行的结存汇总（台账流水在详情页按物料下钻） */
    @PreAuthorize("@ss.hasPermi('bonded:stock:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrMaterialStock query)
    {
        startPage();
        List<BrMaterialStock> list = stockMapper.selectStockSummary(query);
        return getDataTable(list);
    }

    /** 库存结存汇总导出（按当前筛选条件） */
    @PreAuthorize("@ss.hasPermi('bonded:stock:list')")
    @PostMapping("/export")
    public void export(jakarta.servlet.http.HttpServletResponse response, BrMaterialStock query)
    {
        List<BrMaterialStock> list = stockMapper.selectStockSummary(query);
        com.ruoyi.common.utils.poi.ExcelUtil<BrMaterialStock> util = new com.ruoyi.common.utils.poi.ExcelUtil<>(BrMaterialStock.class);
        util.exportExcel(response, list, "物料库存结存");
    }

    /** 物料台账流水（详情页"更新记录"下钻用，按 materialId + 筛选条件） */
    @PreAuthorize("@ss.hasPermi('bonded:stock:list')")
    @GetMapping("/ledger")
    public TableDataInfo ledgerList(BrStockLedger query)
    {
        startPage();
        List<BrStockLedger> list = ledgerMapper.selectList(query);
        return getDataTable(list);
    }

    /** 物料台账流水导出（详情页"导出更新记录"用） */
    @PreAuthorize("@ss.hasPermi('bonded:stock:list')")
    @PostMapping("/ledger/export")
    public void ledgerExport(jakarta.servlet.http.HttpServletResponse response, BrStockLedger query)
    {
        List<BrStockLedger> list = ledgerMapper.selectList(query);
        com.ruoyi.common.utils.poi.ExcelUtil<BrStockLedger> util = new com.ruoyi.common.utils.poi.ExcelUtil<>(BrStockLedger.class);
        util.exportExcel(response, list, "库存台账");
    }

    /** 物料库存详情：物料基本信息 + 当前结存 + 本月四向流量 */
    @PreAuthorize("@ss.hasPermi('bonded:stock:list')")
    @GetMapping("/material/{materialId}")
    public AjaxResult materialDetail(@PathVariable Long materialId)
    {
        BrMaterialStock stock = stockMapper.selectByMaterialId(materialId);
        if (stock == null) { return error("物料库存不存在"); }
        String monthBegin = java.time.LocalDate.now().withDayOfMonth(1).toString();
        Map<String, Object> data = new HashMap<>();
        data.put("stock", stock);
        data.put("monthStats", stockMapper.selectMonthStats(materialId, monthBegin));
        return success(data);
    }
}
