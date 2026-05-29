# Controllo variabile di ambiente NDK_FOLDER
if (-not $Env:NDK_FOLDER) {
    Write-Host "Errore: variabile di sistema NDK_FOLDER non trovata!" -ForegroundColor Red
    exit 1
}

# Richiesta input per API level
$apiLevel = Read-Host "Inserisci il numero di API level per clang (es: 28, 30, 33)"

Write-Host "`n(PC) Compilation...`n" -ForegroundColor Blue

# Costruzione comando
$clangPath = "$Env:NDK_FOLDER\bin\aarch64-linux-android$apiLevel-clang"
$sysroot = "$Env:NDK_FOLDER\sysroot"
$outputFile = "sensor"

& $clangPath --sysroot=$sysroot sensor.c -o $outputFile -landroid -llog

Start-Sleep -Seconds 2

adb push sensor /data/local/tmp/ #Push file
adb shell chmod 755 /data/local/tmp/sensor  #Grant permissions
adb shell /data/local/tmp/sensor #Run it


Start-Sleep -Seconds 5
Write-Host "`n`n (PC) Cleaning..." -ForegroundColor Red
adb shell rm /data/local/tmp/sensor #Delete android Elf
Remove-Item .\sensor #delete local Elf