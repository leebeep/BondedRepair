# M2 E2E test: sample 01 first-half flow (phase A: materials pass, phase B: business flow)
$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080'
$sqlDir = 'd:\Filez\app\development\BondedRepair\sql'

function RunSql($file) {
    docker cp $file br-mysql:/tmp/run.sql
    try { docker exec br-mysql sh -c "mysql --default-character-set=utf8mb4 -uroot -pbr_root_2026 bonded_repair < /tmp/run.sql" 2>$null | Out-Null } catch { }
}

$login = Invoke-RestMethod -Uri "$base/login" -Method POST -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}'
$h = @{ Authorization = "Bearer $($login.token)" }
function PostJson($url, $obj) {
    $json = $obj | ConvertTo-Json -Depth 6 -Compress
    Invoke-RestMethod -Uri "$base$url" -Method POST -ContentType "application/json;charset=utf-8" -Headers $h -Body ([Text.Encoding]::UTF8.GetBytes($json))
}
function GetJson($url) { Invoke-RestMethod -Uri "$base$url" -Method GET -Headers $h }
function PostUrl($url) { Invoke-RestMethod -Uri "$base$url" -Method POST -Headers $h }

$mats = @(
    @{ materialCd='BAT-M01'; markType='1'; gType='I'; gModel='CTP-100Ah' },
    @{ materialCd='SCREW-M4'; markType='4'; gType='I'; codeTs='7318150090'; gName='钢制螺钉'; gModel='M4x10' },
    @{ materialCd='BAT-F01'; markType='2'; gType='E'; gModel='CTP-100Ah-F' },
    @{ materialCd='BAT-A01'; markType='3'; gType='E'; gModel='CTP-100Ah-X' }
)

# ===== Phase A: create + declare 4 materials, mock receipts, wait =====
$mlist = GetJson '/bonded/material/list?pageSize=50'
if ($mlist.rows.Count -ge 4 -and ($mlist.rows | Where-Object { $_.declareStatus -ne '2' }).Count -eq 0) {
    Write-Host 'materials already passed, skip Phase A creation'
} else {
    if ($mlist.rows.Count -lt 4) {
        $cust = PostJson '/bonded/customer' @{ customerName = 'Thai EV Manufacturer Co., Ltd'; countryCode = '764' }
        Write-Host "customer: $($cust.code)"
        foreach ($m in $mats) {
            PostJson '/bonded/material' @{
                materialCd=$m.materialCd; version='1'; calcUnit='004'; materialName='新能源动力电池模组'
                markType=$m.markType; gType=$m.gType; isBonded='1'; codeTs='8507600090'; gName='锂电池模组'; gModel=$m.gModel
                remark = $(if ($m.materialCd -eq 'SCREW-M4') { 'M4 precision screw' } else { $null })
            } | Out-Null
        }
        $mlist = GetJson '/bonded/material/list?pageSize=50'
        $ids = ($mlist.rows | ForEach-Object { $_.id }) -join ','
        PostUrl "/bonded/material/declare/1/$ids" | Out-Null
        Write-Host "EPD101 declared: $ids"
    }
    RunSql "$sqlDir\mock_epd101.sql"
    Write-Host "mock receipts written, waiting 65s for poll..."
    Start-Sleep 65
    $mlist = GetJson '/bonded/material/list?pageSize=50'
}

$mid  = ($mlist.rows | Where-Object { $_.materialCd -eq 'BAT-M01' }).id
$sid  = ($mlist.rows | Where-Object { $_.materialCd -eq 'SCREW-M4' }).id
$fid  = ($mlist.rows | Where-Object { $_.materialCd -eq 'BAT-F01' }).id
$aid  = ($mlist.rows | Where-Object { $_.materialCd -eq 'BAT-A01' }).id
$notPassed = ($mlist.rows | Where-Object { $_.declareStatus -ne '2' }).Count
if ($notPassed -gt 0) { throw "materials not passed: $notPassed" }
Write-Host "all materials passed. ids: M=$mid S=$sid F=$fid A=$aid"

# ===== Phase B: business flow (工单号/批次号/服务费随单据录入，自动建档) =====

PostJson '/bonded/inbound' @{
    inboundNo='IN-M2-01'; relaEntNm='Thai EV Manufacturer Co., Ltd'; busiType='04'
    relaCustomsFormType='03'; relaCustomsFormNum='DEC-2026-001'; inOutTime='2026-11-26 09:00:00'
    items = @(@{ materialId=$mid; storageCnt=120; price=2100; currency='502'; specificationsModels='CTP-100Ah'; busiFormNum='PO-2026-001'; unit1Num=120; goodsSourceType='g'; goodsSourceNo='PO-2026-001' })
} | Out-Null
PostJson '/bonded/inbound' @{
    inboundNo='IN-M2-02'; relaEntNm='Thai EV Manufacturer Co., Ltd'; busiType='05'
    relaCustomsFormType='03'; relaCustomsFormNum='DEC-2026-001'; inOutTime='2026-11-26 09:10:00'
    items = @(@{ materialId=$sid; storageCnt=200; price=12.5; currency='502'; specificationsModels='M4x10'; unit1Num=200 })
} | Out-Null
$inl = GetJson '/bonded/inbound/list?pageSize=10'
$i1 = ($inl.rows | Where-Object { $_.inboundNo -eq 'IN-M2-01' }).id
$i2 = ($inl.rows | Where-Object { $_.inboundNo -eq 'IN-M2-02' }).id
PostUrl "/bonded/inbound/audit/$i1" | Out-Null
PostUrl "/bonded/inbound/audit/$i2" | Out-Null
PostUrl "/bonded/inbound/declare/1/$i1,$i2" | Out-Null
Write-Host "PTO2 declared: $i1,$i2"

PostJson '/bonded/issue' @{
    issueNo='IS-M2-01'; workNo='WO-2026-001'; batchNo='B202611-001'; issueTime='2026-11-26 10:00:00'
    items = @(
        @{ materialId=$mid; goodsSourceType='g'; goodsSourceNo='IN-M2-01'; issueQty=120 },
        @{ materialId=$sid; goodsSourceType='g'; goodsSourceNo='IN-M2-02'; issueQty=150 }
    )
} | Out-Null
$isd = (GetJson '/bonded/issue/list').rows[0].id
PostUrl "/bonded/issue/audit/$isd" | Out-Null
PostUrl "/bonded/issue/declare/1/$isd" | Out-Null
Write-Host "RRD2 declared: $isd"

PostJson '/bonded/receive' @{
    receiveNo='RC-M2-01'; workNo='WO-2026-001'; batchNo='B202611-001'
    serviceFee=460000; currency='142'; receiveTime='2026-11-26 15:00:00'
    items = @(
        @{ materialId=$fid; materialNum=100; returnType='5' },
        @{ materialId=$aid; materialNum=20;  returnType='a' },
        @{ materialId=$sid; materialNum=50;  returnType='1' }
    )
} | Out-Null
$rcd = (GetJson '/bonded/receive/list').rows[0].id
$ra = PostUrl "/bonded/receive/audit/$rcd"
Write-Host "receive audit: $($ra.code)"
PostUrl "/bonded/receive/declare/1/$rcd" | Out-Null
Write-Host "MSO2 declared: $rcd"

# mock receipts for PTO2/RRD2/MSO2 mirrors
RunSql "$sqlDir\mock_biz_receipts.sql"
Write-Host "mock biz receipts written, waiting 65s for poll..."
Start-Sleep 65
Write-Host "PHASE DONE - verify in DB"
