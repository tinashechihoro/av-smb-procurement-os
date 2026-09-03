$base = 'C:\Users\user\Downloads\av-smb-platform\backend\src\main\java\com\avsmc\procurement'

$files = @(
    @{ Path='finance\dto\FinanceDtos.java'; Pkg='com.avsmc.procurement.finance.dto' },
    @{ Path='procurement\dto\ProcurementDtos.java'; Pkg='com.avsmc.procurement.procurement.dto' },
    @{ Path='purchasing\dto\PurchasingDtos.java'; Pkg='com.avsmc.procurement.purchasing.dto' },
    @{ Path='inventory\dto\InventoryDtos.java'; Pkg='com.avsmc.procurement.inventory.dto' }
)

foreach ($fileInfo in $files) {
    $filePath = Join-Path $base $fileInfo.Path
    $dir = Split-Path $filePath -Parent
    $content = Get-Content $filePath -Raw
    $pkg = $fileInfo.Pkg

    # Extract imports block
    $imports = ''
    $importMatches = [regex]::Matches($content, '(?m)^import .+;$')
    foreach ($im in $importMatches) {
        $imports += $im.Value + "`n"
    }

    # Split by class declarations
    $classPattern = '(?ms)(@Data(?:\s+@Builder)?|@Data)\s*\r?\nclass\s+(\w+)\s*\{(.*?)\n\}'
    $matches = [regex]::Matches($content, $classPattern)

    foreach ($m in $matches) {
        $annotations = $m.Groups[1].Value
        $className = $m.Groups[2].Value
        $body = $m.Groups[3].Value

        $fileContent = "package $pkg;`n`n${imports}`n$annotations`npublic class $className {$body`n}`n"
        $outPath = Join-Path $dir "$className.java"
        Set-Content -Path $outPath -Value $fileContent -NoNewline
        Write-Host "Created: $className.java"
    }
}

Write-Host "`nDone splitting DTOs."
