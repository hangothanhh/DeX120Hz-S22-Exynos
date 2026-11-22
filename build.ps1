$ErrorActionPreference = "Stop"

$Root = $PSScriptRoot

function Resolve-JavaBin {
    $candidates = @()
    if ($env:JAVA_HOME) {
        $candidates += (Join-Path $env:JAVA_HOME "bin")
    }
    $candidates += "C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot\bin"
    foreach ($dir in $candidates) {
        if ($dir -and (Test-Path (Join-Path $dir "javac.exe"))) {
            return $dir
        }
    }
    $cmd = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($cmd) {
        return Split-Path -Parent $cmd.Source
    }
    throw "JDK not found. Set JAVA_HOME or install JDK 17."
}

function Resolve-AndroidSdk {
    $candidates = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, "C:\Android\Sdk") | Where-Object { $_ }
    foreach ($sdk in $candidates) {
        if (Test-Path (Join-Path $sdk "platforms")) {
            return $sdk
        }
    }
    throw "Android SDK not found. Set ANDROID_HOME or ANDROID_SDK_ROOT."
}

$JavaBin = Resolve-JavaBin
$Javac = Join-Path $JavaBin "javac.exe"
$Jar = Join-Path $JavaBin "jar.exe"
$Keytool = Join-Path $JavaBin "keytool.exe"

$AndroidSdk = Resolve-AndroidSdk
$PlatformDir = Join-Path $AndroidSdk "platforms\android-34"
if (-not (Test-Path (Join-Path $PlatformDir "android.jar"))) {
    $PlatformDir = Get-ChildItem (Join-Path $AndroidSdk "platforms") -Directory |
        Where-Object { $_.Name -match '^android-(\d+)$' -and [int]$Matches[1] -ge 31 -and (Test-Path (Join-Path $_.FullName "android.jar")) } |
        Sort-Object { [int]($_.Name -replace 'android-', '') } -Descending |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $PlatformDir) { throw "No Android SDK platform >= 31 found under $AndroidSdk\platforms" }
$AndroidJar = Join-Path $PlatformDir "android.jar"

$BuildTools = Join-Path $AndroidSdk "build-tools\34.0.0"
if (-not (Test-Path (Join-Path $BuildTools "aapt2.exe"))) {
    $BuildTools = Get-ChildItem (Join-Path $AndroidSdk "build-tools") -Directory |
        Where-Object { Test-Path (Join-Path $_.FullName "aapt2.exe") } |
        Sort-Object Name -Descending |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $BuildTools) { throw "No Android build-tools found under $AndroidSdk\build-tools" }

$Aapt2 = Join-Path $BuildTools "aapt2.exe"
$D8 = Join-Path $BuildTools "d8.bat"
$ZipAlign = Join-Path $BuildTools "zipalign.exe"
$ApkSigner = Join-Path $BuildTools "apksigner.bat"

$BuildDir = Join-Path $Root "build"
$OutDir = Join-Path $Root "out"

