package com.ruoyi.bonded.mapper;

import java.util.List;
import com.ruoyi.bonded.domain.BrDeclareLog;
import org.apache.ibatis.annotations.Param;

/**
 * 申报日志 Mapper
 */
public interface BrDeclareLogMapper
{
    public int insertBrDeclareLog(BrDeclareLog log);

    public List<BrDeclareLog> selectPendingLogs(@Param("limit") int limit);

    public int updateReceipt(BrDeclareLog log);

    /** 申报日志分页查询（看板明细） */
    public List<BrDeclareLog> selectLogList(BrDeclareLog query);

    /** 看板汇总：按 biz_type + result_status 分组计数 */
    public List<java.util.Map<String, Object>> selectBoard();
}
