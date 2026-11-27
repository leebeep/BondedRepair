package com.ruoyi.bonded.mapper;

import java.util.Date;
import org.apache.ibatis.annotations.Param;

/**
 * 镜像表回执回写 Mapper（主动查询 API 覆盖回执字段，按 uuid 定位）
 */
public interface CustomsMirrorMapper
{
    public int updateReceiptEpd101(@Param("uuid") String uuid, @Param("resultStatus") String resultStatus,
            @Param("msg") String msg, @Param("resultTime") Date resultTime);

    public int updateReceiptPto2(@Param("uuid") String uuid, @Param("resultStatus") String resultStatus,
            @Param("msg") String msg, @Param("resultTime") Date resultTime);

    public int updateReceiptRrd2(@Param("uuid") String uuid, @Param("resultStatus") String resultStatus,
            @Param("msg") String msg, @Param("resultTime") Date resultTime);

    public int updateReceiptMso2(@Param("uuid") String uuid, @Param("resultStatus") String resultStatus,
            @Param("msg") String msg, @Param("resultTime") Date resultTime);

    public int updateReceiptSdo2(@Param("uuid") String uuid, @Param("resultStatus") String resultStatus,
            @Param("msg") String msg, @Param("resultTime") Date resultTime);
}
