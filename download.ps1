$apiUrl = 'https://api.github.com/repos/Sanotsu/china-food-composition-data/contents/json_data_vision_251206_Qwen2-5-VL-72B-Instruct'
$files = Invoke-RestMethod -Uri $apiUrl -Headers @{'User-Agent'='Mozilla/5.0'}
Write-Host ('Found ' + $files.Count + ' files')
$allItems = @()
foreach ($f in $files) {
    if ($f.name -notlike '*.json') { continue }
    Write-Host ('Downloading ' + $f.name + '...') -NoNewline
    try {
        $data = Invoke-RestMethod -Uri $f.download_url -Headers @{'User-Agent'='Mozilla/5.0'}
        Write-Host (' ' + $data.Count + ' items')
        $allItems += $data
    } catch {
        Write-Host ' FAILED'
    }
}
$assetsDir = 'app\src\main\assets'
New-Item -ItemType Directory -Force -Path $assetsDir | Out-Null
$mergedPath = Join-Path $assetsDir 'nutrition_database.json'
$allItems | ConvertTo-Json -Depth 10 -Compress | Out-File -FilePath $mergedPath -Encoding UTF8
$size = (Get-Item $mergedPath).Length
Write-Host ('Merged ' + $allItems.Count + ' items -> ' + $mergedPath + ' (' + $size + ' bytes)')
