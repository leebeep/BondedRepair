package com.ruoyi.bonded.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.bonded.domain.BrReceive;
import com.ruoyi.bonded.domain.BrReceiveItem;

/**
 * 工单收料单 Mapper（MSO2）
 */
public interface BrReceiveMapper
{
    public BrReceive selectById(@Param("id") Long id);

    public List<BrReceive> selectList(BrReceive receive);

    public BrReceive selectByReceiveNo(@Param("receiveNo") String receiveNo);

    /** 按receiveNo查询（含软删除行，用于编号占用校验的友好提示） */
    public BrReceive selectByReceiveNoAny(@Param("receiveNo") String receiveNo);

    /** 按前缀查最大收料单号（自动编号流水用，含逻辑删除行避免流水撞号） */
    public String selectMaxReceiveNoByPrefix(@Param("prefix") String prefix);

    public int insert(BrReceive receive);

    public int update(BrReceive receive);

    public int deleteByIds(@Param("ids") Long[] ids);

    public List<BrReceiveItem> selectItemsByReceiveId(@Param("receiveId") Long receiveId);

    public int insertItem(BrReceiveItem item);

    public int deleteItemsByReceiveId(@Param("receiveId") Long receiveId);

    public int updateDeclareResult(@Param("id") Long id, @Param("uuid") String uuid,
            @Param("docStatus") String docStatus, @Param("customsStatus") String customsStatus,
            @Param("executeType") String executeType);
}
