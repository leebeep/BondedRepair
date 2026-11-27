package com.ruoyi.web.controller.bonded;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.bonded.domain.BrStockLedger;
import com.ruoyi.bonded.domain.StockReconcileRow;
import com.ruoyi.bonded.mapper.BrStockLedgerMapper;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 库存台账与对账 Controller
 */
@RestController
@RequestMapping("/bonded/stock")
public class BrStockController extends BaseController
{
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Autowired
    private BrStockLedgerMapper ledgerMapper;

    @PreAuthorize("@ss.hasPermi('bonded:stock:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrStockLedger query)
    {
        startPage();
        List<BrStockLedger> list = ledgerMapper.selectList(query);
        return getDataTable(list);
    }

    /**
     * 六类货物核销对账（PRD §7 公式）：
     * 待修复账存 = 入库(04) − 发料 + 未使用收料(1) − 未投入退运 = 0；
     * 实际投入 = 发料 − 未使用收料 = 已修复收料 + 无法修复收料；
     * 料件账存 = 入库(05) − 发料 + 余料收料 − 余料复运 = 结转下期；
     * 已修复/无法修复/旧件/边角料账存 = 收料 − 出库 = 0。
     */
    @PreAuthorize("@ss.hasPermi('bonded:stock:list')")
    @GetMapping("/reconcile")
    public AjaxResult reconcile(@RequestParam(required = false) String batchNo,
                                @RequestParam(required = false) String workNo)
    {
        List<StockReconcileRow> summary = ledgerMapper.selectMarkTypeSummary(batchNo, workNo);
        Map<String, StockReconcileRow> byMark = new HashMap<>();
        for (StockReconcileRow r : summary)
        {
            byMark.put(r.getMarkType(), r);
        }

        // 六类账存卡片（1/2/3/4/5/6/8）
        List<Map<String, Object>> cards = new ArrayList<>();
        String[] types = { "1", "2", "3", "4", "5", "6", "8" };
        for (String t : types)
        {
            Map<String, Object> c = new HashMap<>();
            c.put("markType", t);
            c.put("balance", balance(byMark, t));
            cards.add(c);
        }

        // 四项公式对账行：项目 / 期望 / 实际 / 差异(0 为平)
        BigDecimal n1 = nz(byMark, "1"), n2 = nz(byMark, "2"), n3 = nz(byMark, "3");
        BigDecimal n5 = nz(byMark, "5"), n6 = nz(byMark, "6");
        BigDecimal issue1 = byMark.containsKey("1") ? byMark.get("1").getIssueQty() : ZERO;
        BigDecimal rcv1 = byMark.containsKey("1") ? byMark.get("1").getRcvQty() : ZERO;
        BigDecimal rcv2 = byMark.containsKey("2") ? byMark.get("2").getRcvQty() : ZERO;
        BigDecimal rcv3 = byMark.containsKey("3") ? byMark.get("3").getRcvQty() : ZERO;
        // 发料 ISSUE 为负值
        BigDecimal invested = issue1.negate().subtract(rcv1);
        BigDecimal repairedRcv = rcv2.add(rcv3);

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(formulaRow("待修复账存（入库04−发料+未使用收料−退运）", ZERO, n1));
        rows.add(formulaRow("实际投入（发料−未使用收料）", invested, invested));
        rows.add(formulaRow("已修复+无法修复收料（=实际投入）", invested, repairedRcv));
        rows.add(formulaRow("料件账存（入库05−发料+余料收料−余料复运，结转下期）", null, nz(byMark, "4")));
        rows.add(formulaRow("已修复账存（收料−出库）", ZERO, n2));
        rows.add(formulaRow("无法修复账存（收料−出库）", ZERO, n3));
        rows.add(formulaRow("旧件坏件账存（收料−出库）", ZERO, n5));
        rows.add(formulaRow("边角料账存（收料−出库）", ZERO, n6));

        boolean allZero = n1.compareTo(ZERO) == 0 && n2.compareTo(ZERO) == 0
                && n3.compareTo(ZERO) == 0 && n5.compareTo(ZERO) == 0 && n6.compareTo(ZERO) == 0
                && invested.compareTo(repairedRcv) == 0;

        Map<String, Object> data = new HashMap<>();
        data.put("cards", cards);
        data.put("rows", rows);
        data.put("allZero", allZero);
        return success(data);
    }

    private BigDecimal balance(Map<String, StockReconcileRow> byMark, String markType)
    {
        return byMark.containsKey(markType) ? byMark.get(markType).getBalance() : ZERO;
    }

    private BigDecimal nz(Map<String, StockReconcileRow> byMark, String markType)
    {
        return balance(byMark, markType);
    }

    private Map<String, Object> formulaRow(String name, BigDecimal expected, BigDecimal actual)
    {
        Map<String, Object> r = new HashMap<>();
        r.put("item", name);
        r.put("expected", expected == null ? "结转" : expected);
        r.put("actual", actual);
        r.put("diff", expected == null ? ZERO : actual.subtract(expected));
        return r;
    }
}
