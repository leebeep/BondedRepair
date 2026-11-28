package com.ruoyi.bonded.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.bonded.domain.BrDeclareLog;
import com.ruoyi.bonded.domain.BrInbound;
import com.ruoyi.bonded.domain.BrInboundItem;
import com.ruoyi.bonded.domain.BrMaterial;
import com.ruoyi.bonded.domain.Pto2Data;
import com.ruoyi.bonded.mapper.BrDeclareLogMapper;
import com.ruoyi.bonded.mapper.BrInboundMapper;
import com.ruoyi.bonded.mapper.BrMaterialMapper;
import com.ruoyi.bonded.mapper.Pto2DataMapper;
import com.ruoyi.bonded.service.BrLedgerService;
import com.ruoyi.bonded.service.BrPushSwitch;
import com.ruoyi.bonded.service.IBrInboundService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 入库单服务实现（PTO2）
 */
@Service
public class BrInboundServiceImpl implements IBrInboundService
{
    @Autowired private BrInboundMapper inboundMapper;
    @Autowired private BrMaterialMapper materialMapper;
    @Autowired private Pto2DataMapper pto2DataMapper;
    @Autowired private BrDeclareLogMapper declareLogMapper;
    @Autowired private BrLedgerService ledgerService;
    @Autowired private BrPushSwitch pushSwitch;

    /** 入库单号自动生成前缀参数（sys_config，可在 参数设置 中修改） */
    public static final String INBOUND_NO_PREFIX_KEY = "br.inbound.no.prefix";
    public static final String DEFAULT_INBOUND_NO_PREFIX = "IN";

    @Autowired private com.ruoyi.system.service.ISysConfigService configService;

    @Override
    public String nextInboundNo()
    {
        String prefix = configService.selectConfigByKey(INBOUND_NO_PREFIX_KEY);
        if (prefix == null || prefix.isEmpty()) { prefix = DEFAULT_INBOUND_NO_PREFIX; }
        String head = prefix + new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        String max = inboundMapper.selectMaxInboundNoByPrefix(head);
        int seq = 1;
        if (max != null && max.length() > head.length())
        {
            try { seq = Integer.parseInt(max.substring(head.length())) + 1; }
            catch (NumberFormatException ignored) { }
        }
        return head + String.format("%03d", seq);
    }

    @Override
    public List<BrInbound> selectList(BrInbound inbound)
    {
        List<BrInbound> list = inboundMapper.selectList(inbound);
        for (BrInbound h : list) { h.setItems(inboundMapper.selectItemsByInboundId(h.getId())); }
        return list;
    }

