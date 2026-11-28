package com.ruoyi.bonded.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.bonded.domain.BrIssueItem;
import com.ruoyi.bonded.domain.BrMaterial;
import com.ruoyi.bonded.domain.BrWorkOrder;
import com.ruoyi.bonded.domain.BrWorkOrderItem;
import com.ruoyi.bonded.mapper.BrMaterialMapper;
import com.ruoyi.bonded.mapper.BrWorkOrderMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 维修工单内部支撑服务（非海关单证）。
 * 工单号/批次号/服务费由发料单、收料单表单直接录入，本服务负责按工单号自动建档/更新，
 * 供 RRD2/MSO2 报文 work_no、维修服务费行与收齐校验使用。不再提供独立页面与 REST 接口。
 */
@Service
public class BrWorkOrderService
{
    @Autowired
    private BrWorkOrderMapper workOrderMapper;

    /** 按工单号查工单（只读，供收料表单带出服务费；不存在返回 null） */
    public BrWorkOrder findByWorkNo(String workNo)
    {
        if (StringUtils.isEmpty(workNo)) { return null; }
        return workOrderMapper.selectByWorkNo(workNo);
    }

    @Autowired
    private BrMaterialMapper materialMapper;

    /**
     * 按工单号取工单，不存在则自动建档（order_status=01 立项）；
     * 存在时若表单携带批次号/服务费/币制则更新。
     */
    @Transactional(rollbackFor = Exception.class)
    public BrWorkOrder getOrCreate(String workNo, String batchNo, BigDecimal serviceFee, String currency)
    {
        if (StringUtils.isEmpty(workNo)) { throw new ServiceException("工单号必填"); }
        BrWorkOrder wo = workOrderMapper.selectByWorkNo(workNo);
        if (wo == null)
        {
            wo = new BrWorkOrder();
            wo.setWorkNo(workNo);
            wo.setBatchNo(batchNo);
            wo.setServiceFee(serviceFee);
            wo.setCurrency(currency);
            wo.setCreateBy(SecurityUtils.getUsername());
            workOrderMapper.insert(wo);
            return wo;
        }
        boolean changed = false;
        if (StringUtils.isNotEmpty(batchNo) && !batchNo.equals(wo.getBatchNo())) { wo.setBatchNo(batchNo); changed = true; }
        if (serviceFee != null && serviceFee.compareTo(wo.getServiceFee() == null ? BigDecimal.ZERO : wo.getServiceFee()) != 0) { wo.setServiceFee(serviceFee); changed = true; }
        if (StringUtils.isNotEmpty(currency) && !currency.equals(wo.getCurrency())) { wo.setCurrency(currency); changed = true; }
        if (changed)
        {
            BrWorkOrder upd = new BrWorkOrder();
            upd.setId(wo.getId());
            upd.setBatchNo(wo.getBatchNo());
            upd.setServiceFee(wo.getServiceFee());
            upd.setCurrency(wo.getCurrency());
            upd.setUpdateBy(SecurityUtils.getUsername());
            workOrderMapper.update(upd);
        }
        return wo;
    }

    /** 发料单保存后，将其中待维修货物(markType=1)同步为工单投入明细（供服务费行与收齐校验使用） */
    @Transactional(rollbackFor = Exception.class)
    public void syncRepairItems(BrWorkOrder wo, List<BrIssueItem> issueItems)
    {
        if (wo == null || issueItems == null) { return; }
        List<BrWorkOrderItem> existing = workOrderMapper.selectItemsByWorkOrderId(wo.getId());
        for (BrIssueItem item : issueItems)
        {
            BrMaterial m = materialMapper.selectBrMaterialById(item.getMaterialId());
            if (m == null || !"1".equals(m.getMarkType())) { continue; }
            boolean exists = existing.stream().anyMatch(i -> i.getMaterialId().equals(item.getMaterialId()));
            if (exists) { continue; }
            BrWorkOrderItem wi = new BrWorkOrderItem();
            wi.setWorkOrderId(wo.getId());
            wi.setMaterialId(item.getMaterialId());
            wi.setPlanQty(item.getIssueQty());
            workOrderMapper.insertItem(wi);
        }
    }

    /**
     * 按发料记录重建工单投入明细：删除旧明细后，以该工单全部发料的待维修货物(markType=1)
     * 按物料汇总（发料数量合计）重新生成，保证工单明细与发料实际记录一致。
     */
    @Transactional(rollbackFor = Exception.class)
    public void rebuildRepairItems(Long workOrderId)
    {
        if (workOrderId == null) { return; }
        List<BrWorkOrderItem> summary = workOrderMapper.selectRepairItemSummary(workOrderId);
        workOrderMapper.deleteItemsByWorkOrderId(workOrderId);
        for (BrWorkOrderItem wi : summary)
        {
            wi.setWorkOrderId(workOrderId);
            workOrderMapper.insertItem(wi);
        }
    }

    /**
     * 出清校验：工单须出区收料（5/a/1/7/8/9，余料 6 结转除外）全部出库（03/05）→ 转 04 已出清；
     * 已出清/已结案不回退。
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshOutClearStatus(Long workOrderId)
    {
        BrWorkOrder wo = workOrderMapper.selectById(workOrderId);
        if (wo == null) { return; }
        String st = wo.getOrderStatus();
        if ("04".equals(st) || "05".equals(st)) { return; }
        BigDecimal toClear = workOrderMapper.sumToClearQty(wo.getWorkNo());
        BigDecimal out = workOrderMapper.sumOutboundQty(wo.getWorkNo());
        if (toClear.compareTo(BigDecimal.ZERO) > 0 && out.compareTo(toClear) >= 0)
        {
            workOrderMapper.updateOrderStatus(workOrderId, "04");
        }
        else if ("04".equals(st))
        {
            // 出库单作废冲账后出清不再成立：按发料/收料实际重建（01/02/03）
            BigDecimal issued = workOrderMapper.sumIssuedRepairQty(wo.getWorkNo());
            String next;
            if (issued.compareTo(BigDecimal.ZERO) <= 0) { next = "01"; }
            else
            {
                BigDecimal received = workOrderMapper.sumReceivedRepairQty(wo.getWorkNo());
                BigDecimal unused = workOrderMapper.sumUnusedRepairQty(wo.getWorkNo());
                next = received.compareTo(issued.subtract(unused)) >= 0 ? "03" : "02";
            }
            workOrderMapper.updateOrderStatus(workOrderId, next);
        }
    }

    /**
     * 按发料/收料实际情况重建工单状态（发料草稿删除等场景）：
     * 无发料 → 01 立项；收齐（已修复+残次品 >= 实际投入）→ 03；其余 → 02 生产中。不降级 04/05。
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshWorkOrderStatus(Long workOrderId)
    {
        BrWorkOrder wo = workOrderMapper.selectById(workOrderId);
        if (wo == null) { return; }
        String st = wo.getOrderStatus();
        if ("04".equals(st) || "05".equals(st)) { return; }
        BigDecimal issued = workOrderMapper.sumIssuedRepairQty(wo.getWorkNo());
        if (issued.compareTo(BigDecimal.ZERO) <= 0)
        {
            workOrderMapper.updateOrderStatus(workOrderId, "01");
            return;
        }
        BigDecimal received = workOrderMapper.sumReceivedRepairQty(wo.getWorkNo());
        BigDecimal unused = workOrderMapper.sumUnusedRepairQty(wo.getWorkNo());
        String next = received.compareTo(issued.subtract(unused)) >= 0 ? "03" : "02";
        if (!next.equals(st)) { workOrderMapper.updateOrderStatus(workOrderId, next); }
    }
}
