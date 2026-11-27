package com.ruoyi.bonded.domain;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 库存对账汇总行（按六类标记聚合台账流水）
 */
public class StockReconcileRow implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 六类标记 1/2/3/4/5/6/8 */
    private String markType;

    /** 收入合计（IN，正） */
    private BigDecimal inQty;

    /** 发料合计（ISSUE，负） */
    private BigDecimal issueQty;

    /** 收料合计（RCV，正） */
    private BigDecimal rcvQty;

    /** 出库合计（OUT，负） */
    private BigDecimal outQty;

    /** 账存（范围内净额 = 收入-发料+收料-出库） */
    private BigDecimal balance;

    public String getMarkType() { return markType; }
    public void setMarkType(String markType) { this.markType = markType; }
    public BigDecimal getInQty() { return inQty; }
    public void setInQty(BigDecimal inQty) { this.inQty = inQty; }
    public BigDecimal getIssueQty() { return issueQty; }
    public void setIssueQty(BigDecimal issueQty) { this.issueQty = issueQty; }
    public BigDecimal getRcvQty() { return rcvQty; }
    public void setRcvQty(BigDecimal rcvQty) { this.rcvQty = rcvQty; }
    public BigDecimal getOutQty() { return outQty; }
    public void setOutQty(BigDecimal outQty) { this.outQty = outQty; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
