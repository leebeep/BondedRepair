package com.ruoyi.bonded.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.bonded.domain.BrIssue;
import com.ruoyi.bonded.domain.BrIssueItem;

/**
 * 工单发料单 Mapper（RRD2）
 */
public interface BrIssueMapper
{
    public BrIssue selectById(@Param("id") Long id);

    public List<BrIssue> selectList(BrIssue issue);

    public BrIssue selectByIssueNo(@Param("issueNo") String issueNo);

    /** 按issueNo查询（含软删除行，用于编号占用校验的友好提示） */
    public BrIssue selectByIssueNoAny(@Param("issueNo") String issueNo);

    /** 按前缀查最大发料单号（自动编号流水用，含逻辑删除行避免流水撞号） */
    public String selectMaxIssueNoByPrefix(@Param("prefix") String prefix);

    public int insert(BrIssue issue);

    public int update(BrIssue issue);

    public int deleteByIds(@Param("ids") Long[] ids);

    public List<BrIssueItem> selectItemsByIssueId(@Param("issueId") Long issueId);

    public int insertItem(BrIssueItem item);

    public int deleteItemsByIssueId(@Param("issueId") Long issueId);

    public int updateDeclareResult(@Param("id") Long id, @Param("uuid") String uuid,
            @Param("docStatus") String docStatus, @Param("customsStatus") String customsStatus,
            @Param("executeType") String executeType);
}
