package com.ruoyi.bonded.service;

import java.util.List;
import com.ruoyi.bonded.domain.BrReceive;

/**
 * 工单收料服务（MSO2）
 */
public interface IBrReceiveService
{
    public List<BrReceive> selectList(BrReceive receive);

    public BrReceive selectById(Long id);

    /** 新增（工单有服务费且未包含 returnType=8 行时自动生成服务费行） */
    public int insert(BrReceive receive);

    public int update(BrReceive receive);

    public int deleteByIds(Long[] ids);

    /** 审核：收料校验 + 台账 RCV */
    public int audit(Long id);

    /** 提交 MSO2 申报 */
    public int declare(Long[] ids, String executeType);
}
