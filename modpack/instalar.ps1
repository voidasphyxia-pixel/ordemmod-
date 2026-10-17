# Instala o modpack (configs, shaders, options) numa instancia do Minecraft e copia o mod Ordem + TACZ.
# Uso (PowerShell, na pasta modpack):
#   .\instalar.ps1 -Instancia "C:\caminho\da\instancia\.minecraft"
# Na Modrinth App: clique direito no perfil > "Abrir pasta"; no Prism: Pasta da instancia > .minecraft
param(
    [Parameter(Mandatory = $true)][string]$Instancia
)
$ErrorActionPreference = "Stop"
$aqui = Split-Path -Parent $MyInvocation.MyCommand.Path
$raiz = Split-Path -Parent $aqui
if (-not (Test-Path $Instancia)) { throw "Pasta nao encontrada: $Instancia" }

# 1) configs, shaders e options.txt (o options.txt antigo vira options.txt.bak)
$opt = Join-Path $Instancia "options.txt"
if (Test-Path $opt) { Copy-Item $opt "$opt.bak" -Force }
Copy-Item -Path (Join-Path $aqui "overrides\*") -Destination $Instancia -Recurse -Force
Write-Host "Configs, shaderpacks e options.txt copiados."

# 2) o mod Ordem (gradlew build) e o TACZ-Refabricated (libs/)
$mods = Join-Path $Instancia "mods"
New-Item -ItemType Directory -Force $mods | Out-Null
Get-ChildItem $mods -Filter "ordemmod*.jar" -ErrorAction SilentlyContinue | Remove-Item -Force
$jar = Get-ChildItem (Join-Path $raiz "build\libs") -Filter "*.jar" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch "sources|dev|javadoc" } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($jar) { Copy-Item $jar.FullName $mods -Force; Write-Host "Mod copiado: $($jar.Name)" }
else { Write-Warning "Nenhum jar em build\libs. Rode .\gradlew.bat build na raiz do projeto e execute este script de novo." }
$tacz = Get-ChildItem (Join-Path $raiz "libs") -Filter "TACZ-Refabricated*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($tacz) { Copy-Item $tacz.FullName $mods -Force; Write-Host "TACZ copiado: $($tacz.Name)" }
else { Write-Warning "TACZ-Refabricated*.jar nao esta em libs\. Coloque o jar dele em $mods manualmente." }

Write-Host ""
Write-Host "Falta (nao vai no repositorio): os outros mods na pasta mods\ e os resourcepacks na pasta resourcepacks\. Veja modpack\README.md."