if (Test-Path $BuildDir) { Remove-Item -Recurse -Force $BuildDir }
New-Item -ItemType Directory -Force -Path `
    (Join-Path $BuildDir "stub-classes"), `
    (Join-Path $BuildDir "gen"), `
    (Join-Path $BuildDir "classes"), `
    (Join-Path $BuildDir "dex"), `
    $OutDir | Out-Null

function Invoke-Checked {
    param([string]$Exe, [string[]]$ToolArgs)
    & $Exe @ToolArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed ($LASTEXITCODE): $Exe $($ToolArgs -join ' ')"
    }
}

Write-Host "[1/7] Compiling Xposed compile-only stubs..."
$StubSources = Get-ChildItem -Path (Join-Path $Root "stubs") -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
Invoke-Checked $Javac (@("--release", "11", "-encoding", "UTF-8", "-cp", $AndroidJar, "-d", (Join-Path $BuildDir "stub-classes")) + $StubSources)
$StubsJar = Join-Path $BuildDir "xposed-stubs.jar"
Invoke-Checked $Jar @("cf", $StubsJar, "-C", (Join-Path $BuildDir "stub-classes"), ".")

Write-Host "[2/7] Compiling resources with aapt2..."
$ResZip = Join-Path $BuildDir "res.zip"
Invoke-Checked $Aapt2 @("compile", "--dir", (Join-Path $Root "app\src\main\res"), "-o", $ResZip)

Write-Host "[3/7] Linking APK & generating R.java..."
$UnalignedApk = Join-Path $BuildDir "unaligned.apk"
Invoke-Checked $Aapt2 @(
    "link",
    "-o", $UnalignedApk,
    "-I", $AndroidJar,
    "--manifest", (Join-Path $Root "app\src\main\AndroidManifest.xml"),
    "-A", (Join-Path $Root "app\src\main\assets"),
    "--min-sdk-version", "31",
    "--target-sdk-version", "31",
    "--java", (Join-Path $BuildDir "gen"),
    $ResZip
)

Write-Host "[4/7] Compiling app + Xposed module classes..."
$AppSources = @(
    Get-ChildItem -Path (Join-Path $Root "app\src\main\java") -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
    Get-ChildItem -Path (Join-Path $BuildDir "gen") -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
)
Invoke-Checked $Javac (@("--release", "11", "-encoding", "UTF-8", "-cp", "$AndroidJar;$StubsJar", "-d", (Join-Path $BuildDir "classes")) + $AppSources)

$AppJar = Join-Path $BuildDir "app-classes.jar"
Invoke-Checked $Jar @("cf", $AppJar, "-C", (Join-Path $BuildDir "classes"), ".")

Write-Host "[5/7] Converting classes to classes.dex with d8 (excluding Xposed stubs)..."
Invoke-Checked $D8 @("--min-api", "31", "--lib", $AndroidJar, "--output", (Join-Path $BuildDir "dex"), $AppJar)

Write-Host "[6/7] Packaging classes.dex & aligning APK..."
$ClassesDex = Join-Path $BuildDir "dex\classes.dex"
python -c "import zipfile; z=zipfile.ZipFile(r'$UnalignedApk', 'a', zipfile.ZIP_DEFLATED); z.write(r'$ClassesDex', 'classes.dex'); z.close()"
if ($LASTEXITCODE -ne 0) { throw "Failed to inject classes.dex into APK" }

$AlignedApk = Join-Path $BuildDir "aligned.apk"
Invoke-Checked $ZipAlign @("-f", "-p", "4", $UnalignedApk, $AlignedApk)

Write-Host "[7/7] Signing APK..."
$Keystore = if ($env:DEX120_KEYSTORE) { $env:DEX120_KEYSTORE } else { Join-Path $Root "debug.keystore" }
$KsPass = if ($env:DEX120_KEYSTORE_PASS) { $env:DEX120_KEYSTORE_PASS } else { "android" }
$KsAlias = if ($env:DEX120_KEY_ALIAS) { $env:DEX120_KEY_ALIAS } else { "dex120" }
$KeyPass = if ($env:DEX120_KEY_PASS) { $env:DEX120_KEY_PASS } else { "android" }

if (-not (Test-Path $Keystore)) {
    Invoke-Checked $Keytool @(
        "-genkeypair", "-v",
        "-keystore", $Keystore,
        "-storepass", $KsPass,
        "-alias", $KsAlias,
        "-keypass", $KeyPass,
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-dname", "CN=DeX120Hz, OU=S22, O=DeX120Hz-S22, C=VN"
    )
}

$FinalApk = Join-Path $OutDir "DeX120Hz-S22.apk"
Invoke-Checked $ApkSigner @(
    "sign",
    "--ks", $Keystore,
    "--ks-pass", "pass:$KsPass",
    "--key-pass", "pass:$KeyPass",
    "--ks-key-alias", $KsAlias,
    "--out", $FinalApk,
    $AlignedApk
)

$Hash = (Get-FileHash -Algorithm SHA256 -Path $FinalApk).Hash.ToLowerInvariant()
$ShaFile = "$FinalApk.sha256"
[System.IO.File]::WriteAllText($ShaFile, "$Hash  DeX120Hz-S22.apk`n", [System.Text.UTF8Encoding]::new($false))

Write-Host "Build succeeded: $FinalApk"
Write-Host "SHA-256 ($ShaFile): $Hash"
