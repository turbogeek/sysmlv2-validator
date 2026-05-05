$testDir = "E:\_Documents\git\SysML-v2-Release\sysml\src\examples\Simple Tests"
$jarPath = "E:\_Documents\git\sysml-validator\validator-cli\target\sysml-validator.jar"

$passed = 0
$failed = 0
$failedFiles = @()

Get-ChildItem -Path $testDir -Filter "*.sysml" | ForEach-Object {
    $result = & java -jar $jarPath $_.FullName 2>&1 | Out-String
    if ($result -match "VALIDATION PASSED") {
        Write-Host "PASS: $($_.Name)" -ForegroundColor Green
        $passed++
    } else {
        Write-Host "FAIL: $($_.Name)" -ForegroundColor Red
        $failed++
        $failedFiles += $_.Name
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "SUMMARY" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Passed: $passed/$($passed + $failed) ($([math]::Round($passed/($passed+$failed)*100, 1))%)" -ForegroundColor Green
Write-Host "Failed: $failed/$($passed + $failed)" -ForegroundColor Red

if ($failedFiles.Count -gt 0) {
    Write-Host "`nFailing files:" -ForegroundColor Yellow
    $failedFiles | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
}
