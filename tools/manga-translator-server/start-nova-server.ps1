[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$MangaTranslatorRoot,

    [Parameter(Mandatory = $true)]
    [string]$ConfigPath,

    [Parameter(Mandatory = $true)]
    [string]$Token,

    [string]$DataDirectory = (Join-Path $PSScriptRoot "data"),
    [string]$Python = (Join-Path $PSScriptRoot ".venv\Scripts\python.exe"),
    [ValidateRange(1, 65535)]
    [int]$Port = 8765,
    [switch]$ForceCpu
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $Python -PathType Leaf)) {
    throw "Python environment not found: $Python"
}
if (-not (Test-Path -LiteralPath $MangaTranslatorRoot -PathType Container)) {
    throw "MangaTranslator directory not found: $MangaTranslatorRoot"
}
if (-not (Test-Path -LiteralPath $ConfigPath -PathType Leaf)) {
    throw "MangaTranslator config not found: $ConfigPath"
}
if ([string]::IsNullOrWhiteSpace($Token)) {
    throw "Token must not be empty"
}

$resolvedRoot = (Resolve-Path -LiteralPath $MangaTranslatorRoot).Path
$resolvedConfig = (Resolve-Path -LiteralPath $ConfigPath).Path
$resolvedPython = (Resolve-Path -LiteralPath $Python).Path
$resolvedData = [System.IO.Path]::GetFullPath($DataDirectory)
[System.IO.Directory]::CreateDirectory($resolvedData) | Out-Null

$env:NOVA_MANGA_TRANSLATOR_ROOT = $resolvedRoot
$env:NOVA_MANGA_TRANSLATOR_CONFIG = $resolvedConfig
$env:NOVA_MANGA_TRANSLATOR_DATA = $resolvedData
$env:NOVA_MANGA_TRANSLATOR_TOKEN = $Token
$env:NOVA_MANGA_TRANSLATOR_PORT = $Port.ToString([System.Globalization.CultureInfo]::InvariantCulture)
$env:NOVA_MANGA_TRANSLATOR_FORCE_CPU = if ($ForceCpu) { "1" } else { "0" }

Push-Location $PSScriptRoot
try {
    & $resolvedPython -m nova_manga_translator.server
    if ($LASTEXITCODE -ne 0) {
        throw "Nova MangaTranslator server exited with code $LASTEXITCODE"
    }
} finally {
    Pop-Location
    Remove-Item Env:NOVA_MANGA_TRANSLATOR_TOKEN -ErrorAction SilentlyContinue
}
