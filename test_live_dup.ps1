$temp = [System.IO.Path]::GetTempPath()
$jpg = Join-Path $temp "real_sample.jpg"

$cmd1 = "curl.exe -s -F `"file=@$jpg;type=image/jpeg`" http://localhost:8001/verify"
$res1 = Invoke-Expression $cmd1
Write-Host "Response 1:"
Write-Host $res1

$obj1 = $res1 | ConvertFrom-Json
$sha = $obj1.sha256_hash
$phash = $obj1.phash

$cmd2 = "curl.exe -s -F `"file=@$jpg;type=image/jpeg`" -F `"existing_sha256=$sha`" -F `"existing_phashes=$phash`" http://localhost:8001/verify"
$res2 = Invoke-Expression $cmd2
Write-Host "`nResponse 2 (Duplicate Check):"
Write-Host $res2

$obj2 = $res2 | ConvertFrom-Json
if ($obj2.exact_duplicate -eq $true -and $obj2.verification_status -eq "DUPLICATE_DETECTED") {
    Write-Host "`nPASS: Scenario 6 - Live AI microservice detected exact SHA-256 duplicate photo!"
} else {
    Write-Host "`nFAIL: Live duplicate detection failed."
}
