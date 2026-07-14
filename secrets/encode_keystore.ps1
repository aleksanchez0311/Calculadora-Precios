# Uso: .\encode_keystore.ps1 -Path .\release.keystore -Out keystore.base64
param(
  [Parameter(Mandatory=$true)][string]$Path,
  [string]$Out = "keystore.base64"
)
if (-Not (Test-Path $Path)) {
  Write-Error "No se encontró: $Path"
  exit 2
}
$bytes = [System.IO.File]::ReadAllBytes($Path)
$base64 = [Convert]::ToBase64String($bytes)
Set-Content -Path $Out -Value $base64 -NoNewline
Write-Host "Base64 guardado en $Out"
