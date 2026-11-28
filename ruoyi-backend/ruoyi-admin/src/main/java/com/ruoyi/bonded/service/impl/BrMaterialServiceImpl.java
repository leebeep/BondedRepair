package com.ruoyi.bonded.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.bonded.domain.BrDeclareLog;
import com.ruoyi.bonded.domain.BrMaterial;
import com.ruoyi.bonded.domain.Epd101Data;
import com.ruoyi.bonded.mapper.BrDeclareLogMapper;
import com.ruoyi.bonded.mapper.BrMaterialMapper;
import com.ruoyi.bonded.mapper.Epd101DataMapper;
import com.ruoyi.bonded.service.BrPushSwitch;
import com.ruoyi.bonded.service.IBrMaterialService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 物料清单服务实现
 */
@Service
public class BrMaterialServiceImpl implements IBrMaterialService
{
    @Autowired
    private BrMaterialMapper materialMapper;

    @Autowired
    private Epd101DataMapper epd101DataMapper;

    @Autowired
    private BrDeclareLogMapper declareLogMapper;

    @Autowired
    private BrPushSwitch pushSwitch;

    /** 物料编号自动生成参数（sys_config，默认 true） */
    public static final String AUTO_NO_KEY = "br.material.auto.no";

    @Autowired
    private com.ruoyi.system.service.ISysConfigService configService;

    /** 物料编号自动生成开关（未配置视为开） */
    public boolean isAutoNoEnabled()
    {
        String v = configService.selectConfigByKey(AUTO_NO_KEY);
        return v == null || v.isEmpty() || "true".equalsIgnoreCase(v);
    }

    /** 生成物料编号：WL + yyyyMMdd + 4 位当日流水（与备案联合主键唯一性配套） */
    private String generateMaterialCd()
    {
        String prefix = "WL" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String max = materialMapper.selectMaxMaterialCdByPrefix(prefix);
        int seq = 1;
        if (max != null && max.length() > prefix.length())
        {
            try
            {
                seq = Integer.parseInt(max.substring(prefix.length())) + 1;
            }
            catch (NumberFormatException ignored)
            {
            }
        }
        return prefix + String.format("%04d", seq);
    }

    @Value("${br.enterpCd}")
    private String enterpCd;

    @Value("${br.enterpNm}")
    private String enterpNm;

    @Value("${br.masterCuscd}")
    private String masterCuscd;

    /** 货物类型（六类标记）→ 品名后缀（规范 v1.4 / 样例数据口径，如 新能源动力电池模组（无法修复）） */
    public static String markSuffix(String markType)
    {
        return markSuffix(markType, "1");
    }

    /** 同上；维修用料（markType=4）按保税标志区分后缀：非保税 → （非保税） */
    public static String markSuffix(String markType, String isBonded)
    {
        switch (markType)
        {
            case "1": return "（待修复）";
            case "2": return "（已修复）";
            case "3": return "（无法修复）";
            case "4": return "0".equals(isBonded) ? "（非保税）" : "（维修用保税料件）";
            case "5": return "（维修替换下的旧件或坏件）";
            case "6": return "（维修产生的边角料）";
            case "8": return "维修服务费";
            default: return "";
        }
    }

    @Override
    public List<BrMaterial> selectBrMaterialList(BrMaterial material)
    {
        return materialMapper.selectBrMaterialList(material);
    }

    @Override
    public BrMaterial selectBrMaterialById(Long id)
    {
        return materialMapper.selectBrMaterialById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertBrMaterial(BrMaterial material)
    {
        material.setVersion(StringUtils.defaultString(material.getVersion(), "1"));
        if (materialMapper.selectBrMaterialByBizKey(material) != null)
        {
            throw new ServiceException("物料编号+版本号+计量单位已存在（备案联合业务主键唯一）");
        }
        // 物料编号：未填时按参数自动生成（br.material.auto.no，默认开）；关闭则必填
        if (StringUtils.isEmpty(material.getMaterialCd()))
        {
            if (isAutoNoEnabled())
            {
                material.setMaterialCd(generateMaterialCd());
            }
            else
            {
                throw new ServiceException("物料编号必填（自动生成已关闭，可在 系统管理-参数设置 br.material.auto.no 调整）");
            }
        }
        validate(material);
        material.setCreateBy(SecurityUtils.getUsername());
        int rows = materialMapper.insertBrMaterial(material);
        // 自动备案：保存即写入镜像库（服务费虚物料 markType=8 非报关对象，仅本地记账）
        if (pushSwitch.isEnabled() && !"8".equals(material.getMarkType()))
        {
            declareEPD101(new Long[] { material.getId() }, "1");
        }
        return rows;
    }

    @Override
    public int updateBrMaterial(BrMaterial material)
    {
        BrMaterial db = materialMapper.selectBrMaterialById(material.getId());
        if (db == null)
        {
            throw new ServiceException("物料不存在");
        }
        if ("2".equals(db.getDeclareStatus()))
        {
            throw new ServiceException("已备案通过的物料禁止修改，请使用【变更申报】");
        }
        validate(material);
        material.setUpdateBy(SecurityUtils.getUsername());
        int rows = materialMapper.updateBrMaterial(material);
        // 退单改单后自动重报备案（首报口径），保证不滞留
        if ("3".equals(db.getDeclareStatus()) && pushSwitch.isEnabled() && !"8".equals(db.getMarkType()))
        {
            declareEPD101(new Long[] { material.getId() }, "1");
        }
        return rows;
    }

    @Override
    public int deleteBrMaterialByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            BrMaterial db = materialMapper.selectBrMaterialById(id);
            if (db != null && "2".equals(db.getDeclareStatus()))
            {
                throw new ServiceException("物料【" + db.getMaterialName() + "】已备案通过，禁止删除，请使用【作废申报】");
            }
        }
        return materialMapper.deleteBrMaterialByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int declareEPD101(Long[] ids, String executeType)
    {
        int count = 0;
        for (Long id : ids)
        {
            BrMaterial material = materialMapper.selectBrMaterialById(id);
            if (material == null)
            {
                throw new ServiceException("物料 id=" + id + " 不存在");
            }
            checkDeclareAllowed(material, executeType);
            if ("1".equals(executeType))
            {
                validate(material);
            }

            Epd101Data data = buildEpd101(material, executeType);
            // 变更/作废（executeType=2/3）必须携带原预录入编号（规范 §2.2.x）
            String lastUuid = "1".equals(executeType) ? null : material.getUuid();
            data.setUuid(lastUuid);
            epd101DataMapper.insertEpd101Data(data);

            BrDeclareLog log = new BrDeclareLog();
            log.setBizType("EPD101");
            log.setBizTable("br_material");
            log.setBizId(id);
            log.setMirrorTable("epd101_data");
            log.setMirrorRowId(data.getId());
            log.setBizNo(material.getMaterialCd());
            log.setExecuteType(executeType);
            log.setUuid(StringUtils.defaultString(lastUuid, StringUtils.EMPTY));
            declareLogMapper.insertBrDeclareLog(log);

            materialMapper.updateDeclareResult(id, lastUuid, "1", "10", executeType);
            count++;
        }
        return count;
    }

