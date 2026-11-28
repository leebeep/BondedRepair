package com.ruoyi.bonded.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.bonded.domain.BrIssue;
import com.ruoyi.bonded.domain.BrIssueItem;
import com.ruoyi.bonded.domain.BrMaterial;
import com.ruoyi.bonded.domain.BrOutbound;
import com.ruoyi.bonded.domain.BrReceive;
import com.ruoyi.bonded.domain.BrReceiveItem;
import com.ruoyi.bonded.domain.BrWorkOrder;
import com.ruoyi.bonded.domain.BrWorkOrderItem;
import com.ruoyi.bonded.mapper.BrIssueMapper;
import com.ruoyi.bonded.mapper.BrMaterialMapper;
import com.ruoyi.bonded.mapper.BrOutboundMapper;
import com.ruoyi.bonded.mapper.BrReceiveMapper;
import com.ruoyi.bonded.mapper.BrWorkOrderMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 维修工单服务（非海关单证）。
 * 工单在"维修工单"页面先行创建（工单先行），发料/收料单从下拉中选择关联，
 * 本服务负责建档/生成工单号/收发料关联校验、投入明细重建与收齐/出清状态流转，
 * 供 RRD2/MSO2 报文 work_no、维修服务费行与收齐校验使用。
 */
@Service
public class BrWorkOrderService
{
    public static final String WO_NO_PREFIX_KEY = "br.workorder.no.prefix";
    public static final String DEFAULT_WO_NO_PREFIX = "WO";

    @Autowired
    private BrWorkOrderMapper workOrderMapper;

    @Autowired
    private BrMaterialMapper materialMapper;

    @Autowired
    private BrIssueMapper issueMapper;

    @Autowired
    private BrReceiveMapper receiveMapper;

    @Autowired
    private BrOutboundMapper outboundMapper;

    @Autowired
    private com.ruoyi.system.service.ISysConfigService configService;

    /** 按工单号查工单（只读，供收料表单带出服务费；不存在返回 null） */
    public BrWorkOrder findByWorkNo(String workNo)
    {
        if (StringUtils.isEmpty(workNo)) { return null; }
        return workOrderMapper.selectByWorkNo(workNo);
    }

    /** 按工单号取工单，不存在则报错（工单先行：发料/收料必须选择已创建的工单） */
    public BrWorkOrder requireByWorkNo(String workNo)
    {
        BrWorkOrder wo = findByWorkNo(workNo);
        if (wo == null) { throw new ServiceException("维修工单【" + workNo + "】不存在，请先在 维修工单 页面创建"); }
        return wo;
    }

    /** 下一张工单号（默认自动生成，前缀经 参数设置 br.workorder.no.prefix 修改） */
    public String nextWorkOrderNo()
    {
        String prefix = configService.selectConfigByKey(WO_NO_PREFIX_KEY);
        if (prefix == null || prefix.isEmpty()) { prefix = DEFAULT_WO_NO_PREFIX; }
        String head = prefix + new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        String max = workOrderMapper.selectMaxWorkNoByPrefix(head);
        int seq = 1;
        if (max != null && max.length() > head.length())
        {
            try { seq = Integer.parseInt(max.substring(head.length())) + 1; }
            catch (NumberFormatException ignored) { }
        }
        return head + String.format("%03d", seq);
    }

    // ---------- 工单管理 ----------

    public List<BrWorkOrder> selectList(BrWorkOrder workOrder)
    {
        return workOrderMapper.selectList(workOrder);
    }

    /** 未结案工单（01/02/03），发料/收料表单下拉 */
    public List<BrWorkOrder> selectOpenList()
    {
        return workOrderMapper.selectOpenList();
    }

    /** 新建工单（工单先行立项，状态 01） */
    @Transactional(rollbackFor = Exception.class)
    public int insertWorkOrder(BrWorkOrder wo)
    {
        if (StringUtils.isEmpty(wo.getWorkNo())) { throw new ServiceException("工单号必填"); }
        if (workOrderMapper.selectByWorkNoAny(wo.getWorkNo()) != null)
        {
            throw new ServiceException("工单号【" + wo.getWorkNo() + "】已被占用（含已删除工单，编号不可复用）");
        }
        wo.setCreateBy(SecurityUtils.getUsername());
        return workOrderMapper.insert(wo);
    }