    @Override
    public BrInbound selectById(Long id)
    {
        BrInbound h = inboundMapper.selectById(id);
        if (h != null) { h.setItems(inboundMapper.selectItemsByInboundId(id)); }
        return h;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(BrInbound inbound)
    {
        validate(inbound, null);
        inbound.setCreateBy(SecurityUtils.getUsername());
        inboundMapper.insert(inbound);
        for (BrInboundItem item : inbound.getItems())
        {
            item.setInboundId(inbound.getId());
            inboundMapper.insertItem(item);
        }
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(BrInbound inbound)
    {
        BrInbound db = inboundMapper.selectById(inbound.getId());
        if (db == null) { throw new ServiceException("入库单不存在"); }
        if (!"01".equals(db.getDocStatus()) && !"06".equals(db.getDocStatus())) { throw new ServiceException("仅草稿或退单状态可修改"); }
        validate(inbound, inbound.getId());
        inbound.setUpdateBy(SecurityUtils.getUsername());
        inboundMapper.update(inbound);
        inboundMapper.deleteItemsByInboundId(inbound.getId());
        for (BrInboundItem item : inbound.getItems())
        {
            item.setInboundId(inbound.getId());
            inboundMapper.insertItem(item);
        }
        // 退单改单后回草稿，重新审核时以 executeType=2 变更重报（携带原 uuid）
        if ("06".equals(db.getDocStatus()))
        {
            inboundMapper.updateDeclareResult(inbound.getId(), db.getLastUuid(), "01", "00", db.getExecuteType());
        }
        return 1;
    }

    @Override
    public int deleteByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            BrInbound db = inboundMapper.selectById(id);
            if (db != null && !"01".equals(db.getDocStatus())) { throw new ServiceException("入库单【" + db.getInboundNo() + "】非草稿状态，禁止删除"); }
        }
        return inboundMapper.deleteByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int audit(Long id)
    {
        BrInbound h = inboundMapper.selectById(id);
        if (h == null) { throw new ServiceException("入库单不存在"); }
        if (!"01".equals(h.getDocStatus())) { throw new ServiceException("入库单【" + h.getInboundNo() + "】已审核"); }
        List<BrInboundItem> items = inboundMapper.selectItemsByInboundId(id);
        if (items.isEmpty()) { throw new ServiceException("入库单明细为空"); }
        for (BrInboundItem item : items)
        {
            BrMaterial m = materialMapper.selectBrMaterialById(item.getMaterialId());
            if (m == null) { throw new ServiceException("入库明细物料不存在: " + item.getMaterialId()); }
            ledgerService.record(item.getMaterialId(), m.getMarkType(), "IN", "br_inbound", id, h.getInboundNo(),
                    "g", h.getInboundNo(), null, null, item.getStorageCnt());
        }
        // 审核通过；自动申报开关打开时同事务写入镜像库（首报/变更自动判定），关闭则滞留待申报由补报任务处理
        int rows = inboundMapper.updateDeclareResult(id, h.getLastUuid(), "03", h.getCustomsStatus(), "1");
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
            BrInbound h = inboundMapper.selectById(id);
            if (h == null) { throw new ServiceException("入库单不存在"); }
            // A=自动判定：已申报过（有预录入编号）走变更(2)，否则首报(1)
            String t = executeType;
            if ("A".equals(t)) { t = StringUtils.isNotEmpty(h.getLastUuid()) ? "2" : "1"; }
                        // 执行策略联动：变更(2)/作废(3)仅限 申报中/申报通过/退单；在途(申报中未回执)禁止任何申报，避免报文叠写
            if ("03".equals(h.getDocStatus()) && ("10".equals(h.getCustomsStatus()) || "20".equals(h.getCustomsStatus()) || "21".equals(h.getCustomsStatus())))
            {
                throw new ServiceException("单据【" + h.getInboundNo() + "】申报回执未到（海关状态：" + h.getCustomsStatus() + "），请等待回执后再申报");
            }
            // 执行策略联动：已申报过（有预录入编号）只能变更或作废，不允许再次备案
            if ("1".equals(t) && StringUtils.isNotEmpty(h.getLastUuid()))
            {
                throw new ServiceException("入库单【" + h.getInboundNo() + "】已申报（预录入编号：" + h.getLastUuid() + "），只能变更或作废，不允许再次备案");
            }
            if ("2".equals(t) && !"03".equals(h.getDocStatus()) && !"05".equals(h.getDocStatus()) && !"06".equals(h.getDocStatus()))
            {
                throw new ServiceException("单据【" + h.getInboundNo() + "】仅申报中/申报通过/退单状态可变更（已作废单据不可变更）");
            }
            if ("1".equals(t) && !"03".equals(h.getDocStatus()))
            {
                throw new ServiceException("入库单【" + h.getInboundNo() + "】未审核通过，不能申报");
            }
            if ("2".equals(t) && StringUtils.isEmpty(h.getLastUuid()))
            {
                throw new ServiceException("入库单【" + h.getInboundNo() + "】未申报过，不能变更");
            }
            if ("3".equals(t) && !"03".equals(h.getDocStatus()) && !"05".equals(h.getDocStatus()) && !"06".equals(h.getDocStatus()))
            {
                throw new ServiceException("入库单【" + h.getInboundNo() + "】仅申报中/申报通过/退单状态可作废");
            }
            // 变更/作废（executeType=2/3）必须携带原预录入编号（规范 §2.2.x）
            String lastUuid = "1".equals(t) ? null : h.getLastUuid();
            for (BrInboundItem item : inboundMapper.selectItemsByInboundId(id))
            {
                BrMaterial m = materialMapper.selectBrMaterialById(item.getMaterialId());
                if (m == null || !"2".equals(m.getDeclareStatus()))
                {
                    throw new ServiceException("明细物料未备案通过，不能申报: " + (m == null ? item.getMaterialId() : m.getMaterialName()));
                }
                String spec = StringUtils.defaultString(item.getSpecificationsModels(), m.getgModel());
                if (StringUtils.isEmpty(spec))
                {
                    throw new ServiceException("物料【" + m.getMaterialName() + "】规格型号必填（规范 §3.2.1）");
                }
                Pto2Data d = new Pto2Data();
                d.setExecuteType(t);
                d.setTakeOrderCd(h.getInboundNo());
                d.setRelaEntNm(h.getRelaEntNm());
                d.setMaterialCd(m.getMaterialCd());
                d.setMaterialDesc(m.getMaterialName() + BrMaterialServiceImpl.markSuffix(m.getMarkType(), m.getIsBonded()));
                d.setMaterialVersion(parseVersion(m.getVersion(), m.getMaterialName()));
                d.setSpecificationsModels(spec);
                d.setStorageCnt(item.getStorageCnt());
                d.setPrice(item.getPrice());
                d.setCurrency(item.getCurrency());
                d.setIsBonded(m.getIsBonded());
                d.setCalcUnit(m.getCalcUnit());
                d.setInOutTime(h.getInOutTime());
                d.setBusiType(h.getBusiType());
                d.setBusiDesc(item.getBusiDesc());
                d.setBusiFormNum(StringUtils.defaultString(item.getBusiFormNum(), h.getInboundNo()));
                d.setRelaCustomsFormType(h.getRelaCustomsFormType());
                d.setRelaCustomsFormNum(h.getRelaCustomsFormNum());
                d.setGoodsSourceType(item.getGoodsSourceType());
                d.setGoodsSourceNo(item.getGoodsSourceNo());
                d.setBrandType(item.getBrandType());
                d.setTradeModeCd(item.getTradeModeCd());
                d.setUnit1Num(item.getUnit1Num());
                d.setUnit2Num(item.getUnit2Num());
                d.setArrivingCountryCode(item.getArrivingCountryCode());
                d.setEndCountryCode(item.getEndCountryCode());
                d.setUuid(lastUuid);
                pto2DataMapper.insert(d);

                BrDeclareLog log = new BrDeclareLog();
                log.setBizType("PTO2");
                log.setBizTable("br_inbound");
                log.setBizId(id);
                log.setMirrorTable("pto2_data");
                log.setMirrorRowId(d.getId());
                log.setBizNo(h.getInboundNo());
                log.setExecuteType(t);
                log.setUuid(StringUtils.defaultString(lastUuid, ""));
                declareLogMapper.insertBrDeclareLog(log);
            }
            // 写入镜像表即申报：03=已同步待回执（04 状态废弃），customs 10=待采集
            inboundMapper.updateDeclareResult(id, lastUuid, "03", "10", t);
        }
        return ids.length;
    }

