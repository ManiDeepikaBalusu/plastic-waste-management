$baseUrl = "http://localhost:8080/api/v1"

# 1. Admin Login
$adminLogin = @{ email = "admin@plasticwaste.org"; password = "AdminPassword2026!" } | ConvertTo-Json
$adminRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $adminLogin -ContentType "application/json"
$adminToken = $adminRes.token

# 2. Register Citizen & Submit Complaint
$citEmail = "cit.race." + [guid]::NewGuid().ToString().Substring(0,6) + "@test.org"
$cit = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -Body (@{ fullName="Race Citizen"; email=$citEmail; password="Password123!"; role="CITIZEN" } | ConvertTo-Json) -ContentType "application/json"

$tempDir = [System.IO.Path]::GetTempPath()
$validJpgPath = Join-Path $tempDir "valid_sample.jpg"
$compPath = Join-Path $tempDir "comp_race.json"
@{ title="Concurrent Spot"; description="Testing race condition"; wasteType="PLASTIC_MIXED"; latitude=12.91; longitude=77.61 } | ConvertTo-Json | Set-Content -Path $compPath

$raw = & curl.exe -s -H "Authorization: Bearer $($cit.token)" -F "data=@$compPath;type=application/json" -F "files=@$validJpgPath;type=image/jpeg" "$baseUrl/complaints/submit-with-photos"
$comp = $raw | ConvertFrom-Json
$compId = $comp.id
Write-Host "Created complaint: $compId"

# 3. Transition to VERIFIED
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
Invoke-RestMethod -Uri "$baseUrl/complaints/$compId/review" -Method Patch -Headers $adminHeaders | Out-Null
Invoke-RestMethod -Uri "$baseUrl/complaints/$compId/verify" -Method Patch -Headers $adminHeaders | Out-Null
Write-Host "Complaint $compId moved to VERIFIED"

# 4. Register two volunteers
$v1Email = "vol1." + [guid]::NewGuid().ToString().Substring(0,6) + "@test.org"
$v1 = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -Body (@{ fullName="Vol One"; email=$v1Email; password="Password123!"; role="VOLUNTEER" } | ConvertTo-Json) -ContentType "application/json"

$v2Email = "vol2." + [guid]::NewGuid().ToString().Substring(0,6) + "@test.org"
$v2 = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -Body (@{ fullName="Vol Two"; email=$v2Email; password="Password123!"; role="VOLUNTEER" } | ConvertTo-Json) -ContentType "application/json"

# 5. Concurrently fire assignments using PowerShell jobs
Write-Host "Triggering concurrent assignment requests..."
$body1 = @{ complaintId = $compId; volunteerId = $v1.user.id; notes = "Assigned to Vol 1" } | ConvertTo-Json
$body2 = @{ complaintId = $compId; volunteerId = $v2.user.id; notes = "Assigned to Vol 2" } | ConvertTo-Json

$job1 = Start-Job -ScriptBlock {
    param($url, $token, $b)
    try {
        $res = Invoke-RestMethod -Uri "$url/assignments" -Method Post -Body $b -ContentType "application/json" -Headers @{ Authorization = "Bearer $token" }
        return @{ code = 200; status = $res.status }
    } catch {
        return @{ code = $_.Exception.Response.StatusCode.value__; err = $_.Exception.Message }
    }
} -ArgumentList $baseUrl, $adminToken, $body1

$job2 = Start-Job -ScriptBlock {
    param($url, $token, $b)
    try {
        $res = Invoke-RestMethod -Uri "$url/assignments" -Method Post -Body $b -ContentType "application/json" -Headers @{ Authorization = "Bearer $token" }
        return @{ code = 200; status = $res.status }
    } catch {
        return @{ code = $_.Exception.Response.StatusCode.value__; err = $_.Exception.Message }
    }
} -ArgumentList $baseUrl, $adminToken, $body2

$r1 = Receive-Job -Job $job1 -Wait
$r2 = Receive-Job -Job $job2 -Wait
Remove-Job $job1, $job2

Write-Host "Job 1 response code: $($r1.code)"
Write-Host "Job 2 response code: $($r2.code)"

# Check total assignments for this complaint via volunteer assignment endpoints
$vol1Tasks = Invoke-RestMethod -Uri "$baseUrl/assignments/my-tasks" -Method Get -Headers @{ Authorization = "Bearer $($v1.token)" }
$vol2Tasks = Invoke-RestMethod -Uri "$baseUrl/assignments/my-tasks" -Method Get -Headers @{ Authorization = "Bearer $($v2.token)" }

$activeCount = 0
$v1Status = "NONE"
$v2Status = "NONE"

if ($vol1Tasks) {
    $t1 = $vol1Tasks | Where-Object { $_.complaintId -eq $compId }
    if ($t1) { $v1Status = $t1.status; if ($t1.status -eq "PENDING") { $activeCount++ } }
}
if ($vol2Tasks) {
    $t2 = $vol2Tasks | Where-Object { $_.complaintId -eq $compId }
    if ($t2) { $v2Status = $t2.status; if ($t2.status -eq "PENDING") { $activeCount++ } }
}

Write-Host "Vol 1 assignment status: $v1Status"
Write-Host "Vol 2 assignment status: $v2Status"
Write-Host "Total active (PENDING) assignments: $activeCount"

if ($activeCount -eq 1) {
    Write-Host "PASS: Scenario 16 - Concurrent assignment race condition handled cleanly. Exactly 1 active assignment remains."
} else {
    Write-Host "FAIL: Scenario 16 - Concurrency anomaly detected ($activeCount active assignments found)."
}
