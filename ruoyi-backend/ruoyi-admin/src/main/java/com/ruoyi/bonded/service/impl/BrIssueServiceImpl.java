package com.ruoyi.bonded.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.bonded.domain.BrDeclareLog;
import com.ruoyi.bonded.domain.BrIssue;
import com.ruoyi.bonded.domain.BrIssueItem;
import com.ruoyi.bonded.domain.BrMaterial;
import com.ruoyi.bonded.domain.BrWorkOrder;
import com.ruoyi.bonded.domain.Rrd2Data;
import com.ruoyi.bonded.mapper.BrDeclareLogMapper;
import com.ruoyi.bonded.mapper.BrIssueMapper;
import com.ruoyi.bonded.mapper.BrMaterialMapper;
import com.ruoyi.bonded.mapper.BrWorkOrderMapper;
import com.ruoyi.bonded.mapper.Rrd2DataMapper;
import com.ruoyi.bonded.service.BrLedgerService;
import com.ruoyi.bonded.service.BrPushSwitch;
import com.ruoyi.bonded.service.BrWorkOrderService;
import com.ruoyi.bonded.service.IBrIssueService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 工单发料服务实现（RRD2）
 */
@Service
public class BrIssueServiceImpl implements IBrIssueService
{
    @Autowired private BrIssueMapper issueMapper;
    @Autowired private BrWorkOrderMapper workOrderMapper;
    @Autowired private BrMaterialMapper materialMapper;
    @Autowired private Rrd2DataMapper rrd2DataMapper;
    @Autowired private BrDeclareLogMapper declareLogMapper;
    @Autowired private BrLedgerService ledgerService;
    @Autowired private BrWorkOrderService workOrderService;
    @Autowired private BrPushSwitch pushSwitch;

    @Override
    public List<BrIssue> selectList(BrIssue issue)
    {
        List<BrIssue> list = issueMapper.selectList(issue);
        for (BrIssue h : list) { h.setItems(issueMapper.selectItemsByIssueId(h.getId())); }
        return list;
    }

