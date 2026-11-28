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
    @Autowired private com.ruoyi.bonded.service.BrDeclareSupport declareSupport;
    @Autowired private BrWorkOrderService workOrderService;
    @Autowired private BrPushSwitch pushSwitch;

    /** 发料单号自动生成前缀参数（sys_config，可在 参数设置 中修改） */
    public static final String ISSUE_NO_PREFIX_KEY = "br.issue.no.prefix";
    public static final String DEFAULT_ISSUE_NO_PREFIX = "IS";

    @Autowired private com.ruoyi.system.service.ISysConfigService configService;

    @Override
    public String nextIssueNo()
    {
        String prefix = configService.selectConfigByKey(ISSUE_NO_PREFIX_KEY);
        if (prefix == null || prefix.isEmpty()) { prefix = DEFAULT_ISSUE_NO_PREFIX; }
        String head = prefix + new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        String max = issueMapper.selectMaxIssueNoByPrefix(head);
        int seq = 1;
        if (max != null && max.length() > head.length())
        {
            try { seq = Integer.parseInt(max.substring(head.length())) + 1; }
            catch (NumberFormatException ignored) { }
        }
        return head + String.format("%03d", seq);
    }

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
        // 工单先行：必须选择已创建的维修工单（供 RRD2 work_no 与收齐校验）
        com.ruoyi.bonded.domain.BrWorkOrder wo = workOrderService.requireByWorkNo(issue.getWorkNo());
        issue.setWorkOrderId(wo.getId());
        issue.setCreateBy(SecurityUtils.getUsername());
        issueMapper.insert(issue);
        for (BrIssueItem item : issue.getItems())
        {
            item.setIssueId(issue.getId());
            issueMapper.insertItem(item);
        }
        workOrderService.rebuildRepairItems(wo.getId());
        workOrderMapper.updateOrderStatus(wo.getId(), "02");
        // 保存即生效（小企业单人作业，无独立审核环节）：同事务校验来源余额、登记台账并按开关写镜像库，失败整体回滚
        audit(issue.getId());
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(BrIssue issue)
    {
        BrIssue db = issueMapper.selectById(issue.getId());
        if (db == null) { throw new ServiceException("发料单不存在"); }
        // 未采集可修改：草稿(01)、退单(06)、生效未申报(03/00)、待采集(03/10，客户端未取走镜像行)
        boolean editable = "01".equals(db.getDocStatus()) || "06".equals(db.getDocStatus())
                || ("03".equals(db.getDocStatus()) && ("00".equals(db.getCustomsStatus()) || "10".equals(db.getCustomsStatus())));
        if (!editable) { throw new ServiceException("发料单【" + db.getIssueNo() + "】已被海关采集，不可直接修改，请走变更申报流程"); }
        // 原 03 生效单修改：先冲销台账、清理未采集镜像行与待回执日志，按草稿口径重走审核并重写镜像
        if ("03".equals(db.getDocStatus()))
        {
            ledgerService.reverseDoc("br_issue", issue.getId());
            declareSupport.purgePending("br_issue", issue.getId(), db.getIssueNo());
            issueMapper.updateDeclareResult(issue.getId(), null, "01", "00", "1");
        }
        validate(issue, issue.getId());
        if (StringUtils.isNotEmpty(issue.getWorkNo()))
        {
            com.ruoyi.bonded.domain.BrWorkOrder wo = workOrderService.requireByWorkNo(issue.getWorkNo());
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
        // 投入明细按发料记录重建；工单号变更时旧工单一并重建，保证两侧明细一致
        if (db.getWorkOrderId() != null && !db.getWorkOrderId().equals(issue.getWorkOrderId()))
        {
            workOrderService.rebuildRepairItems(db.getWorkOrderId());
        }
        workOrderService.rebuildRepairItems(issue.getWorkOrderId());
        if ("06".equals(db.getDocStatus()))
        {
            issueMapper.updateDeclareResult(issue.getId(), db.getLastUuid(), "01", "00", db.getExecuteType());
        }
        audit(issue.getId());
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(Long[] ids)
    {
        java.util.Set<Long> woIds = new java.util.HashSet<>();
        for (Long id : ids)
        {
            BrIssue db = issueMapper.selectById(id);
            if (db == null) { continue; }
            // 可删除：草稿(01) 或 生效未申报(03/00，镜像表未写入)；已写镜像走变更/作废申报
            boolean deletable = "01".equals(db.getDocStatus())
                    || ("03".equals(db.getDocStatus()) && "00".equals(db.getCustomsStatus()));
            if (!deletable) { throw new ServiceException("发料单【" + db.getIssueNo() + "】已写入海关镜像库，禁止删除，请走变更或作废申报"); }
            if (db.getWorkOrderId() != null) { woIds.add(db.getWorkOrderId()); }
        }
        // 生效单已登记台账：先冲销再删除
        for (Long id : ids)
        {
            BrIssue db = issueMapper.selectById(id);
            if (db != null && "03".equals(db.getDocStatus())) { ledgerService.reverseDoc("br_issue", id); }
        }
        int rows = issueMapper.deleteByIds(ids);
        // 删除后同步重建工单投入明细并按剩余发料/收料重建工单状态（避免删除后状态滞留 02）
        for (Long woId : woIds)
        {
            workOrderService.rebuildRepairItems(woId);
            workOrderService.refreshWorkOrderStatus(woId);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int audit(Long id)
    {
        BrIssue h = issueMapper.selectById(id);
        if (h == null) { throw new ServiceException("发料单不存在"); }
        if (!"01".equals(h.getDocStatus())) { throw new ServiceException("发料单【" + h.getIssueNo() + "】已审核"); }
        // 退单(06)重审：先冲销原 ISSUE 流水（数量取反），再按当前明细重新记账，避免重复入账
        if ("06".equals(h.getDocStatus()))
        {
            ledgerService.reverseDoc("br_issue", id);
        }
        BrWorkOrder wo = workOrderMapper.selectById(h.getWorkOrderId());
        List<BrIssueItem> items = issueMapper.selectItemsByIssueId(id);
        if (items.isEmpty()) { throw new ServiceException("发料单明细为空"); }
        for (BrIssueItem item : items)
        {
            BrMaterial m = materialMapper.selectBrMaterialById(item.getMaterialId());
            if (m == null) { throw new ServiceException("发料明细物料不存在: " + item.getMaterialId()); }
            BigDecimal available = ledgerService.sourceBalance(item.getMaterialId(), item.getGoodsSourceType(), item.getGoodsSourceNo());
            if (available.compareTo(item.getIssueQty()) < 0)
            {
                throw new ServiceException("物料【" + m.getMaterialName() + "】在来源单据 " + item.getGoodsSourceNo()
                        + " 可发余额不足：可用 " + available + "，本次发料 " + item.getIssueQty());
            }
            ledgerService.record(item.getMaterialId(), m.getMarkType(), "ISSUE", "br_issue", id, h.getIssueNo(),
                    item.getGoodsSourceType(), item.getGoodsSourceNo(), wo.getWorkNo(), wo.getBatchNo(),
                    item.getIssueQty().negate());
        }
        // 审核通过；自动申报开关打开时同事务写入镜像库，关闭则滞留待申报由补报任务处理
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
                        // 执行策略联动：变更(2)/作废(3)仅限 申报中/申报通过/退单；在途(申报中未回执)禁止任何申报，避免报文叠写
            if ("03".equals(h.getDocStatus()) && ("10".equals(h.getCustomsStatus()) || "20".equals(h.getCustomsStatus()) || "21".equals(h.getCustomsStatus())))
            {
                throw new ServiceException("单据【" + h.getIssueNo() + "】申报回执未到（海关状态：" + h.getCustomsStatus() + "），请等待回执后再申报");
            }
            // 执行策略联动：已申报过（有预录入编号）只能变更或作废，不允许再次备案
            if ("1".equals(t) && StringUtils.isNotEmpty(h.getLastUuid()))
            {
                throw new ServiceException("发料单【" + h.getIssueNo() + "】已申报（预录入编号：" + h.getLastUuid() + "），只能变更或作废，不允许再次备案");
            }
            if ("2".equals(t) && !"03".equals(h.getDocStatus()) && !"05".equals(h.getDocStatus()) && !"06".equals(h.getDocStatus()))
            {
                throw new ServiceException("单据【" + h.getIssueNo() + "】仅申报中/申报通过/退单状态可变更（已作废单据不可变更）");
            }
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
                d.setMaterialDesc(BrMaterialServiceImpl.mirrorName(m.getMaterialName(), m.getMarkType(), m.getIsBonded()));
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
                log.setBizNo(h.getIssueNo());
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
        if (exist == null && issueMapper.selectByIssueNoAny(issue.getIssueNo()) != null)
        {
            throw new ServiceException("发料单号已被占用（含已删除单据，编号不可复用）: " + issue.getIssueNo());
        }
        if (StringUtils.isEmpty(issue.getWorkNo()) && issue.getWorkOrderId() == null) { throw new ServiceException("工单号必填"); }
        if (issue.getItems() == null || issue.getItems().isEmpty()) { throw new ServiceException("发料明细不能为空"); }
        boolean hasRepairGoods = false;
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
            if ("1".equals(m.getMarkType())) { hasRepairGoods = true; }
        }
        // 待维修货物必须随工单发料：至少一行待维修货物(markType=1)
        if (!hasRepairGoods)
        {
            throw new ServiceException("工单发料必须包含至少一行待维修货物（料标记=1）");
        }
    }

    static Integer parseVersion(String version)
    {
        return BrInboundServiceImpl.parseVersion(version, null);
    }

    /** 草稿保存软校验：来源余额不足时返回提示（不阻断保存，审核时硬拦截） */
    @Override
    public String draftWarning(BrIssue issue)
    {
        if (issue.getItems() == null) { return ""; }
        StringBuilder warn = new StringBuilder();
        for (BrIssueItem item : issue.getItems())
        {
            BrMaterial m = materialMapper.selectBrMaterialById(item.getMaterialId());
            if (m == null || item.getIssueQty() == null) { continue; }
            BigDecimal available = ledgerService.sourceBalance(item.getMaterialId(), item.getGoodsSourceType(), item.getGoodsSourceNo());
            if (available.compareTo(item.getIssueQty()) < 0)
            {
                if (warn.length() > 0) { warn.append("；"); }
                warn.append(m.getMaterialName()).append(" 来源 ").append(item.getGoodsSourceNo())
                        .append(" 可用 ").append(available).append("，本次发料 ").append(item.getIssueQty()).append("（审核时将拦截）");
            }
        }
        return warn.toString();
    }
}
