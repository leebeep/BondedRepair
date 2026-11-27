package com.ruoyi.bonded.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 首页工作台统计 Mapper
 */
public interface BrDashboardMapper
{
    /** 物料清单按备案状态分组计数（declare_status -> cnt） */
    public List<Map<String, Object>> selectMaterialByStatus();

    /** 维修工单按工单状态分组计数（order_status -> cnt） */
    public List<Map<String, Object>> selectWorkOrderByStatus();

    /** 单据按单据状态分组计数：docType(IN/OUT/ISSUE/RCV) + doc_status -> cnt + todayCnt */
    public List<Map<String, Object>> selectDocByStatus();

    /** 申报日志按回执状态计数（PENDING/Y/N/1/2/3 -> cnt） */
    public List<Map<String, Object>> selectDeclareByStatus();

    /** 近 7 日出入库数量趋势（date -> inQty/outQty） */
    public List<Map<String, Object>> selectTrend7();

    /** 最近申报动态（最近 limit 条） */
    public List<Map<String, Object>> selectRecentLogs(@Param("limit") int limit);
}
