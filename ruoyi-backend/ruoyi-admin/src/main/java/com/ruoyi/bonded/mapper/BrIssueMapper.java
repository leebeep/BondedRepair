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
