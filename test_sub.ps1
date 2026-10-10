$baseUrl = "http://localhost:8080/api/v1"
$citEmail = "cit." + [guid]::NewGuid().ToString().Substring(0,6) + "@test.org"
$cit = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -Body (@{ fullName="Citizen Concurrent"; email=$citEmail; password="Password123!"; role="CITIZEN" } | ConvertTo-Json) -ContentType "application/json"

$tempDir = [System.IO.Path]::GetTempPath()
$validJpgPath = Join-Path $tempDir "valid_sample.jpg"
$compPath = Join-Path $tempDir "comp_c.json"
@{ title="Concurrent Spot"; description="Testing race condition"; wasteType="PLASTIC_MIXED"; latitude=12.91; longitude=77.61 } | ConvertTo-Json | Set-Content -Path $compPath

$res = & curl.exe -s -H "Authorization: Bearer $($cit.token)" -F "data=@$compPath;type=application/json" -F "files=@$validJpgPath;type=image/jpeg" "$baseUrl/complaints/submit-with-photos"
Write-Host "CURL RAW RESULT:"
Write-Host $res
$parsed = $res | ConvertFrom-Json
Write-Host "Parsed ID: $($parsed.id)"
