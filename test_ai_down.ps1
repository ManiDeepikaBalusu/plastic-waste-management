$baseUrl = "http://localhost:8080/api/v1"

# Verify AI service is offline (port 8001 connection should fail)
try {
    Invoke-RestMethod -Uri "http://localhost:8001/health" -TimeoutSec 2
    Write-Host "WARNING: AI service is still responding."
} catch {
    Write-Host "CONFIRMED: AI service is OFFLINE / Unavailable."
}

# 1. Admin Login
$adminLogin = @{ email = "admin@plasticwaste.org"; password = "AdminPassword2026!" } | ConvertTo-Json
$adminRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $adminLogin -ContentType "application/json"
$adminToken = $adminRes.token

# 2. Register Citizen & Submit Complaint
$citEmail = "cit.aidown." + [guid]::NewGuid().ToString().Substring(0,6) + "@test.org"
$cit = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -Body (@{ fullName="Fault Tolerance Citizen"; email=$citEmail; password="Password123!"; role="CITIZEN" } | ConvertTo-Json) -ContentType "application/json"

$tempDir = [System.IO.Path]::GetTempPath()
$validJpgPath = Join-Path $tempDir "valid_sample.jpg"
$compPath = Join-Path $tempDir "comp_aidown.json"
@{ title="AI Offline Report"; description="Waste report filed while AI service is unreachable"; wasteType="PLASTIC_MIXED"; latitude=12.92; longitude=77.62 } | ConvertTo-Json | Set-Content -Path $compPath

$raw = & curl.exe -s -H "Authorization: Bearer $($cit.token)" -F "data=@$compPath;type=application/json" -F "files=@$validJpgPath;type=image/jpeg" "$baseUrl/complaints/submit-with-photos"
$comp = $raw | ConvertFrom-Json
$compId = $comp.id
Write-Host "Complaint submitted successfully: $compId"

# 3. Transition through admin review & verify
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
$rev = Invoke-RestMethod -Uri "$baseUrl/complaints/$compId/review" -Method Patch -Headers $adminHeaders
$ver = Invoke-RestMethod -Uri "$baseUrl/complaints/$compId/verify" -Method Patch -Headers $adminHeaders

Write-Host "Complaint status post-verification: $($ver.status)"

if ($ver.status -eq "VERIFIED") {
    Write-Host "PASS: Scenario 19 - System handles AI service unavailability gracefully without operational disruption."
} else {
    Write-Host "FAIL: Scenario 19 - Complaint state machine failed when AI service was unavailable."
}
