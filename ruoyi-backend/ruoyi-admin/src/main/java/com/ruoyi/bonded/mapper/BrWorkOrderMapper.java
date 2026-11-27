package com.ruoyi.bonded.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.bonded.domain.BrWorkOrder;
import com.ruoyi.bonded.domain.BrWorkOrderItem;

/**
 * 维修工单 Mapper
 */
public interface BrWorkOrderMapper
{
    public BrWorkOrder selectById(@Param("id") Long id);

    public List<BrWorkOrder> selectList(BrWorkOrder workOrder);

    public BrWorkOrder selectByWorkNo(@Param("workNo") String workNo);

    public int insert(BrWorkOrder workOrder);

    public int update(BrWorkOrder workOrder);

    public int deleteByIds(@Param("ids") Long[] ids);

    public List<BrWorkOrderItem> selectItemsByWorkOrderId(@Param("workOrderId") Long workOrderId);

    public int insertItem(BrWorkOrderItem item);

    public int deleteItemsByWorkOrderId(@Param("workOrderId") Long workOrderId);

    /** 工单发料明细按物料汇总（仅待维修货物 markType=1，planQty=发料数量合计） */
    public List<BrWorkOrderItem> selectRepairItemSummary(@Param("workOrderId") Long workOrderId);

    /** 工单状态更新 */
    public int updateOrderStatus(@Param("id") Long id, @Param("orderStatus") String orderStatus);

    /** 工单已发料的待维修货物数量合计（实际投入） */
    public java.math.BigDecimal sumIssuedRepairQty(@Param("workNo") String workNo);

    /** 工单已收的已修复+残次品数量合计 */
    public java.math.BigDecimal sumReceivedRepairQty(@Param("workNo") String workNo);

    /** 工单未使用料件收料合计（returnType=1） */
    public java.math.BigDecimal sumUnusedRepairQty(@Param("workNo") String workNo);
}
