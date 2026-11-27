package com.ruoyi.web.controller.bonded;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.bonded.domain.BrDeclareLog;
import com.ruoyi.bonded.mapper.BrDeclareLogMapper;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 申报管理 Controller（看板 + 日志）
 */
@RestController
@RequestMapping("/bonded/declare")
public class BrDeclareController extends BaseController
{
    @Autowired
    private BrDeclareLogMapper declareLogMapper;

    /** 看板：五类报文按回执状态计数 */
    @PreAuthorize("@ss.hasPermi('bonded:declare:list')")
    @GetMapping("/board")
    public AjaxResult board()
    {
        return success(declareLogMapper.selectBoard());
    }

    /** 申报日志明细（分页） */
    @PreAuthorize("@ss.hasPermi('bonded:declare:list')")
    @GetMapping("/log")
    public TableDataInfo log(BrDeclareLog query)
    {
        startPage();
        List<BrDeclareLog> list = declareLogMapper.selectLogList(query);
        return getDataTable(list);
    }
}
