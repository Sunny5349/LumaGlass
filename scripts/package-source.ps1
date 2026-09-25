[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$version = (Select-String -LiteralPath "$project/gradle.properties" -Pattern '^mod_version=(.+)$').Matches.Groups[1].Value
$output = Join-Path $project "build/github/LumaGlass-$version"
if (Test-Path -LiteralPath $output) { throw "Output already exists: $output. Use a new version or move the previous export." }
New-Item -ItemType Directory -Force -Path $output | Out-Null
$files = @('README.md','LICENSE-LumaGlass.txt','build.gradle','settings.gradle','gradle.properties',
    'gradlew','gradlew.bat','.gitattributes',
    'docs/API.md','docs/GITHUB-DESCRIPTION.txt','docs/RELEASE.md',
    'scripts/test-production.ps1','scripts/package-source.ps1',
    'examples/api-consumer/build.gradle','examples/api-consumer/settings.gradle',
    'examples/api-consumer/gradle.properties','examples/api-consumer/README.md')
foreach ($directory in @('src','gradle/wrapper','docs/images')) {
    foreach ($file in Get-ChildItem -LiteralPath "$project/$directory" -File -Recurse) {
        $files += [IO.Path]::GetRelativePath($project,$file.FullName)
    }
}
foreach ($relative in $files) {
    $destination = Join-Path $output $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
    Copy-Item -LiteralPath (Join-Path $project $relative) -Destination $destination
}
Copy-Item -LiteralPath "$project/LICENSE-LumaGlass.txt" -Destination "$output/LICENSE"
# Preserve Forge MDK and Gradle wrapper notices with their respective components.
if (Test-Path -LiteralPath "$project/LICENSE.txt") {
    New-Item -ItemType Directory -Force -Path "$output/third-party" | Out-Null
    Copy-Item -LiteralPath "$project/LICENSE.txt" -Destination "$output/third-party/Forge-MDK-LICENSE.txt"
    Copy-Item -LiteralPath "$project/CREDITS.txt" -Destination "$output/third-party/Forge-MDK-CREDITS.txt"
} elseif (Test-Path -LiteralPath "$project/third-party") {
    Copy-Item -LiteralPath "$project/third-party" -Destination "$output/third-party" -Recurse
}
@'
.gradle/
.gradle-user-home/
build/
bin/
out/
run/
run-*/
tools/
.idea/
.settings/
.classpath
.project
*.iml
*.log
*.hprof
hs_err_pid*
replay_pid*
.env
.env.*
'@ | Set-Content -LiteralPath "$output/.gitignore" -Encoding utf8NoBOM
$archive = Join-Path $project "build/github/LumaGlass-$version-source.zip"
Add-Type -AssemblyName System.IO.Compression.FileSystem
[IO.Compression.ZipFile]::CreateFromDirectory($output,$archive)
Write-Host "Source directory: $output"
Write-Host "Source archive: $archive"
