package com.ruoyi.bonded.service;

import java.util.List;
import com.ruoyi.bonded.domain.BrIssue;

/**
 * 工单发料服务（RRD2）
 */
public interface IBrIssueService
{
    public List<BrIssue> selectList(BrIssue issue);

    public BrIssue selectById(Long id);

    public int insert(BrIssue issue);

    public int update(BrIssue issue);

    public int deleteByIds(Long[] ids);

    /** 审核：校验来源余额，登记台账 ISSUE（负数） */
    public int audit(Long id);

    /** 提交 RRD2 申报 */
    public int declare(Long[] ids, String executeType);
}
