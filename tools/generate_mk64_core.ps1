param(
    [Parameter(Mandatory=$true)]
    [string]$Rom,
    [string]$N64Recomp = "",
    [string]$RSPRecomp = ""
)

$ErrorActionPreference = "Stop"
$ExpectedSha1 = "579c48e211ae952530ffc8738709f078d5dd215e"
$Root = Split-Path -Parent $PSScriptRoot
$Upstream = Join-Path $Root "upstream\MK64Recomp"

if (-not $N64Recomp) { $N64Recomp = Join-Path $Root "tools\N64Recomp.exe" }
if (-not $RSPRecomp) { $RSPRecomp = Join-Path $Root "tools\RSPRecomp.exe" }

if (-not (Test-Path $Rom -PathType Leaf)) {
    throw "ROM not found: $Rom"
}
if (-not (Test-Path $N64Recomp -PathType Leaf)) {
    throw "N64Recomp.exe not found. Pass -N64Recomp or put it in tools\."
}
if (-not (Test-Path $RSPRecomp -PathType Leaf)) {
    throw "RSPRecomp.exe not found. Pass -RSPRecomp or put it in tools\."
}
if (-not (Test-Path (Join-Path $Upstream "us.rev1.toml") -PathType Leaf)) {
    throw "Upstream submodule missing. Run: git submodule update --init --recursive"
}

$ActualSha1 = (Get-FileHash -Algorithm SHA1 $Rom).Hash.ToLowerInvariant()
if ($ActualSha1 -ne $ExpectedSha1) {
    throw "Wrong ROM. Expected SHA-1 $ExpectedSha1 but got $ActualSha1"
}

$TempRom = Join-Path $Upstream "mk64.us.z64"

try {
    Write-Host "ROM verified."
    Copy-Item -LiteralPath $Rom -Destination $TempRom -Force

    $Funcs = Join-Path $Upstream "RecompiledFuncs"
    if (Test-Path $Funcs) { Remove-Item $Funcs -Recurse -Force }
    New-Item -ItemType Directory -Path $Funcs | Out-Null

    Push-Location $Upstream
    try {
        & $N64Recomp "us.rev1.toml"
        if ($LASTEXITCODE -ne 0) { throw "N64Recomp failed with exit code $LASTEXITCODE" }

        & $RSPRecomp "aspMain.us.rev1.toml"
        if ($LASTEXITCODE -ne 0) { throw "RSPRecomp aspMain failed with exit code $LASTEXITCODE" }

        if (Test-Path "njpgdspMain.us.rev1.toml") {
            & $RSPRecomp "njpgdspMain.us.rev1.toml"
            if ($LASTEXITCODE -ne 0) { throw "RSPRecomp njpgdspMain failed with exit code $LASTEXITCODE" }
        }

        if (-not (Get-ChildItem "RecompiledFuncs" -File -ErrorAction SilentlyContinue | Select-Object -First 1)) {
            throw "No RecompiledFuncs output was generated."
        }
        if (-not (Test-Path "rsp\aspMain.cpp")) {
            throw "rsp\aspMain.cpp was not generated."
        }

        Write-Host "MK64 recompiled sources generated successfully."
    }
    finally {
        Pop-Location
    }
}
finally {
    Remove-Item -LiteralPath $TempRom -Force -ErrorAction SilentlyContinue
}
