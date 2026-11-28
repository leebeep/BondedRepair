package com.ruoyi.bonded.task;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.ruoyi.bonded.mapper.BrSyncMapper;
import com.ruoyi.bonded.mapper.CustomsMirrorMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.common.utils.StringUtils;

/**
 * 主动查询补偿（公服查询接口 executeType=4）：
 * 扫描已采集/申报中(20/21) 超 30 分钟无回执的 uuid，调公服查询接口，
 * 回执写回镜像表，再由 BrReceiptPollTask 统一回读更新业务单据。
 *
 * 配置（均为 sys_config 参数设置项；url/appKey 任一未配置则任务自动跳过，仅镜像轮询兜底）：
 *   br.query.api.url        公服查询接口地址（规范 §2.3.2 固定值）
 *   br.query.api.appKey     x-app-key（公服企业注册后分配）
 *   br.query.api.privateKey RSA 私钥（PKCS#8 PEM；sys_config.config_value 已扩至 varchar(4000) 可存）
 *   application.yml br.query.api.privateKey 为兜底读取（sys_config 优先）
 */
@Service
public class CustomsQueryService
{
    private static final Logger log = LoggerFactory.getLogger(CustomsQueryService.class);

    public static final String CFG_URL = "br.query.api.url";
    public static final String CFG_APP_KEY = "br.query.api.appKey";
    public static final String CFG_PRIVATE_KEY = "br.query.api.privateKey";

    @Autowired private ISysConfigService configService;
    @Autowired private BrSyncMapper syncMapper;
    @Autowired private CustomsMirrorMapper mirrorMapper;

    @Value("${br.query.api.privateKey:}")
    private String privateKeyFromYml;

    private final RestTemplate restTemplate = new RestTemplate();

    /** 主动查询是否可用：url + appKey 均已配置（前端据此置灰"立即主动查询"按钮） */
    public boolean isConfigured()
    {
        return StringUtils.isNotEmpty(configService.selectConfigByKey(CFG_URL))
                && StringUtils.isNotEmpty(configService.selectConfigByKey(CFG_APP_KEY));
    }

    /**
     * 自动触发：每天一次（默认凌晨 03:00，可用 br.query.cron 配置）；
     * 手动触发：联网日志模块【立即主动查询】按钮。
     * 覆盖节奏：回执缺失 30 分钟后才纳入查询范围，一次日查足以兜底。
     */
    @Scheduled(cron = "${br.query.cron:0 0 3 * * ?}")
    public void scheduled()
    {
        queryOnce();
    }

    /** 可由联网日志模块手动触发；返回本次查询 uuid 数 */
    public int queryOnce()
    {
        String url = configService.selectConfigByKey(CFG_URL);
        String appKey = configService.selectConfigByKey(CFG_APP_KEY);
        String privateKey = configService.selectConfigByKey(CFG_PRIVATE_KEY);
        // 私钥优先取参数设置，未配置时回退 application.yml（varchar(500) 存不下完整 PEM）
        if (StringUtils.isEmpty(privateKey)) { privateKey = privateKeyFromYml; }
        if (StringUtils.isEmpty(url) || StringUtils.isEmpty(appKey))
        {
            return 0;
        }
        int queried = 0;
        queried += queryTable("PTO2", syncMapper.selectQueryUuidsInbound(), url, appKey, privateKey);
        queried += queryTable("RRD2", syncMapper.selectQueryUuidsIssue(), url, appKey, privateKey);
        queried += queryTable("MSO2", syncMapper.selectQueryUuidsReceive(), url, appKey, privateKey);
        queried += queryTable("SDO2", syncMapper.selectQueryUuidsOutbound(), url, appKey, privateKey);
        queried += queryTable("EPD101", syncMapper.selectQueryUuidsMaterial(), url, appKey, privateKey);
        if (queried > 0)
        {
            log.info("主动查询完成，覆盖 {} 条回执", queried);
        }
        return queried;
    }

    @SuppressWarnings("unchecked")
    private int queryTable(String bizType, List<String> uuids, String url, String appKey, String privateKey)
    {
        if (uuids == null || uuids.isEmpty())
        {
            return 0;
        }
        int covered = 0;
        for (String uuid : uuids)
        {
            try
            {
                Map<String, String> payload = new HashMap<>();
                payload.put("executeType", "4");
                payload.put("bizType", bizType);
                payload.put("uuid", uuid);
                String body = com.alibaba.fastjson2.JSON.toJSONString(payload);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("x-app-key", appKey);
                if (StringUtils.isNotEmpty(privateKey))
                {
                    headers.set("x-signature", sign(body, privateKey));
                }

                Map<String, Object> resp = restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
                if (resp == null)
                {
                    continue;
                }
                Object dataObj = resp.get("data");
                Map<String, Object> receipt = null;
                if (dataObj instanceof Map)
                {
                    receipt = (Map<String, Object>) dataObj;
                }
                else if (dataObj instanceof List && !((List<Object>) dataObj).isEmpty())
                {
                    Object first = ((List<Object>) dataObj).get(0);
                    if (first instanceof Map) { receipt = (Map<String, Object>) first; }
                }
                if (receipt == null || StringUtils.isEmpty((String) receipt.get("resultStatus")))
                {
                    continue;
                }
                String resultStatus = String.valueOf(receipt.get("resultStatus"));
                String msg = (String) receipt.get("msg");
                Object rt = receipt.get("resultTime");
                Date resultTime = rt instanceof Date ? (Date) rt : new Date();
                covered += updateMirror(bizType, uuid, resultStatus, msg, resultTime);
            }
            catch (Exception e)
            {
                log.warn("{} 主动查询失败 uuid={}: {}", bizType, uuid, e.getMessage());
            }
        }
        return covered;
    }

    private int updateMirror(String bizType, String uuid, String resultStatus, String msg, Date resultTime)
    {
        switch (bizType)
        {
            case "EPD101": return mirrorMapper.updateReceiptEpd101(uuid, resultStatus, msg, resultTime);
            case "PTO2": return mirrorMapper.updateReceiptPto2(uuid, resultStatus, msg, resultTime);
            case "RRD2": return mirrorMapper.updateReceiptRrd2(uuid, resultStatus, msg, resultTime);
            case "MSO2": return mirrorMapper.updateReceiptMso2(uuid, resultStatus, msg, resultTime);
            case "SDO2": return mirrorMapper.updateReceiptSdo2(uuid, resultStatus, msg, resultTime);
            default: return 0;
        }
    }

    /** RSA-SHA256 签名（PKCS#8），Base64 输出；协议细节以《规范 v1.4》为准 */
    private String sign(String body, String privateKeyPem)
    {
        try
        {
            String pem = privateKeyPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(pem);
            PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(key);
            sig.update(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig.sign());
        }
        catch (Exception e)
        {
            throw new IllegalStateException("私钥签名失败，请检查 br.query.api.privateKey 配置", e);
        }
    }
}
