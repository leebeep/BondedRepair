# M3 E2E test: sample 01 second-half flow (SDO2 outbound + receipt + reconcile)

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

# ===== Phase A: create + audit + declare outbound (skip if already passed) =====
$mlist = GetJson '/bonded/material/list?pageSize=50'
$fid = ($mlist.rows | Where-Object { $_.materialCd -eq 'BAT-F01' }).id
$aid = ($mlist.rows | Where-Object { $_.materialCd -eq 'BAT-A01' }).id
$svf = ($mlist.rows | Where-Object { $_.materialCd -eq 'BAT-M01-SVF' }).id
if (-not $fid -or -not $aid -or -not $svf) { throw 'materials missing, run test-m2.ps1 first' }

$oul = GetJson '/bonded/outbound/list?pageSize=20'
$exists = $oul.rows | Where-Object { $_.outboundNo -eq 'OUT-M3-01' }
if ($exists -and $exists.docStatus -eq '05') {
    Write-Host 'outbound already passed, skip Phase A'
} else {
    if (-not $exists) {
        # busiType=05 must include service-fee line (qty 0.1)
        PostJson '/bonded/outbound' @{
            outboundNo='OUT-M3-01'; relaEntNm='Thai EV Manufacturer Co., Ltd'; busiType='05'
            busiFormNum='REEXP-2026-001'; relaCustomsFormType='03'; relaCustomsFormNum='DEC-2026-001'
            inOutTime='2026-11-26 16:00:00'
            items = @(
                @{ materialId=$fid; goodsSourceType='i'; goodsSourceNo='RC-M2-01'; deliveryCnt=100; price=2600; currency='502'; busiDesc='reexport repaired goods' },
                @{ materialId=$aid; goodsSourceType='i'; goodsSourceNo='RC-M2-01'; deliveryCnt=20;  price=1500; currency='502'; busiDesc='reexport defective goods' },
                @{ materialId=$svf; goodsSourceType='i'; goodsSourceNo='RC-M2-01'; deliveryCnt=0.1; price=4600000; currency='502'; busiDesc='service fee' }
            )
        } | Out-Null
        Write-Host 'outbound created'
    }
    $oid = (GetJson '/bonded/outbound/list?pageSize=20').rows | Where-Object { $_.outboundNo -eq 'OUT-M3-01' } | Select-Object -First 1 -ExpandProperty id
    PostUrl "/bonded/outbound/audit/$oid" | Out-Null
    Write-Host "outbound audited: $oid"
    PostUrl "/bonded/outbound/declare/1/$oid" | Out-Null
    Write-Host "SDO2 declared: $oid"

    RunSql "$sqlDir\mock_sdo2_receipt.sql"
    Write-Host 'mock SDO2 receipt written, waiting 65s for poll...'
    Start-Sleep 65
}

# ===== Phase B: verify receipt + reconcile =====
$oul = GetJson '/bonded/outbound/list?pageSize=20'
$o = $oul.rows | Where-Object { $_.outboundNo -eq 'OUT-M3-01' }
Write-Host "outbound docStatus=$($o.docStatus) customsStatus=$($o.customsStatus)"
if ($o.docStatus -ne '05') { throw "SDO2 receipt not passed: docStatus=$($o.docStatus)" }

$rec = GetJson '/bonded/stock/reconcile'
Write-Host "reconcile allZero: $($rec.data.allZero)"
$rec.data.cards | ForEach-Object { Write-Host "  card markType=$($_.markType) balance=$($_.balance)" }
if (-not $rec.data.allZero) { throw 'reconcile not all zero' }

$bd = GetJson '/bonded/declare/board'
$bd.data | ForEach-Object { Write-Host "board $($_.bizType) $($_.resultStatus) = $($_.cnt)" }
Write-Host 'PHASE DONE - M3 E2E PASSED'
