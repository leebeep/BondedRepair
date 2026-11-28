package com.ruoyi.bonded.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.bonded.domain.BrDeclareLog;
import com.ruoyi.bonded.domain.BrMaterial;
import com.ruoyi.bonded.domain.BrReceive;
import com.ruoyi.bonded.domain.BrReceiveItem;
import com.ruoyi.bonded.domain.BrWorkOrder;
import com.ruoyi.bonded.domain.BrWorkOrderItem;
import com.ruoyi.bonded.domain.Mso2Data;
import com.ruoyi.bonded.mapper.BrDeclareLogMapper;
import com.ruoyi.bonded.mapper.BrMaterialMapper;
import com.ruoyi.bonded.mapper.BrReceiveMapper;
import com.ruoyi.bonded.mapper.BrWorkOrderMapper;
import com.ruoyi.bonded.mapper.Mso2DataMapper;
import com.ruoyi.bonded.service.BrLedgerService;
import com.ruoyi.bonded.service.BrPushSwitch;
import com.ruoyi.bonded.service.BrWorkOrderService;
import com.ruoyi.bonded.service.IBrReceiveService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 工单收料服务实现（MSO2）
 * 规则（样例集约定）：服务费行(returnType=8)自动生成：数量固定0.1，
 * 物料=被维修商品编码及名称+"维修服务费"；已修复(5)+残次品(a) 收料合计 <= 工单实际投入。
 */
@Service
public class BrReceiveServiceImpl implements IBrReceiveService
{
    private static final BigDecimal SERVICE_FEE_QTY = new BigDecimal("0.1");

    /** 收料单号自动生成前缀参数（sys_config，可在 参数设置 中修改） */
    public static final String RECEIVE_NO_PREFIX_KEY = "br.receive.no.prefix";
    public static final String DEFAULT_RECEIVE_NO_PREFIX = "RC";

    @Autowired private com.ruoyi.system.service.ISysConfigService configService;

    @Override
    public String nextReceiveNo()
    {
        String prefix = configService.selectConfigByKey(RECEIVE_NO_PREFIX_KEY);
        if (prefix == null || prefix.isEmpty()) { prefix = DEFAULT_RECEIVE_NO_PREFIX; }
        String head = prefix + new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        String max = receiveMapper.selectMaxReceiveNoByPrefix(head);
        int seq = 1;
        if (max != null && max.length() > head.length())
        {
            try { seq = Integer.parseInt(max.substring(head.length())) + 1; }
            catch (NumberFormatException ignored) { }
        }
        return head + String.format("%03d", seq);
    }

    @Autowired private BrReceiveMapper receiveMapper;
    @Autowired private BrWorkOrderMapper workOrderMapper;
    @Autowired private BrMaterialMapper materialMapper;
    @Autowired private Mso2DataMapper mso2DataMapper;
    @Autowired private BrDeclareLogMapper declareLogMapper;
    @Autowired private BrLedgerService ledgerService;
    @Autowired private BrPushSwitch pushSwitch;
    @Autowired private BrWorkOrderService workOrderService;

    @Override
    public List<BrReceive> selectList(BrReceive receive)
    {
        List<BrReceive> list = receiveMapper.selectList(receive);
        for (BrReceive h : list) { h.setItems(receiveMapper.selectItemsByReceiveId(h.getId())); }
        return list;
    }

