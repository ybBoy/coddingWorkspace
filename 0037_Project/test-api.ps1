# Fitness Checkin API Test Script
# Usage: Run .\test-api.ps1 in PowerShell

$BASE_URL = "http://localhost:8080"
$headers = @{"Content-Type" = "application/json"}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Fitness Checkin API Test Suite" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Query all records
Write-Host "[1/8] Test: Query all records" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/api/checkin" -Method GET -Headers $headers
    Write-Host "  [OK] Success, $($response.Count) records total" -ForegroundColor Green
    if ($response.Count -gt 0) {
        Write-Host "  First record: $($response[0].checkinDate) - $($response[0].exerciseType) $($response[0].duration)min" -ForegroundColor Gray
    }
} catch {
    Write-Host "  [FAIL] Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 2: Add new record
Write-Host "[2/8] Test: Add new checkin record" -ForegroundColor Yellow
$testRecord = @{
    checkinDate = (Get-Date).ToString("yyyy-MM-dd")
    exerciseType = "running"
    duration = 30
    mood = "great"
    note = "API test record - nice weather"
}
try {
    $body = $testRecord | ConvertTo-Json
    $newRecord = Invoke-RestMethod -Uri "$BASE_URL/api/checkin" -Method POST -Headers $headers -Body $body
    Write-Host "  [OK] Added successfully, ID: $($newRecord.id)" -ForegroundColor Green
    $testId = $newRecord.id
} catch {
    Write-Host "  [FAIL] Error: $($_.Exception.Message)" -ForegroundColor Red
    $testId = $null
}
Write-Host ""

# Test 3: Filter by type
Write-Host "[3/8] Test: Filter by exercise type" -ForegroundColor Yellow
try {
    $type = "running"
    $encodedType = [System.Uri]::EscapeDataString($type)
    $filtered = Invoke-RestMethod -Uri "$BASE_URL/api/checkin?type=$encodedType" -Method GET -Headers $headers
    Write-Host "  [OK] Filter by '$type' success, $($filtered.Count) records" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 4: Query statistics
Write-Host "[4/8] Test: Query statistics" -ForegroundColor Yellow
try {
    $stats = Invoke-RestMethod -Uri "$BASE_URL/api/stats" -Method GET -Headers $headers
    Write-Host "  [OK] Success" -ForegroundColor Green
    Write-Host "    Weekly minutes: $($stats.weeklyMinutes)" -ForegroundColor Gray
    Write-Host "    Streak days: $($stats.streakDays)" -ForegroundColor Gray
    Write-Host "    Weekly goal: $($stats.weeklyGoal)" -ForegroundColor Gray
    Write-Host "    Total count: $($stats.totalCount)" -ForegroundColor Gray
} catch {
    Write-Host "  [FAIL] Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 5: Update record (full fields)
Write-Host "[5/8] Test: Update record (full fields)" -ForegroundColor Yellow
if ($testId) {
    $updateData = @{
        checkinDate = (Get-Date).AddDays(-1).ToString("yyyy-MM-dd")
        exerciseType = "cycling"
        duration = 45
        mood = "good"
        note = "Updated - changed to cycling"
    }
    try {
        $body = $updateData | ConvertTo-Json
        $updated = Invoke-RestMethod -Uri "$BASE_URL/api/checkin/$testId" -Method PUT -Headers $headers -Body $body
        Write-Host "  [OK] Updated successfully" -ForegroundColor Green
        Write-Host "    New date: $($updated.checkinDate)" -ForegroundColor Gray
        Write-Host "    New type: $($updated.exerciseType)" -ForegroundColor Gray
        Write-Host "    New duration: $($updated.duration) min" -ForegroundColor Gray
    } catch {
        Write-Host "  [FAIL] Error: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "  [SKIP] Skipped (no ID from add test)" -ForegroundColor Gray
}
Write-Host ""

# Test 6: Update weekly goal
Write-Host "[6/8] Test: Update weekly goal" -ForegroundColor Yellow
try {
    $goalData = @{ weeklyGoal = 200 } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BASE_URL/api/settings/goal" -Method PUT -Headers $headers -Body $goalData | Out-Null
    Write-Host "  [OK] Goal set successfully" -ForegroundColor Green

    $goalResp = Invoke-RestMethod -Uri "$BASE_URL/api/settings/goal" -Method GET -Headers $headers
    Write-Host "    Current goal: $($goalResp.weeklyGoal) min" -ForegroundColor Gray
} catch {
    Write-Host "  [FAIL] Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 7: Delete record
Write-Host "[7/8] Test: Delete record" -ForegroundColor Yellow
if ($testId) {
    try {
        Invoke-RestMethod -Uri "$BASE_URL/api/checkin/$testId" -Method DELETE -Headers $headers | Out-Null
        Write-Host "  [OK] Deleted successfully" -ForegroundColor Green

        $after = Invoke-RestMethod -Uri "$BASE_URL/api/checkin" -Method GET -Headers $headers
        Write-Host "    Total after delete: $($after.Count)" -ForegroundColor Gray
    } catch {
        Write-Host "  [FAIL] Error: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "  [SKIP] Skipped (no ID to delete)" -ForegroundColor Gray
}
Write-Host ""

# Test 8: Data import
Write-Host "[8/8] Test: Data import (merge mode)" -ForegroundColor Yellow
$importData = @(
    @{
        id = "import-test-001"
        checkinDate = "2026-01-01"
        exerciseType = "swimming"
        duration = 50
        mood = "great"
        note = "New Year swim"
    },
    @{
        id = "import-test-002"
        checkinDate = "2026-01-02"
        exerciseType = "yoga"
        duration = 60
        mood = "good"
        note = "New Year yoga"
    }
)
try {
    $body = $importData | ConvertTo-Json
    $importResult = Invoke-RestMethod -Uri "$BASE_URL/api/checkin/import" -Method POST -Headers $headers -Body $body
    Write-Host "  [OK] Imported $($importResult.imported) records" -ForegroundColor Green
    Write-Host "    Overwrite mode: $($importResult.overwrite)" -ForegroundColor Gray

    # Cleanup test import data
    Invoke-RestMethod -Uri "$BASE_URL/api/checkin/import-test-001" -Method DELETE -Headers $headers | Out-Null
    Invoke-RestMethod -Uri "$BASE_URL/api/checkin/import-test-002" -Method DELETE -Headers $headers | Out-Null
    Write-Host "    Test data cleaned up" -ForegroundColor Gray
} catch {
    Write-Host "  [FAIL] Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  All tests completed!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Additional verification:" -ForegroundColor Yellow
Write-Host "  - Check data/checkins.json exists" -ForegroundColor Gray
Write-Host "  - Check data/checkins.json.bak backup file" -ForegroundColor Gray
Write-Host "  - Data should persist after server restart" -ForegroundColor Gray
Write-Host "  - Visit $BASE_URL in browser for UI" -ForegroundColor Gray
