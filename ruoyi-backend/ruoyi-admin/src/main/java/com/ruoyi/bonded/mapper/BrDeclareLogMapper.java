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

    /** 单据的全部申报日志（修改前清理未采集镜像行用） */
    public List<BrDeclareLog> selectLogsByBiz(@Param("bizTable") String bizTable, @Param("bizId") Long bizId);

    /** 删除单条申报日志 */
    public int deleteLogById(@Param("id") Long id);

    /** 删除镜像表行（mirrorTable 仅限白名单，由服务层校验） */
    public int deleteMirrorRow(@Param("mirrorTable") String mirrorTable, @Param("mirrorRowId") Long mirrorRowId);
}