    @Override
    public BrIssue selectById(Long id)
    {
        BrIssue h = issueMapper.selectById(id);
        if (h != null) { h.setItems(issueMapper.selectItemsByIssueId(id)); }
        return h;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(BrIssue issue)
    {
        validate(issue, null);
        // 工单号由表单录入：不存在自动建档（批次号一并维护），非海关单证、仅供 RRD2 work_no 与收齐校验
        com.ruoyi.bonded.domain.BrWorkOrder wo = workOrderService.getOrCreate(issue.getWorkNo(), issue.getBatchNo(), null, null);
        issue.setWorkOrderId(wo.getId());
        issue.setCreateBy(SecurityUtils.getUsername());
        issueMapper.insert(issue);
        for (BrIssueItem item : issue.getItems())
        {
            item.setIssueId(issue.getId());
            issueMapper.insertItem(item);
        }
        workOrderService.syncRepairItems(wo, issue.getItems());
        workOrderMapper.updateOrderStatus(wo.getId(), "02");
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(BrIssue issue)
    {
        BrIssue db = issueMapper.selectById(issue.getId());
        if (db == null) { throw new ServiceException("发料单不存在"); }
        if (!"01".equals(db.getDocStatus()) && !"06".equals(db.getDocStatus())) { throw new ServiceException("仅草稿或退单状态可修改"); }
        validate(issue, issue.getId());
        if (StringUtils.isNotEmpty(issue.getWorkNo()))
        {
            com.ruoyi.bonded.domain.BrWorkOrder wo = workOrderService.getOrCreate(issue.getWorkNo(), issue.getBatchNo(), null, null);
            issue.setWorkOrderId(wo.getId());
        }
        issue.setUpdateBy(SecurityUtils.getUsername());
        issueMapper.update(issue);
        issueMapper.deleteItemsByIssueId(issue.getId());
        for (BrIssueItem item : issue.getItems())
        {
            item.setIssueId(issue.getId());
            issueMapper.insertItem(item);
        }
        if ("06".equals(db.getDocStatus()))
        {
            issueMapper.updateDeclareResult(issue.getId(), db.getLastUuid(), "01", "00", db.getExecuteType());
        }
        return 1;
    }

    @Override
    public int deleteByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            BrIssue db = issueMapper.selectById(id);
            if (db != null && !"01".equals(db.getDocStatus())) { throw new ServiceException("发料单【" + db.getIssueNo() + "】非草稿状态，禁止删除"); }
        }
        return issueMapper.deleteByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int audit(Long id)
    {
        BrIssue h = issueMapper.selectById(id);
        if (h == null) { throw new ServiceException("发料单不存在"); }
        if (!"01".equals(h.getDocStatus()) && !"02".equals(h.getDocStatus())) { throw new ServiceException("发料单【" + h.getIssueNo() + "】已审核"); }
        BrWorkOrder wo = workOrderMapper.selectById(h.getWorkOrderId());
        List<BrIssueItem> items = issueMapper.selectItemsByIssueId(id);
        if (items.isEmpty()) { throw new ServiceException("发料单明细为空"); }
        for (BrIssueItem item : items)
        {
            BrMaterial m = materialMapper.selectBrMaterialById(item.getMaterialId());
            BigDecimal available = ledgerService.sourceBalance(item.getMaterialId(), item.getGoodsSourceType(), item.getGoodsSourceNo());
            if (available.compareTo(item.getIssueQty()) < 0)
            {
                throw new ServiceException("物料【" + m.getMaterialName() + "】在来源单据 " + item.getGoodsSourceNo()
                        + " 可发余额不足：可用 " + available + "，本次发料 " + item.getIssueQty());
            }
            ledgerService.record(item.getMaterialId(), m.getMarkType(), "ISSUE", "br_issue", id,
                    item.getGoodsSourceType(), item.getGoodsSourceNo(), wo.getWorkNo(), wo.getBatchNo(),
                    item.getIssueQty().negate());
        }
        // 审核通过；自动推送开关打开时同事务写入镜像库，关闭则滞留待同步由补推任务处理
        int rows = issueMapper.updateDeclareResult(id, h.getLastUuid(), "03", h.getCustomsStatus(), "1");
        if (pushSwitch.isEnabled())
        {
            declare(new Long[] { id }, "A");
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int declare(Long[] ids, String executeType)
    {
        for (Long id : ids)
        {
            BrIssue h = issueMapper.selectById(id);
            if (h == null) { throw new ServiceException("发料单不存在"); }
            // A=自动判定：已申报过（有预录入编号）走变更(2)，否则首报(1)
            String t = executeType;
            if ("A".equals(t)) { t = StringUtils.isNotEmpty(h.getLastUuid()) ? "2" : "1"; }
            if ("1".equals(t) && !"03".equals(h.getDocStatus()))
            {
                throw new ServiceException("发料单【" + h.getIssueNo() + "】未审核通过，不能申报");
            }
            if ("2".equals(t) && StringUtils.isEmpty(h.getLastUuid()))
            {
                throw new ServiceException("发料单【" + h.getIssueNo() + "】未申报过，不能变更");
            }
            if ("3".equals(t) && !"03".equals(h.getDocStatus()) && !"05".equals(h.getDocStatus()) && !"06".equals(h.getDocStatus()))
            {
                throw new ServiceException("发料单【" + h.getIssueNo() + "】仅申报中/申报通过/退单状态可作废");
            }
            // 变更/作废（executeType=2/3）必须携带原预录入编号（规范 §2.2.x）
            String lastUuid = "1".equals(t) ? null : h.getLastUuid();
            BrWorkOrder wo = workOrderMapper.selectById(h.getWorkOrderId());
            for (BrIssueItem item : issueMapper.selectItemsByIssueId(id))
            {
                BrMaterial m = materialMapper.selectBrMaterialById(item.getMaterialId());
                if (m == null || !"2".equals(m.getDeclareStatus()))
                {
                    throw new ServiceException("明细物料未备案通过，不能申报");
                }
                Rrd2Data d = new Rrd2Data();
                d.setExecuteType(t);
                d.setFinishMaterialNum(h.getIssueNo());
                d.setFinishMaterialTm(h.getIssueTime());
                d.setWorkNo(wo.getWorkNo());
                d.setGoodsSourceType(item.getGoodsSourceType());
                d.setGoodsSourceNo(item.getGoodsSourceNo());
                d.setPrice(item.getPrice());
                d.setMaterialCd(m.getMaterialCd());
                d.setMaterialDesc(m.getMaterialName() + BrMaterialServiceImpl.markSuffix(m.getMarkType()));
                d.setMaterialVersion(BrInboundServiceImpl.parseVersion(m.getVersion(), m.getMaterialName()));
                d.setIssueMaterialQty(item.getIssueQty());
                d.setCalcUnit(m.getCalcUnit());
                d.setIsBonded(m.getIsBonded());
                d.setUuid(lastUuid);
                rrd2DataMapper.insert(d);

                BrDeclareLog log = new BrDeclareLog();
                log.setBizType("RRD2");
                log.setBizTable("br_issue");
                log.setBizId(id);
                log.setMirrorTable("rrd2_data");
                log.setMirrorRowId(d.getId());
                log.setExecuteType(t);
                log.setUuid(StringUtils.defaultString(lastUuid, ""));
                declareLogMapper.insertBrDeclareLog(log);
            }
            // 写入镜像表即申报：03=已同步待回执（04 废弃）
            issueMapper.updateDeclareResult(id, lastUuid, "03", "10", t);
        }
        return ids.length;
    }

    private void validate(BrIssue issue, Long selfId)
    {
        if (StringUtils.isEmpty(issue.getIssueNo())) { throw new ServiceException("发料单号必填"); }
        BrIssue exist = issueMapper.selectByIssueNo(issue.getIssueNo());
        if (exist != null && !exist.getId().equals(selfId)) { throw new ServiceException("发料单号已存在: " + issue.getIssueNo()); }
        if (StringUtils.isEmpty(issue.getWorkNo()) && issue.getWorkOrderId() == null) { throw new ServiceException("工单号必填"); }
        if (issue.getItems() == null || issue.getItems().isEmpty()) { throw new ServiceException("发料明细不能为空"); }
        for (BrIssueItem item : issue.getItems())
        {
            if (!"g".equals(item.getGoodsSourceType()) && !"i".equals(item.getGoodsSourceType()))
            {
                throw new ServiceException("货物来源单证类型必须为 g(入库清单) 或 i(工单收料)");
            }
            if (StringUtils.isEmpty(item.getGoodsSourceNo())) { throw new ServiceException("货物来源单证编号必填"); }
            if (item.getIssueQty() == null || item.getIssueQty().signum() <= 0) { throw new ServiceException("发料数量必须大于 0"); }
            BrMaterial m = materialMapper.selectBrMaterialById(item.getMaterialId());
            if (m == null || !"2".equals(m.getDeclareStatus()))
            {
                throw new ServiceException("明细物料未备案通过");
            }
        }
    }

    static Integer parseVersion(String version)
    {
        return BrInboundServiceImpl.parseVersion(version, null);
    }
}