    /** 构造 EPD101 镜像行（品名自动拼接货物类型后缀） */
    private Epd101Data buildEpd101(BrMaterial m, String executeType)
    {
        Epd101Data d = new Epd101Data();
        d.setExecuteType(executeType);
        d.setDclTypecd(executeType);
        d.setEnterpCd(enterpCd);
        d.setEnterpNm(enterpNm);
        d.setMasterCuscd(masterCuscd);
        d.setIntProductNo(m.getMaterialCd());
        d.setIntProductName(m.getMaterialName() + markSuffix(m.getMarkType(), m.getIsBonded()));
        d.setVersion(m.getVersion());
        d.setgType(m.getgType());
        d.setgModel(StringUtils.defaultString(m.getgModel(), "无"));
        d.setIsBonded(m.getIsBonded());
        d.setCodeTs(m.getCodeTs());
        d.setgName(m.getgName());
        d.setCalcUnit(m.getCalcUnit());
        d.setUnit1(m.getUnit1());
        d.setUnit2(m.getUnit2());
        d.setDutyMode(m.getDutyMode());
        d.setTotalGrossWt(m.getTotalGrossWt());
        d.setTotalNetWt(m.getTotalNetWt());
        d.setNote(m.getRemark());
        return d;
    }

    private void checkDeclareAllowed(BrMaterial m, String executeType)
    {
        // 样例集 00 注2：维修服务费不单独备案，禁止 markType=8 执行 EPD101
        if ("8".equals(m.getMarkType()))
        {
            throw new ServiceException("维修服务费虚物料【" + m.getMaterialName() + "】不单独备案（样例集 00 注2），仅作台账库存载体");
        }
        String st = m.getDeclareStatus();
        switch (executeType)
        {
            case "1":
                if (!"0".equals(st) && !"3".equals(st))
                {
                    throw new ServiceException("物料【" + m.getMaterialName() + "】当前状态不允许备案申报");
                }
                break;
            case "2":
            case "3":
                if ("1".equals(st))
                {
                    throw new ServiceException("物料【" + m.getMaterialName() + "】申报回执未到（已申报待回执），请等待回执后再申报");
                }
                if (!"2".equals(st))
                {
                    throw new ServiceException("变更/作废仅适用于已备案通过的物料【" + m.getMaterialName() + "】");
                }
                break;
            default:
                throw new ServiceException("非法执行策略: " + executeType);
        }
    }

    /** 数据预检：复刻客户端校验规则，减少采集失败 */
    private void validate(BrMaterial m)
    {
        if ("1".equals(m.getIsBonded()))
        {
            if (StringUtils.isEmpty(m.getCodeTs()))
            {
                throw new ServiceException("保税货物必填 HS 编码（规范 §2.2.1）");
            }
            if (StringUtils.isEmpty(m.getgName()))
            {
                throw new ServiceException("保税货物必填 HS 商品名称（规范 §2.2.1）");
            }
            if (StringUtils.isEmpty(m.getUnit1()))
            {
                throw new ServiceException("保税货物必填法定计量单位（规范 §2.2.1）");
            }
        }
        if (StringUtils.isEmpty(m.getgModel()))
        {
            throw new ServiceException("商品规格型号必填");
        }
        if (StringUtils.isEmpty(m.getCalcUnit()))
        {
            throw new ServiceException("计量单位必填（联合业务主键）");
        }
    }
}
