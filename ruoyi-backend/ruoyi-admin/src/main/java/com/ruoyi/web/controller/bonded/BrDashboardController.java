package com.ruoyi.web.controller.bonded;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.bonded.domain.StockReconcileRow;
import com.ruoyi.bonded.mapper.BrDashboardMapper;
import com.ruoyi.bonded.mapper.BrStockLedgerMapper;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;

/**
 * 首页工作台 Controller：统计概览 + 导航数据
 */
@RestController
@RequestMapping("/bonded/dashboard")
public class BrDashboardController extends BaseController
{
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Autowired
    private BrDashboardMapper dashboardMapper;

    @Autowired
    private BrStockLedgerMapper ledgerMapper;

    /** 工作台统计数据（登录即可访问，供首页展示） */
    @GetMapping("/stats")
    public AjaxResult stats()
    {
        Map<String, Object> data = new HashMap<>();

        // 1. 物料备案：总数 + 各备案状态数量
        List<Map<String, Object>> matRows = dashboardMapper.selectMaterialByStatus();
        Map<String, Object> material = new HashMap<>();
        material.put("total", sumCnt(matRows));
        Map<String, Object> matByStatus = new HashMap<>();
        for (Map<String, Object> r : matRows)
        {
            matByStatus.put(String.valueOf(r.get("k")), r.get("cnt"));
        }
        material.put("byStatus", matByStatus);
        data.put("material", material);

        // 2. 维修工单：各状态数量 + 进行中(01/02/03)
        List<Map<String, Object>> woRows = dashboardMapper.selectWorkOrderByStatus();
        Map<String, Object> woByStatus = new HashMap<>();
        long woTotal = 0, woActive = 0;
        for (Map<String, Object> r : woRows)
        {
            String st = String.valueOf(r.get("k"));
            long c = toLong(r.get("cnt"));
            woByStatus.put(st, c);
            woTotal += c;
            if ("01".equals(st) || "02".equals(st) || "03".equals(st))
            {
                woActive += c;
            }
        }
        Map<String, Object> workOrder = new HashMap<>();
        workOrder.put("total", woTotal);
        workOrder.put("active", woActive);
        workOrder.put("byStatus", woByStatus);
        data.put("workOrder", workOrder);

        // 3. 单据流：入库/出库/发料/收料 各状态计数 + 今日新增
        List<Map<String, Object>> docRows = dashboardMapper.selectDocByStatus();
        Map<String, Map<String, Object>> flow = new LinkedHashMap<>();
        for (String t : new String[] { "IN", "OUT", "ISSUE", "RCV" })
        {
            Map<String, Object> f = new HashMap<>();
            f.put("total", 0L);
            f.put("today", 0L);
            f.put("pendingAudit", 0L);   // 02-待审核
            f.put("pendingDeclare", 0L); // 03-审核通过待申报
            f.put("back", 0L);           // 06-退单
            flow.put(t, f);
        }
        for (Map<String, Object> r : docRows)
        {
            String t = String.valueOf(r.get("docType"));
            Map<String, Object> f = flow.get(t);
            if (f == null)
            {
                continue;
            }
            String st = String.valueOf(r.get("k"));
            long c = toLong(r.get("cnt"));
            long td = toLong(r.get("todayCnt"));
            f.put("total", toLong(f.get("total")) + c);
            f.put("today", toLong(f.get("today")) + td);
            if ("02".equals(st)) f.put("pendingAudit", c);
            if ("03".equals(st)) f.put("pendingDeclare", c);
            if ("06".equals(st)) f.put("back", c);
        }
        data.put("flow", flow);

        // 4. 申报：待回执/通过/退单失败/转人工
        List<Map<String, Object>> dlRows = dashboardMapper.selectDeclareByStatus();
        Map<String, Object> declare = new HashMap<>();
        long pending = 0, success = 0, fail = 0, manual = 0;
        for (Map<String, Object> r : dlRows)
        {
            String st = String.valueOf(r.get("k"));
            long c = toLong(r.get("cnt"));
            if ("PENDING".equals(st)) pending += c;
            else if ("Y".equals(st) || "1".equals(st)) success += c;
            else if ("N".equals(st) || "3".equals(st)) fail += c;
            else if ("2".equals(st)) manual += c;
        }
        declare.put("pending", pending);
        declare.put("success", success);
        declare.put("fail", fail);
        declare.put("manual", manual);
        data.put("declare", declare);

        // 5. 六类账存（全量口径，与库存台账-对账一致）
        List<StockReconcileRow> summary = ledgerMapper.selectMarkTypeSummary(null, null);
        Map<String, StockReconcileRow> byMark = new HashMap<>();
        for (StockReconcileRow row : summary)
        {
            byMark.put(row.getMarkType(), row);
        }
        List<Map<String, Object>> cards = new ArrayList<>();
        for (String t : new String[] { "1", "2", "3", "4", "5", "6", "8" })
        {
            Map<String, Object> c = new HashMap<>();
            c.put("markType", t);
            c.put("balance", byMark.containsKey(t) ? byMark.get(t).getBalance() : ZERO);
            cards.add(c);
        }
        data.put("stockCards", cards);

        // 6. 近 7 日出入库趋势（缺失日期补 0）
        List<Map<String, Object>> trendRows = dashboardMapper.selectTrend7();
        Map<String, Map<String, Object>> byDate = new HashMap<>();
        for (Map<String, Object> r : trendRows)
        {
            byDate.put(String.valueOf(r.get("d")), r);
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--)
        {
            LocalDate d = today.minusDays(i);
            String ds = d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Map<String, Object> row = byDate.get(ds);
            Map<String, Object> item = new HashMap<>();
            item.put("date", d.format(fmt));
            item.put("inQty", row == null ? ZERO : nz(row.get("inQty")));
            item.put("outQty", row == null ? ZERO : nz(row.get("outQty")));
            trend.add(item);
        }
        data.put("trend", trend);

        // 7. 最近申报动态（8 条）
        data.put("recentLogs", dashboardMapper.selectRecentLogs(8));

        return success(data);
    }

    private long sumCnt(List<Map<String, Object>> rows)
    {
        long s = 0;
        for (Map<String, Object> r : rows)
        {
            s += toLong(r.get("cnt"));
        }
        return s;
    }

    private long toLong(Object v)
    {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).longValue();
        try
        {
            return Long.parseLong(String.valueOf(v));
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    private BigDecimal nz(Object v)
    {
        if (v == null) return ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        return new BigDecimal(String.valueOf(v));
    }
}
