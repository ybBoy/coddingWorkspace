# 运动打卡 API 测试脚本
# 使用方法：在 PowerShell 中运行 .\test-api.ps1

$BASE_URL = "http://localhost:8080"
$headers = @{"Content-Type" = "application/json"}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  运动打卡 API 测试脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 测试 1: 查询所有记录
Write-Host "[1/8] 测试: 查询所有记录" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/api/checkin" -Method GET -Headers $headers
    Write-Host "  ✓ 成功，共 $($response.Count) 条记录" -ForegroundColor Green
    if ($response.Count -gt 0) {
        Write-Host "  最新一条: $($response[0].checkinDate) - $($response[0].exerciseType) $($response[0].duration)分钟" -ForegroundColor Gray
    }
} catch {
    Write-Host "  ✗ 失败: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 测试 2: 新增记录
Write-Host "[2/8] 测试: 新增打卡记录" -ForegroundColor Yellow
$testRecord = @{
    checkinDate = (Get-Date).ToString("yyyy-MM-dd")
    exerciseType = "测试跑步"
    duration = 30
    mood = "很棒"
    note = "API测试记录 - 今天天气很好"
}
try {
    $body = $testRecord | ConvertTo-Json
    $newRecord = Invoke-RestMethod -Uri "$BASE_URL/api/checkin" -Method POST -Headers $headers -Body $body
    Write-Host "  ✓ 新增成功，ID: $($newRecord.id)" -ForegroundColor Green
    $testId = $newRecord.id
} catch {
    Write-Host "  ✗ 失败: $($_.Exception.Message)" -ForegroundColor Red
    $testId = $null
}
Write-Host ""

# 测试 3: 按类型筛选
Write-Host "[3/8] 测试: 按运动类型筛选" -ForegroundColor Yellow
try {
    $type = "跑步"
    $encodedType = [System.Uri]::EscapeDataString($type)
    $filtered = Invoke-RestMethod -Uri "$BASE_URL/api/checkin?type=$encodedType" -Method GET -Headers $headers
    Write-Host "  ✓ 筛选'$type'成功，共 $($filtered.Count) 条" -ForegroundColor Green
} catch {
    Write-Host "  ✗ 失败: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 测试 4: 查询统计信息
Write-Host "[4/8] 测试: 查询统计信息" -ForegroundColor Yellow
try {
    $stats = Invoke-RestMethod -Uri "$BASE_URL/api/stats" -Method GET -Headers $headers
    Write-Host "  ✓ 成功" -ForegroundColor Green
    Write-Host "    本周运动: $($stats.weeklyMinutes) 分钟" -ForegroundColor Gray
    Write-Host "    连续打卡: $($stats.streakDays) 天" -ForegroundColor Gray
    Write-Host "    周目标: $($stats.weeklyGoal) 分钟" -ForegroundColor Gray
    Write-Host "    总记录数: $($stats.totalCount)" -ForegroundColor Gray
} catch {
    Write-Host "  ✗ 失败: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 测试 5: 修改记录（完整字段）
Write-Host "[5/8] 测试: 修改记录（完整字段）" -ForegroundColor Yellow
if ($testId) {
    $updateData = @{
        checkinDate = (Get-Date).AddDays(-1).ToString("yyyy-MM-dd")
        exerciseType = "骑行"
        duration = 45
        mood = "不错"
        note = "已修改 - 改为骑行"
    }
    try {
        $body = $updateData | ConvertTo-Json
        $updated = Invoke-RestMethod -Uri "$BASE_URL/api/checkin/$testId" -Method PUT -Headers $headers -Body $body
        Write-Host "  ✓ 修改成功" -ForegroundColor Green
        Write-Host "    新日期: $($updated.checkinDate)" -ForegroundColor Gray
        Write-Host "    新类型: $($updated.exerciseType)" -ForegroundColor Gray
        Write-Host "    新时长: $($updated.duration) 分钟" -ForegroundColor Gray
    } catch {
        Write-Host "  ✗ 失败: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "  - 跳过（新增失败无ID）" -ForegroundColor Gray
}
Write-Host ""

# 测试 6: 修改周目标
Write-Host "[6/8] 测试: 修改周目标" -ForegroundColor Yellow
try {
    $goalData = @{ weeklyGoal = 200 } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BASE_URL/api/settings/goal" -Method PUT -Headers $headers -Body $goalData | Out-Null
    Write-Host "  ✓ 设置目标成功" -ForegroundColor Green

    $goalResp = Invoke-RestMethod -Uri "$BASE_URL/api/settings/goal" -Method GET -Headers $headers
    Write-Host "    当前目标: $($goalResp.weeklyGoal) 分钟" -ForegroundColor Gray
} catch {
    Write-Host "  ✗ 失败: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 测试 7: 删除记录
Write-Host "[7/8] 测试: 删除记录" -ForegroundColor Yellow
if ($testId) {
    try {
        Invoke-RestMethod -Uri "$BASE_URL/api/checkin/$testId" -Method DELETE -Headers $headers | Out-Null
        Write-Host "  ✓ 删除成功" -ForegroundColor Green

        $after = Invoke-RestMethod -Uri "$BASE_URL/api/checkin" -Method GET -Headers $headers
        Write-Host "    删除后总数: $($after.Count)" -ForegroundColor Gray
    } catch {
        Write-Host "  ✗ 失败: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "  - 跳过（无ID可删）" -ForegroundColor Gray
}
Write-Host ""

# 测试 8: 数据导入
Write-Host "[8/8] 测试: 数据导入（合并模式）" -ForegroundColor Yellow
$importData = @(
    @{
        id = "import-test-001"
        checkinDate = "2026-01-01"
        exerciseType = "游泳"
        duration = 50
        mood = "很棒"
        note = "元旦游泳"
    },
    @{
        id = "import-test-002"
        checkinDate = "2026-01-02"
        exerciseType = "瑜伽"
        duration = 60
        mood = "不错"
        note = "新年瑜伽"
    }
)
try {
    $body = $importData | ConvertTo-Json
    $importResult = Invoke-RestMethod -Uri "$BASE_URL/api/checkin/import" -Method POST -Headers $headers -Body $body
    Write-Host "  ✓ 导入成功，导入 $($importResult.imported) 条" -ForegroundColor Green
    Write-Host "    覆盖模式: $($importResult.overwrite)" -ForegroundColor Gray

    # 清理测试导入数据
    Invoke-RestMethod -Uri "$BASE_URL/api/checkin/import-test-001" -Method DELETE -Headers $headers | Out-Null
    Invoke-RestMethod -Uri "$BASE_URL/api/checkin/import-test-002" -Method DELETE -Headers $headers | Out-Null
    Write-Host "    已清理测试数据" -ForegroundColor Gray
} catch {
    Write-Host "  ✗ 失败: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  测试完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "附加验证:" -ForegroundColor Yellow
Write-Host "  • 检查 data/checkins.json 文件是否存在" -ForegroundColor Gray
Write-Host "  • 检查 data/checkins.json.bak 备份文件" -ForegroundColor Gray
Write-Host "  • 重启服务器后数据应能恢复" -ForegroundColor Gray
Write-Host "  • 浏览器访问 $BASE_URL 查看页面" -ForegroundColor Gray
