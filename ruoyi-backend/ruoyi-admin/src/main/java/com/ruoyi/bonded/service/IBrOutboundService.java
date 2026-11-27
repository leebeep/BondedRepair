package com.ruoyi.bonded.service;

import java.util.List;
import com.ruoyi.bonded.domain.BrOutbound;

/**
 * 出库单服务（SDO2）
 */
public interface IBrOutboundService
{
    public List<BrOutbound> selectList(BrOutbound outbound);

    public BrOutbound selectById(Long id);

    /** 下一张出库单号（前缀+日期+流水，前缀取 sys_config 参数 br.outbound.no.prefix） */
    public String nextOutboundNo();

    public int insert(BrOutbound outbound);

    public int update(BrOutbound outbound);

    public int deleteByIds(Long[] ids);

    /** 审核：余额校验 + 台账 OUT（busiType=05 强制服务费行） */
    public int audit(Long id);

    /** 提交 SDO2 申报 */
    public int declare(Long[] ids, String executeType);
}
