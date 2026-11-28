package com.ruoyi.bonded.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.bonded.domain.*;
import com.ruoyi.bonded.mapper.*;
import com.ruoyi.bonded.service.BrLedgerService;
import com.ruoyi.bonded.service.BrPushSwitch;
import com.ruoyi.bonded.service.IBrOutboundService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

/** 出库单服务实现（SDO2）。规则：05 复运回境内区外必须带服务费库存行；出库数量不得超过来源余额。 */
@Service
public class BrOutboundServiceImpl implements IBrOutboundService
{
    @Autowired private BrOutboundMapper outboundMapper;
    @Autowired private BrMaterialMapper materialMapper;
    @Autowired private Sdo2DataMapper sdo2DataMapper;
    @Autowired private BrDeclareLogMapper declareLogMapper;
    @Autowired private BrLedgerService ledgerService;
    @Autowired private com.ruoyi.bonded.service.BrDeclareSupport declareSupport;
    @Autowired private BrPushSwitch pushSwitch;
    @Autowired private com.ruoyi.system.service.ISysConfigService configService;
    @Autowired private com.ruoyi.bonded.mapper.BrWorkOrderMapper workOrderMapper;
    @Autowired private com.ruoyi.bonded.service.BrWorkOrderService workOrderService;

    /** 出库单号自动生成前缀参数（sys_config，可在 参数设置 中修改） */
    public static final String OUTBOUND_NO_PREFIX_KEY = "br.outbound.no.prefix";
    public static final String DEFAULT_OUTBOUND_NO_PREFIX = "OUT";

    @Override
    public String nextOutboundNo()
    {
        String prefix = configService.selectConfigByKey(OUTBOUND_NO_PREFIX_KEY);
        if (prefix == null || prefix.isEmpty()) { prefix = DEFAULT_OUTBOUND_NO_PREFIX; }
        String head = prefix + new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        String max = outboundMapper.selectMaxOutboundNoByPrefix(head);
        int seq = 1;
        if (max != null && max.length() > head.length())
        {
            try { seq = Integer.parseInt(max.substring(head.length())) + 1; }
            catch (NumberFormatException ignored) { }
        }
        return head + String.format("%03d", seq);
    }

    @Override
    public List<BrOutbound> selectList(BrOutbound outbound)
    {
        List<BrOutbound> list = outboundMapper.selectList(outbound);
        for (BrOutbound h : list) { h.setItems(outboundMapper.selectItemsByOutboundId(h.getId())); }
        return list;
    }

