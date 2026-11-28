package com.ruoyi.bonded.service;

import java.util.List;
import com.ruoyi.bonded.domain.BrInbound;

/**
 * 入库单服务（PTO2）
 */
public interface IBrInboundService
{
    public List<BrInbound> selectList(BrInbound inbound);

    public BrInbound selectById(Long id);

    public int insert(BrInbound inbound);

    public int update(BrInbound inbound);

    public int deleteByIds(Long[] ids);

    /** 下一张入库单号（默认自动生成，前缀经 参数设置 br.inbound.no.prefix 修改） */
    public String nextInboundNo();

    /** 审核：01/02 -> 03，登记台账 IN */
    public int audit(Long id);

    /** 提交 PTO2 申报 */
    public int declare(Long[] ids, String executeType);
}
