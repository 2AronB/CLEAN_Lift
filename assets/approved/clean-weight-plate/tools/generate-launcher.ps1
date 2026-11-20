Add-Type -AssemblyName System.Drawing
$culture = [Globalization.CultureInfo]::InvariantCulture
function N($v) { ([double]$v).ToString('0.###', $culture) }
function AndroidPath($path) {
    $p = $path.PathPoints; $t = $path.PathTypes
    $s = [Text.StringBuilder]::new()
    for ($i=0; $i -lt $p.Length; $i++) {
        $kind = $t[$i] -band 7
        if ($kind -eq 0) { [void]$s.Append("M$(N $p[$i].X),$(N $p[$i].Y) ") }
        elseif ($kind -eq 1) { [void]$s.Append("L$(N $p[$i].X),$(N $p[$i].Y) ") }
        elseif ($kind -eq 3) {
            [void]$s.Append("C$(N $p[$i].X),$(N $p[$i].Y) $(N $p[$i+1].X),$(N $p[$i+1].Y) $(N $p[$i+2].X),$(N $p[$i+2].Y) ")
            $i += 2
        }
        if (($t[$i] -band 128) -ne 0) { [void]$s.Append('Z ') }
    }
    $s.ToString().Trim()
}
$plate = [Drawing.Drawing2D.GraphicsPath]::new()
$plate.AddArc(23,23,62,62, 6, 348)
$plate.AddLine(84.83,50.76,63.46,50.76)
$plate.AddArc(44,44,20,20,-19,-322)
$plate.CloseFigure()
$rim = [Drawing.Drawing2D.GraphicsPath]::new()
$rim.AddArc(26.7,26.7,54.6,54.6, -12, 336)
$letters = [Drawing.Drawing2D.GraphicsPath]::new()
$family = [Drawing.FontFamily]::new('Arial')
$letters.AddString('45 LB', $family, [int][Drawing.FontStyle]::Bold, 10.5, [Drawing.PointF]::new(0,0), [Drawing.StringFormat]::GenericTypographic)
$bounds = $letters.GetBounds()
$matrix = [Drawing.Drawing2D.Matrix]::new()
$matrix.Translate([single](54-$bounds.Width/2-$bounds.X), [single](33.5-$bounds.Y))
$letters.Transform($matrix)
$res = Join-Path (Get-Location) 'app\src\main\res'
$plateData = AndroidPath $plate; $textData = AndroidPath $letters; $rimData = AndroidPath $rim
$header = '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">'
$foreground = @"
$header
    <path android:fillColor="#F8F2E4" android:pathData="$plateData" />
    <path android:fillColor="@android:color/transparent" android:strokeColor="#99968F" android:strokeWidth="1" android:strokeLineCap="round" android:pathData="$rimData" />
    <path android:fillColor="#232323" android:pathData="$textData" />
</vector>
"@
$foreground | Set-Content -LiteralPath "$res\drawable\ic_launcher_foreground.xml" -Encoding utf8
$combined = [Drawing.Drawing2D.GraphicsPath]::new([Drawing.Drawing2D.FillMode]::Alternate)
$combined.AddPath($plate,$false); $combined.AddPath($letters,$false)
$monoData = AndroidPath $combined
@"
$header
    <path android:fillColor="#FFFFFFFF" android:fillType="evenOdd" android:pathData="$monoData" />
</vector>
"@ | Set-Content -LiteralPath "$res\drawable\ic_launcher_monochrome.xml" -Encoding utf8
$ivory = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#F8F2E4'))
$charcoal = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#232323'))
$rimPen = [Drawing.Pen]::new([Drawing.ColorTranslator]::FromHtml('#99968F'),1)
foreach ($entry in @(@('mdpi',48),@('hdpi',72),@('xhdpi',96),@('xxhdpi',144),@('xxxhdpi',192),@('preview',512))) {
    $size = [int]$entry[1]
    $bitmap = [Drawing.Bitmap]::new($size,$size)
    $g = [Drawing.Graphics]::FromImage($bitmap)
    $g.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear($charcoal.Color)
    $g.ScaleTransform([single]($size/108.0),[single]($size/108.0))
    # Legacy icons do not have the adaptive mask; use more of the canvas.
    if ($entry[0] -ne 'preview') { $g.TranslateTransform(54,54); $g.ScaleTransform(1.25,1.25); $g.TranslateTransform(-54,-54) }
    $g.FillPath($ivory,$plate); $g.DrawPath($rimPen,$rim); $g.FillPath($charcoal,$letters)
    if ($entry[0] -eq 'preview') { $out = Join-Path (Get-Location) 'assets\approved\clean-weight-plate\launcher-preview.png' }
    else {
        $dir = "$res\mipmap-$($entry[0])"
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        $out = "$dir\ic_launcher.png"
    }
    $bitmap.Save($out,[Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bitmap.Dispose()
}
$plate.Dispose(); $rim.Dispose(); $letters.Dispose(); $combined.Dispose()
$family.Dispose(); $matrix.Dispose(); $ivory.Dispose(); $charcoal.Dispose(); $rimPen.Dispose()