    @Override
    public BrReceive selectById(Long id)
    {
        BrReceive h = receiveMapper.selectById(id);
        if (h != null) { h.setItems(receiveMapper.selectItemsByReceiveId(id)); }
        return h;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(BrReceive receive)
    {
        validate(receive, null);
        // 工单号/批次号/服务费由表单录入：不存在自动建档并更新（非海关单证，仅供 MSO2 work_no 与服务费行）
        com.ruoyi.bonded.domain.BrWorkOrder wo = workOrderService.getOrCreate(
                receive.getWorkNo(), receive.getBatchNo(), receive.getServiceFee(), receive.getCurrency());
        receive.setWorkOrderId(wo.getId());
        autoAppendServiceFeeLine(receive);
        receive.setCreateBy(SecurityUtils.getUsername());
        receiveMapper.insert(receive);
        for (BrReceiveItem item : receive.getItems())
        {
            item.setReceiveId(receive.getId());
            receiveMapper.insertItem(item);
        }
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(BrReceive receive)
    {
        BrReceive db = receiveMapper.selectById(receive.getId());
        if (db == null) { throw new ServiceException("收料单不存在"); }
        if (!"01".equals(db.getDocStatus()) && !"06".equals(db.getDocStatus())) { throw new ServiceException("仅草稿或退单状态可修改"); }
        validate(receive, receive.getId());
        if (StringUtils.isNotEmpty(receive.getWorkNo()))
        {
            com.ruoyi.bonded.domain.BrWorkOrder wo = workOrderService.getOrCreate(
                    receive.getWorkNo(), receive.getBatchNo(), receive.getServiceFee(), receive.getCurrency());
            receive.setWorkOrderId(wo.getId());
        }
        autoAppendServiceFeeLine(receive);
        receive.setUpdateBy(SecurityUtils.getUsername());
        receiveMapper.update(receive);
        receiveMapper.deleteItemsByReceiveId(receive.getId());
        for (BrReceiveItem item : receive.getItems())
        {
            item.setReceiveId(receive.getId());
            receiveMapper.insertItem(item);
        }
        if ("06".equals(db.getDocStatus()))
        {
            receiveMapper.updateDeclareResult(receive.getId(), db.getLastUuid(), "01", "00", db.getExecuteType());
        }
        return 1;
    }

    @Override
    public int deleteByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            BrReceive db = receiveMapper.selectById(id);
            if (db != null && !"01".equals(db.getDocStatus())) { throw new ServiceException("收料单【" + db.getReceiveNo() + "】非草稿状态，禁止删除"); }
        }
        return receiveMapper.deleteByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int audit(Long id)
    {
        BrReceive h = receiveMapper.selectById(id);
        if (h == null) { throw new ServiceException("收料单不存在"); }
        if (!"01".equals(h.getDocStatus())) { throw new ServiceException("收料单【" + h.getReceiveNo() + "】已审核"); }
        BrWorkOrder wo = workOrderMapper.selectById(h.getWorkOrderId());
        if (wo == null) { throw new ServiceException("收料单关联的工单不存在"); }
        // 样例集 00 §0.2：已修复/残次品收料不得超过工单实际投入（发料 - 未使用收料）
        // 汇总口径仅含已审核（03/05）单据，当前单据尚未入账，需手工累加本次收料量后再校验
        BigDecimal issued = workOrderMapper.sumIssuedRepairQty(wo.getWorkNo());
        BigDecimal priorUnused = workOrderMapper.sumUnusedRepairQty(wo.getWorkNo());
        BigDecimal priorReceived = workOrderMapper.sumReceivedRepairQty(wo.getWorkNo());
        BigDecimal curReturn = BigDecimal.ZERO;
        BigDecimal curUnused = BigDecimal.ZERO;
        for (BrReceiveItem item : receiveMapper.selectItemsByReceiveId(id))
        {
            if ("5".equals(item.getReturnType()) || "a".equals(item.getReturnType()))
            {
                curReturn = curReturn.add(item.getMaterialNum());
            }
            else if ("1".equals(item.getReturnType()))
            {
                curUnused = curUnused.add(item.getMaterialNum());
            }
        }
        BigDecimal receivedTotal = priorReceived.add(curReturn);
        BigDecimal unusedTotal = priorUnused.add(curUnused);
        if (receivedTotal.compareTo(issued.subtract(unusedTotal)) > 0)
        {
            throw new ServiceException("工单【" + wo.getWorkNo() + "】已修复/残次品收料数量超过实际投入（已收 "
                    + receivedTotal + "，本次 " + curReturn + "，实际投入 " + issued.subtract(unusedTotal) + "）");
        }
        List<BrReceiveItem> items = receiveMapper.selectItemsByReceiveId(id);
        for (BrReceiveItem item : items)
        {
            BrMaterial m = materialMapper.selectBrMaterialById(item.getMaterialId());
            if (m == null) { throw new ServiceException("收料明细物料不存在: " + item.getMaterialId()); }
            ledgerService.record(item.getMaterialId(), m.getMarkType(), "RCV", "br_receive", id, h.getReceiveNo(),
                    "i", h.getReceiveNo(), wo.getWorkNo(), wo.getBatchNo(), item.getMaterialNum());
        }
        // 审核通过；自动申报开关打开时同事务写入镜像库，关闭则滞留待申报由补报任务处理
        int rows = receiveMapper.updateDeclareResult(id, h.getLastUuid(), "03", h.getCustomsStatus(), "1");
        if (pushSwitch.isEnabled())
        {
            declare(new Long[] { id }, "A");
        }
        // 收齐校验：已修复+残次品 >= 实际投入 → 工单转已收齐（当前单据已入账，直接取最新汇总）
        if ("02".equals(wo.getOrderStatus()))
        {
            BigDecimal nowReceived = workOrderMapper.sumReceivedRepairQty(wo.getWorkNo());
            BigDecimal nowUnused = workOrderMapper.sumUnusedRepairQty(wo.getWorkNo());
            if (nowReceived.compareTo(issued.subtract(nowUnused)) >= 0)
            {
                workOrderMapper.updateOrderStatus(wo.getId(), "03");
            }
        }
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int declare(Long[] ids, String executeType)
    {
        for (Long id : ids)
        {
            BrReceive h = receiveMapper.selectById(id);
            if (h == null) { throw new ServiceException("收料单不存在"); }
            // A=自动判定：已申报过（有预录入编号）走变更(2)，否则首报(1)
            String t = executeType;
            if ("A".equals(t)) { t = StringUtils.isNotEmpty(h.getLastUuid()) ? "2" : "1"; }
                        // 执行策略联动：变更(2)/作废(3)仅限 申报中/申报通过/退单；在途(申报中未回执)禁止任何申报，避免报文叠写
            if ("03".equals(h.getDocStatus()) && ("10".equals(h.getCustomsStatus()) || "20".equals(h.getCustomsStatus()) || "21".equals(h.getCustomsStatus())))
            {
                throw new ServiceException("单据【" + h.getReceiveNo() + "】申报回执未到（海关状态：" + h.getCustomsStatus() + "），请等待回执后再申报");
            }
            // 执行策略联动：已申报过（有预录入编号）只能变更或作废，不允许再次备案
            if ("1".equals(t) && StringUtils.isNotEmpty(h.getLastUuid()))
            {
                throw new ServiceException("收料单【" + h.getReceiveNo() + "】已申报（预录入编号：" + h.getLastUuid() + "），只能变更或作废，不允许再次备案");
            }
            if ("2".equals(t) && !"03".equals(h.getDocStatus()) && !"05".equals(h.getDocStatus()) && !"06".equals(h.getDocStatus()))
            {
                throw new ServiceException("单据【" + h.getReceiveNo() + "】仅申报中/申报通过/退单状态可变更（已作废单据不可变更）");
            }
            if ("1".equals(t) && !"03".equals(h.getDocStatus()))
            {
                throw new ServiceException("收料单【" + h.getReceiveNo() + "】未审核通过，不能申报");
            }
            if ("2".equals(t) && StringUtils.isEmpty(h.getLastUuid()))
            {
                throw new ServiceException("收料单【" + h.getReceiveNo() + "】未申报过，不能变更");
            }
            if ("3".equals(t) && !"03".equals(h.getDocStatus()) && !"05".equals(h.getDocStatus()) && !"06".equals(h.getDocStatus()))
            {
                throw new ServiceException("收料单【" + h.getReceiveNo() + "】仅申报中/申报通过/退单状态可作废");
            }
            // 变更/作废（executeType=2/3）必须携带原预录入编号（规范 §2.2.x）
            String lastUuid = "1".equals(t) ? null : h.getLastUuid();
            BrWorkOrder wo = workOrderMapper.selectById(h.getWorkOrderId());
            for (BrReceiveItem item : receiveMapper.selectItemsByReceiveId(id))
            {
                BrMaterial m = materialMapper.selectBrMaterialById(item.getMaterialId());
                if (m == null) { throw new ServiceException("明细物料不存在: " + item.getMaterialId()); }
                // 样例集 00 注2：服务费行(returnType=8)按被维修商品口径申报，materialCd/名称/版本/单位/保税标志均取被维修商品
                BrMaterial line = m;
                if ("8".equals(m.getMarkType()))
                {
                    line = repairedMaterialOf(wo);
                }
                else if (!"2".equals(m.getDeclareStatus()))
                {
                    throw new ServiceException("明细物料未备案通过，不能申报: " + m.getMaterialName());
                }
                Mso2Data d = new Mso2Data();
                d.setExecuteType(t);
                d.setReturnMaterialNo(h.getReceiveNo());
                d.setReturnTime(h.getReceiveTime());
                d.setWorkNo(wo.getWorkNo());
                d.setMaterialCd(line.getMaterialCd());
                d.setMaterialDesc("8".equals(m.getMarkType())
                        ? line.getMaterialName() + BrMaterialServiceImpl.markSuffix("2", line.getIsBonded()) + "维修服务费"
                        : line.getMaterialName() + BrMaterialServiceImpl.markSuffix(line.getMarkType(), line.getIsBonded()));
                d.setMaterialVersion(new BigDecimal(StringUtils.defaultString(line.getVersion(), "1")));
                d.setMaterialNum(item.getMaterialNum());
                d.setCalcUnit(line.getCalcUnit());
                d.setIsBonded(line.getIsBonded());
                d.setReturnType(item.getReturnType());
                d.setUuid(lastUuid);
                mso2DataMapper.insert(d);

                BrDeclareLog log = new BrDeclareLog();
                log.setBizType("MSO2");
                log.setBizTable("br_receive");
                log.setBizId(id);
                log.setMirrorTable("mso2_data");
                log.setMirrorRowId(d.getId());
                log.setBizNo(h.getReceiveNo());
                log.setExecuteType(t);
                log.setUuid(StringUtils.defaultString(lastUuid, ""));
                declareLogMapper.insertBrDeclareLog(log);
            }
            receiveMapper.updateDeclareResult(id, lastUuid, "03", "10", t);
        }
        return ids.length;
    }

    /** 自动追加维修服务费行 */
    private void autoAppendServiceFeeLine(BrReceive receive)
    {
        BrWorkOrder wo = workOrderMapper.selectById(receive.getWorkOrderId());
        if (wo.getServiceFee() == null || wo.getServiceFee().signum() <= 0) { return; }
        for (BrReceiveItem item : receive.getItems())
        {
            if ("8".equals(item.getReturnType())) { return; }
        }
        List<BrWorkOrderItem> woItems = workOrderMapper.selectItemsByWorkOrderId(wo.getId());
        if (woItems.isEmpty()) { throw new ServiceException("工单无可维修物料，无法生成服务费行"); }
        BrMaterial repaired = materialMapper.selectBrMaterialById(woItems.get(0).getMaterialId());

        BrMaterial fee = materialMapper.selectBrMaterialByBizKey(bizKey(repaired, wo.getWorkNo()));
        if (fee == null)
        {
            fee = new BrMaterial();
            fee.setMaterialCd(repaired.getMaterialCd() + "-SVF");
            fee.setVersion("1");
            fee.setCalcUnit(repaired.getCalcUnit());
            fee.setMaterialName(repaired.getMaterialName() + "维修服务费");
            fee.setMarkType("8");
            fee.setgType("I");
            fee.setIsBonded("1");
            fee.setgModel(StringUtils.defaultString(repaired.getgModel(), "维修服务费"));
            // 与保税必填口径一致：拷贝被维修商品的法定计量单位/HS 字段（虚物料不单独申报，仅保持数据完整）
            fee.setUnit1(repaired.getUnit1());
            fee.setCodeTs(repaired.getCodeTs());
            fee.setgName(repaired.getgName());
            fee.setRemark("自动生成：工单 " + wo.getWorkNo() + " 维修服务费虚物料");
            materialMapper.insertBrMaterial(fee);
        }
        BrReceiveItem feeLine = new BrReceiveItem();
        feeLine.setMaterialId(fee.getId());
        feeLine.setMaterialNum(SERVICE_FEE_QTY);
        feeLine.setReturnType("8");
        receive.getItems().add(feeLine);
    }

    /** 服务费虚物料按 虚编号+版本+计量单位 联合主键查询 */
    private BrMaterial bizKey(BrMaterial repaired, String workNo)
    {
        BrMaterial key = new BrMaterial();
        key.setMaterialCd(repaired.getMaterialCd() + "-SVF");
        key.setVersion("1");
        key.setCalcUnit(repaired.getCalcUnit());
        return key;
    }

    /** 服务费行申报口径：取工单第一个待维修物料作为被维修商品（样例集 00 注2） */
    private BrMaterial repairedMaterialOf(BrWorkOrder wo)
    {
        List<BrWorkOrderItem> woItems = workOrderMapper.selectItemsByWorkOrderId(wo.getId());
        if (woItems.isEmpty()) { throw new ServiceException("工单【" + wo.getWorkNo() + "】无待维修物料，服务费行无法确定被维修商品"); }
        BrMaterial repaired = materialMapper.selectBrMaterialById(woItems.get(0).getMaterialId());
        if (repaired == null) { throw new ServiceException("工单【" + wo.getWorkNo() + "】被维修物料不存在"); }
        return repaired;
    }

    private void validate(BrReceive receive, Long selfId)
    {
        if (StringUtils.isEmpty(receive.getReceiveNo())) { throw new ServiceException("收料单号必填"); }
        BrReceive exist = receiveMapper.selectByReceiveNo(receive.getReceiveNo());
        if (exist != null && !exist.getId().equals(selfId)) { throw new ServiceException("收料单号已存在: " + receive.getReceiveNo()); }
        if (exist == null && receiveMapper.selectByReceiveNoAny(receive.getReceiveNo()) != null)
        {
            throw new ServiceException("收料单号已被占用（含已删除单据，编号不可复用）: " + receive.getReceiveNo());
        }
        if (StringUtils.isEmpty(receive.getWorkNo()) && receive.getWorkOrderId() == null) { throw new ServiceException("工单号必填"); }
        if (receive.getItems() == null || receive.getItems().isEmpty()) { throw new ServiceException("收料明细不能为空"); }
        for (BrReceiveItem item : receive.getItems())
        {
            if (materialMapper.selectBrMaterialById(item.getMaterialId()) == null)
            {
                throw new ServiceException("收料明细物料不存在: " + item.getMaterialId());
            }
            String rt = item.getReturnType();
            if (!"1".equals(rt) && !"5".equals(rt) && !"6".equals(rt) && !"7".equals(rt)
                    && !"8".equals(rt) && !"9".equals(rt) && !"a".equals(rt))
            {
                throw new ServiceException("非法收料类型: " + rt + "（本期支持 1/5/6/7/8/9/a）");
            }
            if (item.getMaterialNum() == null || item.getMaterialNum().signum() <= 0) { throw new ServiceException("收料数量必须大于 0"); }
        }
    }
}