    private void validate(BrInbound inbound, Long selfId)
    {
        if (StringUtils.isEmpty(inbound.getInboundNo())) { throw new ServiceException("入库单号必填"); }
        BrInbound exist = inboundMapper.selectByInboundNo(inbound.getInboundNo());
        if (exist != null && !exist.getId().equals(selfId)) { throw new ServiceException("入库单号已存在: " + inbound.getInboundNo()); }
        if (exist == null && inboundMapper.selectByInboundNoAny(inbound.getInboundNo()) != null)
        {
            throw new ServiceException("入库单号已被占用（含已删除单据，编号不可复用）: " + inbound.getInboundNo());
        }
        if (!"04".equals(inbound.getBusiType()) && !"05".equals(inbound.getBusiType()) && !"99".equals(inbound.getBusiType()))
        {
            throw new ServiceException("入库业务类型仅支持 04-待维修货物 / 05-维修用料 / 99-其他");
        }
        if (StringUtils.isEmpty(inbound.getRelaCustomsFormType())) { throw new ServiceException("关联申报单证类型必填"); }
        if (inbound.getItems() == null || inbound.getItems().isEmpty()) { throw new ServiceException("入库明细不能为空"); }
        // 规范 §3.2.1：关联申报单证类型=01 径予放行申明时，品牌类型/监管方式/启抵国/原产国必填
        boolean cond01 = "01".equals(inbound.getRelaCustomsFormType());
        for (BrInboundItem item : inbound.getItems())
        {
            BrMaterial m = materialMapper.selectBrMaterialById(item.getMaterialId());
            if (m == null || !"2".equals(m.getDeclareStatus()))
            {
                throw new ServiceException("明细物料未备案通过: " + (m == null ? item.getMaterialId() : m.getMaterialName()));
            }
            if (item.getStorageCnt() == null || item.getStorageCnt().signum() <= 0) { throw new ServiceException("入库数量必须大于 0"); }
            if (item.getPrice() == null) { throw new ServiceException("单价必填"); }
            parseVersion(m.getVersion(), m.getMaterialName());
            if (cond01)
            {
                if (StringUtils.isEmpty(item.getBrandType())) { throw new ServiceException("关联申报单证类型=01 径予放行申明时品牌类型必填（规范 §3.2.1）"); }
                if (StringUtils.isEmpty(item.getTradeModeCd())) { throw new ServiceException("关联申报单证类型=01 径予放行申明时监管方式必填（规范 §3.2.1）"); }
                if (StringUtils.isEmpty(item.getArrivingCountryCode())) { throw new ServiceException("关联申报单证类型=01 径予放行申明时启抵国必填（规范 §3.2.1）"); }
                if (StringUtils.isEmpty(item.getEndCountryCode())) { throw new ServiceException("关联申报单证类型=01 径予放行申明时原产国必填（规范 §3.2.1）"); }
            }
            if ("1".equals(m.getIsBonded()) && item.getUnit1Num() == null)
            {
                throw new ServiceException("保税商品【" + m.getMaterialName() + "】第一法定数量必填（规范 §3.2.1）");
            }
            // 规范 §3.2.2：busiType=99 其他 时入库说明必填
            if ("99".equals(inbound.getBusiType()) && StringUtils.isEmpty(item.getBusiDesc()))
            {
                throw new ServiceException("入库业务类型=99 其他 时入库说明必填（规范 §3.2.2）");
            }
        }
    }

    /** 版本号严格转 Integer（镜像表 material_version int），失败即报错不静默 */
    static Integer parseVersion(String version, String materialName)
    {
        try { return Integer.valueOf(StringUtils.defaultString(version, "1").trim()); }
        catch (Exception e) { throw new ServiceException("物料【" + materialName + "】版本号必须为数字: " + version); }
    }
}
