package com.ruoyi.bonded.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.bonded.domain.BrInbound;
import com.ruoyi.bonded.domain.BrInboundItem;

/**
 * 入库单 Mapper（PTO2）
 */
public interface BrInboundMapper
{
    public BrInbound selectById(@Param("id") Long id);

    public List<BrInbound> selectList(BrInbound inbound);

    public BrInbound selectByInboundNo(@Param("inboundNo") String inboundNo);

    /** 按inboundNo查询（含软删除行，用于编号占用校验的友好提示） */
    public BrInbound selectByInboundNoAny(@Param("inboundNo") String inboundNo);

    /** 按前缀查最大入库单号（自动编号流水用，含逻辑删除行避免流水撞号） */
    public String selectMaxInboundNoByPrefix(@Param("prefix") String prefix);

    public int insert(BrInbound inbound);

    public int update(BrInbound inbound);

    public int deleteByIds(@Param("ids") Long[] ids);

    public List<BrInboundItem> selectItemsByInboundId(@Param("inboundId") Long inboundId);

    public int insertItem(BrInboundItem item);

    public int deleteItemsByInboundId(@Param("inboundId") Long inboundId);

    public int updateDeclareResult(@Param("id") Long id, @Param("uuid") String uuid,
            @Param("docStatus") String docStatus, @Param("customsStatus") String customsStatus,
            @Param("executeType") String executeType);
}
