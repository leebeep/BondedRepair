package com.ruoyi.bonded.service;

import java.util.List;
import com.ruoyi.bonded.domain.BrMaterial;

/**
 * 物料清单服务接口
 */
public interface IBrMaterialService
{
    public List<BrMaterial> selectBrMaterialList(BrMaterial material);

    public BrMaterial selectBrMaterialById(Long id);

    public int insertBrMaterial(BrMaterial material);

    public int updateBrMaterial(BrMaterial material);

    public int deleteBrMaterialByIds(Long[] ids);

    /**
     * 提交 EPD101 备案申报（事务内写镜像表 epd101_data + 申报日志）
     *
     * @param ids 物料 id 集合
     * @param executeType 1-备案 2-变更 3-作废
     */
    public int declareEPD101(Long[] ids, String executeType);
}
