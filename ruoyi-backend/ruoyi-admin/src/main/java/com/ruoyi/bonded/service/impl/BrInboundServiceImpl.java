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
        if (!"01".equals(db.getDocStatus())) { throw new ServiceException("仅草稿状态可修改"); }
        validate(inbound, inbound.getId());
        inbound.setUpdateBy(SecurityUtils.getUsername());
        inboundMapper.update(inbound);
        inboundMapper.deleteItemsByInboundId(inbound.getId());
        for (BrInboundItem item : inbound.getItems())
        {
            item.setInboundId(inbound.getId());
            inboundMapper.insertItem(item);
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
        if (!"01".equals(h.getDocStatus()) && !"02".equals(h.getDocStatus())) { throw new ServiceException("入库单【" + h.getInboundNo() + "】已审核"); }
        List<BrInboundItem> items = inboundMapper.selectItemsByInboundId(id);
        if (items.isEmpty()) { throw new ServiceException("入库单明细为空"); }
        for (BrInboundItem item : items)
        {
            BrMaterial m = materialMapper.selectBrMaterialById(item.getMaterialId());
            ledgerService.record(item.getMaterialId(), m.getMarkType(), "IN", "br_inbound", id,
                    "g", h.getInboundNo(), null, null, item.getStorageCnt());
        }
        return inboundMapper.updateDeclareResult(id, h.getLastUuid(), "03", h.getCustomsStatus(), "1");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int declare(Long[] ids, String executeType)
    {
        for (Long id : ids)
        {
            BrInbound h = inboundMapper.selectById(id);
            if (h == null) { throw new ServiceException("入库单不存在"); }
            if ("1".equals(executeType) && !"03".equals(h.getDocStatus()))
            {
                throw new ServiceException("入库单【" + h.getInboundNo() + "】未审核通过，不能申报");
            }
            if (!"1".equals(executeType) && "01".equals(h.getDocStatus()))
            {
                throw new ServiceException("入库单【" + h.getInboundNo() + "】未申报过，不能变更/作废");
            }
            // 变更/作废（executeType=2/3）必须携带原预录入编号（规范 §2.2.x）
            String lastUuid = "1".equals(executeType) ? null : h.getLastUuid();
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
                d.setExecuteType(executeType);
                d.setTakeOrderCd(h.getInboundNo());
                d.setRelaEntNm(h.getRelaEntNm());
                d.setMaterialCd(m.getMaterialCd());
                d.setMaterialDesc(m.getMaterialName() + BrMaterialServiceImpl.markSuffix(m.getMarkType()));
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
                log.setExecuteType(executeType);
                log.setUuid(StringUtils.defaultString(lastUuid, ""));
                declareLogMapper.insertBrDeclareLog(log);
            }
            inboundMapper.updateDeclareResult(id, lastUuid, "04", "10", executeType);
        }
        return ids.length;
    }

    private void validate(BrInbound inbound, Long selfId)
    {
        if (StringUtils.isEmpty(inbound.getInboundNo())) { throw new ServiceException("入库单号必填"); }
        BrInbound exist = inboundMapper.selectByInboundNo(inbound.getInboundNo());
        if (exist != null && !exist.getId().equals(selfId)) { throw new ServiceException("入库单号已存在: " + inbound.getInboundNo()); }
        if (!"04".equals(inbound.getBusiType()) && !"05".equals(inbound.getBusiType()))
        {
            throw new ServiceException("入库业务类型仅支持 04-待维修货物 / 05-维修用料");
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
        }
    }

    /** 版本号严格转 Integer（镜像表 material_version int），失败即报错不静默 */
    static Integer parseVersion(String version, String materialName)
    {
        try { return Integer.valueOf(StringUtils.defaultString(version, "1").trim()); }
        catch (Exception e) { throw new ServiceException("物料【" + materialName + "】版本号必须为数字: " + version); }
    }
}
