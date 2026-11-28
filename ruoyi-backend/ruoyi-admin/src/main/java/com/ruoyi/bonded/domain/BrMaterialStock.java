package com.ruoyi.bonded.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.ruoyi.common.annotation.Excel;
import lombok.Data;

/**
 * 物料库存余额 br_material_stock（物料为唯一维度，唯一库存事实）
 */
@Data
public class BrMaterialStock
{
    private Long materialId;
    /** 冗余：货物类型(六类标记) */
    private String markType;
    @Excel(name = "当前结存")
    private BigDecimal qty;
    @Excel(name = "最近变动时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastBizTime;
    private Date updateTime;

    /** 关联物料基本信息（联查 br_material，非本表字段） */
    @Excel(name = "物料编号")
    private String materialCd;
    private String version;
    @Excel(name = "物料名称")
    private String materialName;
    @Excel(name = "规格型号")
    private String gModel;
    @Excel(name = "HS编码")
    private String codeTs;
    private String calcUnit;
    @Excel(name = "是否保税")
    private String isBonded;
    private String declareStatus;

    /** 查询用：最近变动时间范围 */
    private Date beginTime;
    private Date endTime;

    /** 查询辅助：物料属性/保税标志多选（列表头筛选） */
    private List<String> markTypeList;
    private List<String> isBondedList;
}