    @Override
    public BrOutbound selectById(Long id)
    {
        BrOutbound h = outboundMapper.selectById(id);
        if (h != null) { h.setItems(outboundMapper.selectItemsByOutboundId(id)); }
        return h;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(BrOutbound outbound)
    {
        // 表单不再录入出库业务单证编号：空时默认=出库单号（同 PTO2 默认=入库单号先例，规范 §2.2.x）
        outbound.setBusiFormNum(StringUtils.defaultString(outbound.getBusiFormNum(), outbound.getOutboundNo()));
        validate(outbound, null);
        checkServiceFeeLine(outbound);
        outbound.setCreateBy(SecurityUtils.getUsername());
        outboundMapper.insert(outbound);
        for (BrOutboundItem item : outbound.getItems())
        {
            item.setOutboundId(outbound.getId());
            outboundMapper.insertItem(item);
        }
        // 保存即生效（小企业单人作业，无独立审核环节）：同事务余额校验、登记台账并按开关写镜像库，失败整体回滚
        audit(outbound.getId());
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(BrOutbound outbound)
    {
        BrOutbound db = outboundMapper.selectById(outbound.getId());
        if (db == null) { throw new ServiceException("出库单不存在"); }
        // 未采集可修改：草稿(01)、退单(06)、生效未申报(03/00)、待采集(03/10，客户端未取走镜像行)
        boolean editable = "01".equals(db.getDocStatus()) || "06".equals(db.getDocStatus())
                || ("03".equals(db.getDocStatus()) && ("00".equals(db.getCustomsStatus()) || "10".equals(db.getCustomsStatus())));
        if (!editable) { throw new ServiceException("出库单【" + db.getOutboundNo() + "】已被海关采集，不可直接修改，请走变更申报流程"); }
        // 原 03 生效单修改：先冲销台账、清理未采集镜像行与待回执日志，按草稿口径重走审核并重写镜像
        if ("03".equals(db.getDocStatus()))
        {
            ledgerService.reverseDoc("br_outbound", outbound.getId());
            declareSupport.purgePending("br_outbound", outbound.getId(), db.getOutboundNo());
            outboundMapper.updateDeclareResult(outbound.getId(), null, "01", "00", "1");
        }
        // 表单不再录入出库业务单证编号：空时默认=原出库单号（DB 列 NOT NULL，兜底旧数据）
        if (StringUtils.isEmpty(outbound.getBusiFormNum()))
        {
            outbound.setBusiFormNum(StringUtils.defaultString(outbound.getOutboundNo(), db.getOutboundNo()));
        }
        validate(outbound, outbound.getId());
        checkServiceFeeLine(outbound);
        outbound.setUpdateBy(SecurityUtils.getUsername());
        outboundMapper.update(outbound);
        outboundMapper.deleteItemsByOutboundId(outbound.getId());
        for (BrOutboundItem item : outbound.getItems())
        {
            item.setOutboundId(outbound.getId());
            outboundMapper.insertItem(item);
        }
        if ("06".equals(db.getDocStatus()))
        {
            outboundMapper.updateDeclareResult(outbound.getId(), db.getLastUuid(), "01", "00", db.getExecuteType());
        }
        audit(outbound.getId());
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(Long[] ids)
    {
        java.util.Set<Long> woIds = new java.util.HashSet<>();
        for (Long id : ids)
        {
            BrOutbound db = outboundMapper.selectById(id);
            if (db == null) { continue; }
            // 可删除：草稿(01) 或 生效未申报(03/00，镜像表未写入)；已写镜像走变更/作废申报
            boolean deletable = "01".equals(db.getDocStatus())
                    || ("03".equals(db.getDocStatus()) && "00".equals(db.getCustomsStatus()));
            if (!deletable) { throw new ServiceException("出库单【" + db.getOutboundNo() + "】已写入海关镜像库，禁止删除，请走变更或作废申报"); }
        }
        // 生效单已登记台账：先冲销再删除，并按剩余出库重建工单出清状态
        for (Long id : ids)
        {
            BrOutbound db = outboundMapper.selectById(id);
            if (db == null) { continue; }
            if ("03".equals(db.getDocStatus())) { ledgerService.reverseDoc("br_outbound", id); }
            for (BrOutboundItem item : outboundMapper.selectItemsByOutboundId(id))
            {
                if ("i".equals(item.getGoodsSourceType()) && StringUtils.isNotEmpty(item.getGoodsSourceNo()))
                {
                    Long woId = workOrderMapper.selectWorkOrderIdByReceiveNo(item.getGoodsSourceNo());
                    if (woId != null) { woIds.add(woId); }
                }
            }
        }
        int rows = outboundMapper.deleteByIds(ids);
        for (Long woId : woIds) { workOrderService.refreshOutClearStatus(woId); }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int audit(Long id)
    {
        BrOutbound h = outboundMapper.selectById(id);
        if (h == null) { throw new ServiceException("出库单不存在"); }
        if (!"01".equals(h.getDocStatus()) && !"06".equals(h.getDocStatus()))
        {
            throw new ServiceException("出库单当前状态不可审核");
        }
        // 退单(06)重审：先冲销原 OUT 流水（数量取反），再按当前明细重新记账，避免重复扣减库存
        if ("06".equals(h.getDocStatus()))
        {
            ledgerService.reverseDoc("br_outbound", id);
        }
        for (BrOutboundItem item : outboundMapper.selectItemsByOutboundId(id))
        {
            BrMaterial m = materialMapper.selectBrMaterialById(item.getMaterialId());
            if (m == null) { throw new ServiceException("出库明细物料不存在: " + item.getMaterialId()); }
            BigDecimal available = ledgerService.sourceBalance(item.getMaterialId(), item.getGoodsSourceType(), item.getGoodsSourceNo());
            BigDecimal out = item.getDeliveryCnt().abs();
            if (available.compareTo(out) < 0)
            {
                throw new ServiceException("物料【" + m.getMaterialName() + "】来源 " + item.getGoodsSourceNo()
                        + " 可出余额不足：可用 " + available + "，本次出库 " + out);
            }
            ledgerService.record(item.getMaterialId(), m.getMarkType(), "OUT", "br_outbound", id, h.getOutboundNo(),
                    item.getGoodsSourceType(), item.getGoodsSourceNo(), null, null, out.negate());
        }
        // 审核通过；自动申报开关打开时同事务写入镜像库（退单重报自动走变更），关闭则滞留待申报由补报任务处理
        int rows = outboundMapper.updateDeclareResult(id, h.getLastUuid(), "03", h.getCustomsStatus(), "1");
        if (pushSwitch.isEnabled())
        {
            declare(new Long[] { id }, "A");
        }
        // 出清推进：本单涉及的工单若须出区收料已全部出库（03/05）→ 04 已出清
        java.util.Set<Long> woIds = new java.util.HashSet<>();
        for (BrOutboundItem item : outboundMapper.selectItemsByOutboundId(id))
        {
            if ("i".equals(item.getGoodsSourceType()) && StringUtils.isNotEmpty(item.getGoodsSourceNo()))
            {
                Long woId = workOrderMapper.selectWorkOrderIdByReceiveNo(item.getGoodsSourceNo());
                if (woId != null) { woIds.add(woId); }
            }
        }
        for (Long woId : woIds) { workOrderService.refreshOutClearStatus(woId); }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int declare(Long[] ids, String executeType)
    {
        for (Long id : ids)
        {
            BrOutbound h = outboundMapper.selectById(id);
            if (h == null) { throw new ServiceException("出库单不存在"); }
            // A=自动判定：已申报过（有预录入编号）走变更(2)，否则首报(1)
            String t = executeType;
            if ("A".equals(t)) { t = StringUtils.isNotEmpty(h.getLastUuid()) ? "2" : "1"; }
                        // 执行策略联动：变更(2)/作废(3)仅限 申报中/申报通过/退单；在途(申报中未回执)禁止任何申报，避免报文叠写
            if ("03".equals(h.getDocStatus()) && ("10".equals(h.getCustomsStatus()) || "20".equals(h.getCustomsStatus()) || "21".equals(h.getCustomsStatus())))
            {
                throw new ServiceException("单据【" + h.getOutboundNo() + "】申报回执未到（海关状态：" + h.getCustomsStatus() + "），请等待回执后再申报");
            }
            // 执行策略联动：已申报过（有预录入编号）只能变更或作废，不允许再次备案
            if ("1".equals(t) && StringUtils.isNotEmpty(h.getLastUuid()))
            {
                throw new ServiceException("出库单【" + h.getOutboundNo() + "】已申报（预录入编号：" + h.getLastUuid() + "），只能变更或作废，不允许再次备案");
            }
            if ("2".equals(t) && !"03".equals(h.getDocStatus()) && !"05".equals(h.getDocStatus()) && !"06".equals(h.getDocStatus()))
            {
                throw new ServiceException("单据【" + h.getOutboundNo() + "】仅申报中/申报通过/退单状态可变更（已作废单据不可变更）");
            }
            if ("1".equals(t) && !"03".equals(h.getDocStatus()))
            {
                throw new ServiceException("出库单未审核通过，不能申报");
            }
            if ("2".equals(t) && StringUtils.isEmpty(h.getLastUuid()))
            {
                throw new ServiceException("出库单未申报过，不能变更");
            }
            if ("3".equals(t) && !"03".equals(h.getDocStatus()) && !"05".equals(h.getDocStatus()) && !"06".equals(h.getDocStatus()))
            {
                throw new ServiceException("出库单仅申报中/申报通过/退单状态可作废");
            }
            // 变更/作废（executeType=2/3）必须携带原预录入编号（规范 §2.2.x）
            String lastUuid = "1".equals(t) ? null : h.getLastUuid();
            for (BrOutboundItem item : outboundMapper.selectItemsByOutboundId(id))
            {
                BrMaterial m = materialMapper.selectBrMaterialById(item.getMaterialId());
                // 样例集 00 注2：服务费行(markType=8)按被维修商品口径申报
                BrMaterial line = m;
                if ("8".equals(m.getMarkType()))
                {
                    line = repairedMaterialOf(m);
                }
                else if (!"2".equals(m.getDeclareStatus()))
                {
                    throw new ServiceException("明细物料未备案通过，不能申报: " + m.getMaterialName());
                }
                String spec = StringUtils.defaultString(item.getSpecificationsModels(), line.getgModel());
                if (StringUtils.isEmpty(spec))
                {
                    throw new ServiceException("物料【" + line.getMaterialName() + "】规格型号必填（规范 §3.5.1）");
                }
                Sdo2Data d = new Sdo2Data();
                d.setExecuteType(t);
                d.setRelaEntNm(h.getRelaEntNm());
                d.setReleaseno(h.getOutboundNo());
                d.setMaterialCd(line.getMaterialCd());
                // 品名按物料名称原样申报（服务费行取虚物料自身名称，materialCd 仍按被维修商品口径）
                d.setMaterialDesc(m.getMaterialName());
                d.setVersion(BrInboundServiceImpl.parseVersion(line.getVersion(), line.getMaterialName()));
                d.setSpecificationsModels(spec);
                d.setDeliveryCnt(item.getDeliveryCnt());
                d.setPrice(item.getPrice());
                d.setCalcUnit(line.getCalcUnit());
                d.setCurrency(item.getCurrency());
                d.setIsBonded(line.getIsBonded());
                d.setInOutTime(h.getInOutTime());
                d.setBusiType(h.getBusiType());
                d.setBusiDesc(item.getBusiDesc());
                d.setBusiFormNum(StringUtils.defaultString(h.getBusiFormNum(), h.getOutboundNo()));
                d.setRelaCustomsFormType(h.getRelaCustomsFormType());
                d.setRelaCustomsFormNum(h.getRelaCustomsFormNum());
                d.setCalculatingRecordNo(h.getCalculatingRecordNo());
                d.setGoodsSourceType(item.getGoodsSourceType());
                d.setGoodsSourceNo(item.getGoodsSourceNo());
                d.setUuid(lastUuid);
                sdo2DataMapper.insert(d);
                BrDeclareLog log = new BrDeclareLog();
                log.setBizType("SDO2");
                log.setBizTable("br_outbound");
                log.setBizId(id);
                log.setMirrorTable("sdo2_data");
                log.setMirrorRowId(d.getId());
                log.setBizNo(h.getOutboundNo());
                log.setExecuteType(t);
                log.setUuid(StringUtils.defaultString(lastUuid, ""));
                declareLogMapper.insertBrDeclareLog(log);
            }
            // 写入镜像表即申报：03=已同步待回执（04 废弃）
            outboundMapper.updateDeclareResult(id, lastUuid, "03", "10", t);
        }
        return ids.length;
    }

    /** busiType=05 复运回境内区外：必须带服务费库存行（收料 returnType=8 生成，数量0.1） */
    private void checkServiceFeeLine(BrOutbound outbound)
    {
        if (!"05".equals(outbound.getBusiType())) { return; }
        for (BrOutboundItem item : outbound.getItems())
        {
            BrMaterial m = materialMapper.selectBrMaterialById(item.getMaterialId());
            if (m != null && "8".equals(m.getMarkType())) { return; }
        }
        throw new ServiceException("复运回境内区外(05)必须同单出库维修服务费库存行（数量0.1）");
    }

    /** 服务费行(markType=8)申报口径：由虚物料编码（被维修商品编码-SVF）反查被维修商品（样例集 00 注2） */
    private BrMaterial repairedMaterialOf(BrMaterial svf)
    {
        String cd = svf.getMaterialCd();
        if (!cd.endsWith("-SVF")) { throw new ServiceException("服务费虚物料编码异常: " + cd); }
        BrMaterial key = new BrMaterial();
        key.setMaterialCd(cd.substring(0, cd.length() - 4));
        key.setVersion("1");
        key.setCalcUnit(svf.getCalcUnit());
        BrMaterial repaired = materialMapper.selectBrMaterialByBizKey(key);
        if (repaired == null) { throw new ServiceException("服务费行无法确定被维修商品: " + key.getMaterialCd()); }
        return repaired;
    }

    private void validate(BrOutbound outbound, Long selfId)
    {
        if (StringUtils.isEmpty(outbound.getOutboundNo())) { throw new ServiceException("出库单号必填"); }
        BrOutbound exist = outboundMapper.selectByOutboundNo(outbound.getOutboundNo());
        if (exist != null && !exist.getId().equals(selfId)) { throw new ServiceException("出库单号已存在: " + outbound.getOutboundNo()); }
        if (exist == null && outboundMapper.selectByOutboundNoAny(outbound.getOutboundNo()) != null)
        {
            throw new ServiceException("出库单号已被占用（含已删除单据，编号不可复用）: " + outbound.getOutboundNo());
        }
        if (!"03".equals(outbound.getBusiType()) && !"04".equals(outbound.getBusiType())
                && !"05".equals(outbound.getBusiType()) && !"99".equals(outbound.getBusiType()))
        {
            throw new ServiceException("出库业务类型仅支持 03/04/05/99");
        }
        if (StringUtils.isEmpty(outbound.getBusiFormNum())) { throw new ServiceException("出库业务单证编号必填"); }
        if (StringUtils.isEmpty(outbound.getRelaCustomsFormType())) { throw new ServiceException("关联申报单证类型必填"); }
        if (outbound.getItems() == null || outbound.getItems().isEmpty()) { throw new ServiceException("出库明细不能为空"); }
        for (BrOutboundItem item : outbound.getItems())
        {
            if (!"g".equals(item.getGoodsSourceType()) && !"i".equals(item.getGoodsSourceType()))
            {
                throw new ServiceException("货物来源单证类型必须为 g(入库清单) 或 i(工单收料)");
            }
            if (StringUtils.isEmpty(item.getGoodsSourceNo())) { throw new ServiceException("货物来源单证编号必填"); }
            if (item.getDeliveryCnt() == null || item.getDeliveryCnt().signum() <= 0) { throw new ServiceException("出库数量必须大于 0"); }
            BrMaterial m = materialMapper.selectBrMaterialById(item.getMaterialId());
            if (m == null) { throw new ServiceException("明细物料不存在: " + item.getMaterialId()); }
            // 样例集 00 注2：服务费行数量固定 0.1
            if ("8".equals(m.getMarkType()) && item.getDeliveryCnt().compareTo(new BigDecimal("0.1")) != 0)
            {
                throw new ServiceException("维修服务费出库数量必须为 0.1（样例集 00 注2）");
            }
            // 规范 §3.5.1：busiType=99 其他 时出库说明必填
            if ("99".equals(outbound.getBusiType()) && StringUtils.isEmpty(item.getBusiDesc()))
            {
                throw new ServiceException("出库业务类型=99 其他 时出库说明必填（规范 §3.5.1）");
            }
            if (!"8".equals(m.getMarkType()))
            {
                BrInboundServiceImpl.parseVersion(m.getVersion(), m.getMaterialName());
            }
        }
    }

    /** 草稿保存软校验：来源余额不足时返回提示（不阻断保存，审核时硬拦截） */
    @Override
    public String draftWarning(BrOutbound outbound)
    {
        if (outbound.getItems() == null) { return ""; }
        StringBuilder warn = new StringBuilder();
        for (BrOutboundItem item : outbound.getItems())
        {
            BrMaterial m = materialMapper.selectBrMaterialById(item.getMaterialId());
            if (m == null || item.getDeliveryCnt() == null) { continue; }
            BigDecimal available = ledgerService.sourceBalance(item.getMaterialId(), item.getGoodsSourceType(), item.getGoodsSourceNo());
            BigDecimal out = item.getDeliveryCnt().abs();
            if (available.compareTo(out) < 0)
            {
                if (warn.length() > 0) { warn.append("；"); }
                warn.append(m.getMaterialName()).append(" 来源 ").append(item.getGoodsSourceNo())
                        .append(" 可用 ").append(available).append("，本次出库 ").append(out).append("（审核时将拦截）");
            }
        }
        return warn.toString();
    }
}
