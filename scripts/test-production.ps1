[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$MinecraftRoot,
    [string]$InstalledVersion = '1.20.1-Forge_47.4.22',
    [string]$JavaExecutable = 'java',
    [switch]$RendererMods,
    [switch]$Jei
)
$ErrorActionPreference = 'Stop'
$JavaExecutable = (Get-Command $JavaExecutable -ErrorAction Stop).Source
$project = Split-Path -Parent $PSScriptRoot
$version = (Select-String -LiteralPath "$project/gradle.properties" -Pattern '^mod_version=(.+)$').Matches.Groups[1].Value
$installed = Join-Path "$MinecraftRoot/versions" $InstalledVersion
# Read library coordinates only. Never execute launcher scripts or reuse account tokens/JVM arguments.
$manifest = Get-Content -LiteralPath "$installed/$InstalledVersion.json" -Raw | ConvertFrom-Json
$libraries = [IO.Path]::GetFullPath("$MinecraftRoot/libraries")
$game = Join-Path $project ("run-production-" + $(if ($RendererMods) { 'renderers-' } else { 'vanilla-' }) + $(if ($Jei) { 'jei-' }) + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Force -Path "$game/mods", "$game/natives" | Out-Null
Copy-Item -LiteralPath "$project/build/libs/lumaglass-$version.jar", "$project/build/test-mods/lumaglass-smoke-$version.jar" -Destination "$game/mods"
Copy-Item -Path "$installed/$InstalledVersion-natives/*" -Destination "$game/natives" -Recurse
if ($Jei) {
    $jeiMods = @(Get-ChildItem -LiteralPath "$installed/mods" -Filter '*jei*.jar')
    if ($jeiMods.Count -ne 1) { throw 'Expected exactly one installed JEI JAR' }
    Copy-Item -LiteralPath $jeiMods[0].FullName -Destination "$game/mods"
}
if ($RendererMods) {
    foreach ($pattern in @('embeddium-*.jar', 'oculus-*.jar')) {
        $mods = @(Get-ChildItem -LiteralPath "$installed/mods" -Filter $pattern)
        if ($mods.Count -ne 1) { throw "Expected exactly one installed $pattern" }
        Copy-Item -LiteralPath $mods[0].FullName -Destination "$game/mods"
    }
}
function Allowed($rules) {
    if (!$rules) { return $true }
    $allow = $false
    foreach ($rule in $rules) {
        if ($rule.features) { continue }
        if ($rule.os.name -and $rule.os.name -ne 'windows') { continue }
        if ($rule.os.arch -and $rule.os.arch -notin @('amd64', 'x86_64')) { continue }
        $allow = $rule.action -eq 'allow'
    }
    return $allow
}
$classpath = foreach ($lib in $manifest.libraries) {
    if (!(Allowed $lib.rules)) { continue }
    $relative = $lib.downloads.artifact.path
    if (!$relative) { throw "No artifact path for $($lib.name)" }
    $path = [IO.Path]::GetFullPath((Join-Path $libraries $relative))
    if (!$path.StartsWith($libraries + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Library escapes installation' }
    if (!(Test-Path -LiteralPath $path)) { throw "Missing library $path" }
    $path
}
$classpath += "$installed/$InstalledVersion.jar"
$moduleJars = @($classpath | Where-Object { [IO.Path]::GetFileName($_) -match '^(bootstraplauncher|securejarhandler|asm|asm-commons|asm-util|asm-analysis|asm-tree|JarJarFileSystems)-[0-9]' })
$launch = @(
    '-Xmx3G', '-Dforge.disableVersionCheck=true', '-Dlumaglass.smoke=true',
    "-Djava.library.path=$game/natives", "-Djna.tmpdir=$game/natives",
    "-Dorg.lwjgl.system.SharedLibraryExtractPath=$game/natives", "-Dio.netty.native.workdir=$game/natives",
    "-DlibraryDirectory=$libraries",
    "-DignoreList=bootstraplauncher,securejarhandler,asm-commons,asm-util,asm-analysis,asm-tree,asm,JarJarFileSystems,client-extra,fmlcore,javafmllanguage,lowcodelanguage,mclanguage,forge-,$InstalledVersion.jar",
    '-DmergeModules=jna-5.10.0.jar,jna-platform-5.10.0.jar',
    '-p', ($moduleJars -join ';'), '--add-modules', 'ALL-MODULE-PATH',
    '--add-opens', 'java.base/java.util.jar=cpw.mods.securejarhandler',
    '--add-opens', 'java.base/java.lang.invoke=cpw.mods.securejarhandler',
    '--add-exports', 'java.base/sun.security.util=cpw.mods.securejarhandler',
    '--add-exports', 'jdk.naming.dns/com.sun.jndi.dns=java.naming',
    '-cp', ($classpath -join ';'), 'cpw.mods.bootstraplauncher.BootstrapLauncher',
    '--launchTarget', 'forgeclient', '--fml.forgeVersion', '47.4.22', '--fml.mcVersion', '1.20.1',
    '--fml.forgeGroup', 'net.minecraftforge', '--fml.mcpVersion', '20230612.114412',
    '--username', 'GlassTest', '--uuid', '00000000000000000000000000000001', '--accessToken', '0',
    '--userType', 'legacy', '--version', $InstalledVersion, '--gameDir', $game,
    '--assetsDir', "$MinecraftRoot/assets", '--assetIndex', '5', '--width', '1280', '--height', '720'
)
$argsFile = "$game/launch.args"
$launch | ForEach-Object { '"' + $_.Replace('\', '/').Replace('"', '\"') + '"' } | Set-Content -LiteralPath $argsFile -Encoding utf8NoBOM
Write-Host "Production smoke directory: $game"
Push-Location $game
try {
    & $JavaExecutable "@$argsFile" *> "$game/console.log"
    if ($LASTEXITCODE -ne 0) { throw "Production client exited $LASTEXITCODE; see $game/console.log" }
    if (!(Select-String -LiteralPath "$game/logs/latest.log" -Pattern 'LUMAGLASS_SMOKE_PASS')) { throw 'Smoke suite did not finish' }
    Write-Host "PRODUCTION_SMOKE_PASS: $game"
} finally { Pop-Location }
