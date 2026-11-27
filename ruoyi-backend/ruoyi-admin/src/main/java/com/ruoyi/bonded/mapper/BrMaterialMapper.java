package com.ruoyi.bonded.mapper;

import java.util.List;
import com.ruoyi.bonded.domain.BrMaterial;
import org.apache.ibatis.annotations.Param;

/**
 * 物料主数据 Mapper
 */
public interface BrMaterialMapper
{
    public BrMaterial selectBrMaterialById(Long id);

    public BrMaterial selectBrMaterialByBizKey(BrMaterial material);

    public List<BrMaterial> selectBrMaterialList(BrMaterial material);

    public int insertBrMaterial(BrMaterial material);

    public int updateBrMaterial(BrMaterial material);

    public int deleteBrMaterialByIds(@Param("ids") Long[] ids);

    /** 备案状态回写 */
    public int updateDeclareResult(@Param("id") Long id, @Param("uuid") String uuid,
            @Param("declareStatus") String declareStatus, @Param("customsStatus") String customsStatus,
            @Param("executeType") String executeType);

    /** 采集超时：待采集超 24 小时置 90 */
    public int markCollectTimeout(@Param("hours") int hours);
}
