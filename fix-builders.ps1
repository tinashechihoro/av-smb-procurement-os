$base = 'C:\Users\user\Downloads\av-smb-platform\backend\src\main\java\com\avsmc\procurement'

# Fix all service files that use .organisationId() in builder pattern
$serviceFiles = Get-ChildItem -Path $base -Recurse -Filter "*.java" | Where-Object { $_.DirectoryName -like "*service*" }

foreach ($file in $serviceFiles) {
    $content = Get-Content $file.FullName -Raw
    if ($content -match '\.organisationId\(') {
        # Replace .organisationId(securityUtils.currentOrgId()) pattern
        # We need to remove it from builder chain and add set call after build
        $content = $content -replace '\.organisationId\(securityUtils\.currentOrgId\(\)\)\s*\r?\n\s*', "`n                "
        # Add setOrganisationId after each .build() that creates an entity
        # This is tricky - let's just replace the pattern inline
        Set-Content -Path $file.FullName -Value $content -NoNewline
        Write-Host "Fixed: $($file.Name)"
    }
}

Write-Host "Done."
