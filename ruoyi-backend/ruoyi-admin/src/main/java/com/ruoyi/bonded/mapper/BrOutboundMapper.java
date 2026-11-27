package com.ruoyi.bonded.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.bonded.domain.BrOutbound;
import com.ruoyi.bonded.domain.BrOutboundItem;

/**
 * 出库单 Mapper（SDO2）
 */
public interface BrOutboundMapper
{
    public BrOutbound selectById(@Param("id") Long id);

    public List<BrOutbound> selectList(BrOutbound outbound);

    public BrOutbound selectByOutboundNo(@Param("outboundNo") String outboundNo);

    public int insert(BrOutbound outbound);

    public int update(BrOutbound outbound);

    public int deleteByIds(@Param("ids") Long[] ids);

    public List<BrOutboundItem> selectItemsByOutboundId(@Param("outboundId") Long outboundId);

    public int insertItem(BrOutboundItem item);

    public int deleteItemsByOutboundId(@Param("outboundId") Long outboundId);

    public int updateDeclareResult(@Param("id") Long id, @Param("uuid") String uuid,
            @Param("docStatus") String docStatus, @Param("customsStatus") String customsStatus,
            @Param("executeType") String executeType);
}
