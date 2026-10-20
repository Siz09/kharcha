Add-Type -AssemblyName System.Drawing

$srcPath = "C:\Users\siz\.gemini\antigravity-cli\brain\9cb22c54-e858-4945-92fe-15e93e0c6168\kharcha2_app_icon_fullbleed_1792522741936.jpg"
$baseRes = "D:\vscode\kharcha\app\src\main\res"

if (-not (Test-Path $srcPath)) {
    Write-Error "Source image not found: $srcPath"
    exit 1
}

$srcBmp = [System.Drawing.Bitmap]::FromFile($srcPath)

$densities = @{
    "mipmap-mdpi"    = @{ Icon = 48; Foreground = 108 }
    "mipmap-hdpi"    = @{ Icon = 72; Foreground = 162 }
    "mipmap-xhdpi"   = @{ Icon = 96; Foreground = 216 }
    "mipmap-xxhdpi"  = @{ Icon = 144; Foreground = 324 }
    "mipmap-xxxhdpi" = @{ Icon = 192; Foreground = 432 }
}

function Resize-Image($src, $w, $h) {
    $dest = New-Object System.Drawing.Bitmap($w, $h)
    $g = [System.Drawing.Graphics]::FromImage($dest)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.DrawImage($src, 0, 0, $w, $h)
    $g.Dispose()
    return $dest
}

function Make-Circle-Image($src, $size) {
    $dest = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($dest)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddEllipse(0, 0, $size, $size)
    $g.SetClip($path)
    $g.DrawImage($src, 0, 0, $size, $size)
    $path.Dispose()
    $g.Dispose()
    return $dest
}

foreach ($d in $densities.Keys) {
    $dir = Join-Path $baseRes $d
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }

    $iconSize = $densities[$d].Icon
    $fgSize = $densities[$d].Foreground

    # 1. Standard square launcher icon
    $iconBmp = Resize-Image $srcBmp $iconSize $iconSize
    $iconPath = Join-Path $dir "ic_launcher.png"
    $iconBmp.Save($iconPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $iconBmp.Dispose()

    # 2. Round launcher icon
    $roundBmp = Make-Circle-Image $srcBmp $iconSize
    $roundPath = Join-Path $dir "ic_launcher_round.png"
    $roundBmp.Save($roundPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $roundBmp.Dispose()

    # 3. Adaptive foreground icon
    $fgBmp = Resize-Image $srcBmp $fgSize $fgSize
    $fgPath = Join-Path $dir "ic_launcher_foreground.png"
    $fgBmp.Save($fgPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $fgBmp.Dispose()

    Write-Host "Generated icons for $d (Icon: ${iconSize}x${iconSize}, FG: ${fgSize}x${fgSize})"
}

$srcBmp.Dispose()
Write-Host "All icons generated successfully!"
