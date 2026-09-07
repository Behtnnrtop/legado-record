Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$sourcePath = Join-Path $root 'assets/icon/image.png'
$outRoot = Join-Path $root 'app/src/main'
$safeInsetRatio = 0.12

if (-not (Test-Path $sourcePath)) {
    throw "Missing source icon: $sourcePath"
}

$source = [System.Drawing.Image]::FromFile($sourcePath)
$cropSize = [Math]::Min($source.Width, $source.Height)
$srcX = [Math]::Floor(($source.Width - $cropSize) / 2)
$srcY = [Math]::Floor(($source.Height - $cropSize) / 2)

function Save-LauncherIcon {
    param(
        [string]$RelativePath,
        [int]$Size
    )

    $targetPath = Join-Path $outRoot $RelativePath
    $targetDir = Split-Path -Parent $targetPath
    if (-not (Test-Path $targetDir)) {
        New-Item -ItemType Directory -Path $targetDir | Out-Null
    }

    $bitmap = New-Object System.Drawing.Bitmap $Size, $Size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

    $inset = [Math]::Max(1, [Math]::Round($Size * $safeInsetRatio))
    $drawSize = $Size - ($inset * 2)
    $dest = New-Object System.Drawing.Rectangle $inset, $inset, $drawSize, $drawSize
    $src = New-Object System.Drawing.Rectangle $srcX, $srcY, $cropSize, $cropSize
    $graphics.DrawImage($source, $dest, $src, [System.Drawing.GraphicsUnit]::Pixel)

    $graphics.Dispose()
    $bitmap.Save($targetPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

$icons = @{
    'res/mipmap-mdpi/ic_launcher.png' = 48
    'res/mipmap-hdpi/ic_launcher.png' = 72
    'res/mipmap-xhdpi/ic_launcher.png' = 96
    'res/mipmap-xxhdpi/ic_launcher.png' = 144
    'res/mipmap-xxxhdpi/ic_launcher.png' = 192
    'res/drawable-nodpi/ic_launcher_foreground.png' = 432
    'ic_launcher-web.png' = 512
}

foreach ($icon in $icons.GetEnumerator()) {
    Save-LauncherIcon $icon.Key $icon.Value
}

$source.Dispose()
