package com.ruoyi.bonded.service.impl;

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

    @Value("${br.enterpCd}")
    private String enterpCd;

    @Value("${br.enterpNm}")
    private String enterpNm;

    @Value("${br.masterCuscd}")
    private String masterCuscd;

    /** 六类标记 → 品名后缀（公告203号第七条口径） */
    public static String markSuffix(String markType)
    {
        switch (markType)
        {
            case "1": return "（待修复）";
            case "2": return "（已修复）";
            case "3": return "（无法修复）";
            case "4": return "（维修用保税料件）";
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
    public int insertBrMaterial(BrMaterial material)
    {
        material.setVersion(StringUtils.defaultString(material.getVersion(), "1"));
        if (materialMapper.selectBrMaterialByBizKey(material) != null)
        {
            throw new ServiceException("物料编号+版本号+计量单位已存在（备案联合业务主键唯一）");
        }
        validate(material);
        material.setCreateBy(SecurityUtils.getUsername());
        return materialMapper.insertBrMaterial(material);
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
        return materialMapper.updateBrMaterial(material);
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
            log.setExecuteType(executeType);
            log.setUuid(StringUtils.defaultString(lastUuid, StringUtils.EMPTY));
            declareLogMapper.insertBrDeclareLog(log);

            materialMapper.updateDeclareResult(id, lastUuid, "1", "10", executeType);
            count++;
        }
        return count;
    }

    /** 构造 EPD101 镜像行（品名自动拼接六类标记后缀） */
    private Epd101Data buildEpd101(BrMaterial m, String executeType)
    {
        Epd101Data d = new Epd101Data();
        d.setExecuteType(executeType);
        d.setDclTypecd(executeType);
        d.setEnterpCd(enterpCd);
        d.setEnterpNm(enterpNm);
        d.setMasterCuscd(masterCuscd);
        d.setIntProductNo(m.getMaterialCd());
        d.setIntProductName(m.getMaterialName() + markSuffix(m.getMarkType()));
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
                if (!"2".equals(st) && !"1".equals(st))
                {
                    throw new ServiceException("变更/作废仅适用于已申报的物料【" + m.getMaterialName() + "】");
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
                throw new ServiceException("保税料件必填 HS 编码（规范 §2.2.1）");
            }
            if (StringUtils.isEmpty(m.getgName()))
            {
                throw new ServiceException("保税料件必填 HS 商品名称（规范 §2.2.1）");
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
