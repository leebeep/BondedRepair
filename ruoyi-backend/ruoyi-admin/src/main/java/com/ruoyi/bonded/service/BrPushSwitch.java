package com.ruoyi.bonded.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 联网自动推送总开关（sys_config: br.auto.push.enabled，true=审核/保存自动写镜像库）
 */
@Component
public class BrPushSwitch
{
    public static final String KEY = "br.auto.push.enabled";

    @Autowired
    private ISysConfigService configService;

    public boolean isEnabled()
    {
        return "true".equalsIgnoreCase(configService.selectConfigByKey(KEY));
    }

    public void setEnabled(boolean enabled)
    {
        String val = String.valueOf(enabled);
        SysConfig query = new SysConfig();
        query.setConfigKey(KEY);
        List<SysConfig> list = configService.selectConfigList(query);
        if (list == null || list.isEmpty())
        {
            SysConfig c = new SysConfig();
            c.setConfigKey(KEY);
            c.setConfigName("联网自动推送开关");
            c.setConfigValue(val);
            c.setConfigType("Y");
            c.setRemark("true=单据审核/物料保存自动写入镜像库；false=仅人工变更/作废/补推写入");
            c.setCreateBy(SecurityUtils.getUsername());
            configService.insertConfig(c);
        }
        else
        {
            SysConfig c = list.get(0);
            c.setConfigValue(val);
            c.setUpdateBy(SecurityUtils.getUsername());
            configService.updateConfig(c);
        }
    }
}