    /** 编辑工单（仅计划完工日期/备注；工单号创建后不可改） */
    public int updateWorkOrder(BrWorkOrder wo)
    {
        BrWorkOrder db = workOrderMapper.selectById(wo.getId());
        if (db == null) { throw new ServiceException("工单不存在"); }
        BrWorkOrder upd = new BrWorkOrder();
        upd.setId(wo.getId());
        upd.setFinishDate(wo.getFinishDate());
        upd.setRemark(wo.getRemark());
        upd.setUpdateBy(SecurityUtils.getUsername());
        return workOrderMapper.update(upd);
    }

    /** 结案：仅已出清(04)可结案 → 05 */
    public int closeWorkOrder(Long id)
    {
        BrWorkOrder db = workOrderMapper.selectById(id);
        if (db == null) { throw new ServiceException("工单不存在"); }
        if (!"04".equals(db.getOrderStatus())) { throw new ServiceException("仅已出清(04)工单可结案"); }
        return workOrderMapper.updateOrderStatus(id, "05");
    }

    /** 删除工单：已关联发料/收料单的禁止删除 */
    public int deleteByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            BrWorkOrder db = workOrderMapper.selectById(id);
            if (db == null) { continue; }
            if (workOrderMapper.countLinkedDocs(id) > 0)
            {
                throw new ServiceException("工单【" + db.getWorkNo() + "】已关联发料/收料单，不可删除");
            }
        }
        return workOrderMapper.deleteByIds(ids);
    }

    /** 工单详情：工单+投入明细+关联发料/收料/出库单+勾稽汇总 */
    public Map<String, Object> selectDetail(Long id)
    {
        BrWorkOrder wo = workOrderMapper.selectById(id);
        if (wo == null) { throw new ServiceException("工单不存在"); }
        wo.setItems(workOrderMapper.selectItemsByWorkOrderId(id));

        List<BrIssue> issues = workOrderMapper.selectIssueHeadsByWorkOrderId(id);
        for (BrIssue i : issues) { i.setItems(issueMapper.selectItemsByIssueId(i.getId())); }
        List<BrReceive> receives = workOrderMapper.selectReceiveHeadsByWorkOrderId(id);
        for (BrReceive r : receives) { r.setItems(receiveMapper.selectItemsByReceiveId(r.getId())); }
        List<BrOutbound> outbounds = workOrderMapper.selectOutboundHeadsByWorkOrderId(id);

        Map<String, Object> sums = new HashMap<>();
        sums.put("issued", workOrderMapper.sumIssuedRepairQty(wo.getWorkNo()));
        sums.put("receivedRepair", workOrderMapper.sumReceivedRepairQty(wo.getWorkNo()));
        sums.put("unused", workOrderMapper.sumUnusedRepairQty(wo.getWorkNo()));
        sums.put("toClear", workOrderMapper.sumToClearQty(wo.getWorkNo()));
        sums.put("outbound", workOrderMapper.sumOutboundQty(wo.getWorkNo()));

        Map<String, Object> data = new HashMap<>();
        data.put("workOrder", wo);
        data.put("issues", issues);
        data.put("receives", receives);
        data.put("outbounds", outbounds);
        data.put("sums", sums);
        return data;
    }

    // ---------- 收发料关联 ----------

    /** 收料保存时按所选工单更新服务费/币制（工单必须已存在） */
    @Transactional(rollbackFor = Exception.class)
    public BrWorkOrder applyFee(String workNo, BigDecimal serviceFee, String currency)
    {
        BrWorkOrder wo = requireByWorkNo(workNo);
        boolean feeChanged = serviceFee != null && serviceFee.compareTo(wo.getServiceFee() == null ? BigDecimal.ZERO : wo.getServiceFee()) != 0;
        boolean curChanged = StringUtils.isNotEmpty(currency) && !currency.equals(wo.getCurrency());
        if (feeChanged || curChanged)
        {
            BrWorkOrder upd = new BrWorkOrder();
            upd.setId(wo.getId());
            if (serviceFee != null) { upd.setServiceFee(serviceFee); }
            if (StringUtils.isNotEmpty(currency)) { upd.setCurrency(currency); }
            upd.setUpdateBy(SecurityUtils.getUsername());
            workOrderMapper.update(upd);
            if (serviceFee != null) { wo.setServiceFee(serviceFee); }
            if (StringUtils.isNotEmpty(currency)) { wo.setCurrency(currency); }
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
     * 无发料 → 01 立项；收齐（已修复+残次品 >= 实际投入）→ 03；其余 → 02 维修中。不降级 04/05。
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
