@echo off
REM ============================================================
REM  Fabrique une version partageable de Journal Phasmo.
REM  A LANCER DEPUIS LE DOSSIER EXTRAIT, pas depuis l'interieur du .zip.
REM  Resultat : le dossier "dist" avec PhasmoCompanion.exe (tout est dedans).
REM ============================================================

setlocal enabledelayedexpansion
cd /d "%~dp0"

REM --- 1. retrouver le projet ---------------------------------
set "PROJ=%~dp0PhasmoCompanion.csproj"
if not exist "%PROJ%" (
  for /r "%~dp0" %%f in (PhasmoCompanion.csproj) do set "PROJ=%%f"
)

if not exist "!PROJ!" (
  echo.
  echo ============================================================
  echo  PhasmoCompanion.csproj est introuvable a cote de ce script.
  echo ============================================================
  echo.
  echo  Dossier ou ce script s'execute :
  echo    %~dp0
  echo.
  echo  Cause la plus frequente : le script a ete lance DEPUIS LE ZIP.
  echo  Windows recopie alors le .cmd tout seul dans un dossier
  echo  temporaire, sans les fichiers du projet.
  echo.
  echo  A faire :
  echo    1. Clic droit sur JournalPhasmo-projet.zip
  echo    2. "Extraire tout..."  puis ouvrir le dossier obtenu
  echo    3. Double-cliquer publier.cmd DANS ce dossier
  echo       ^(il doit etre a cote de PhasmoCompanion.csproj^)
  echo.
  pause
  exit /b 1
)

REM --- 2. verifier le SDK -------------------------------------
where dotnet >nul 2>nul
if errorlevel 1 (
  echo.
  echo Le SDK .NET 8 n'est pas installe.
  echo Telechargez-le sur https://dotnet.microsoft.com/download puis relancez ce script.
  echo.
  pause
  exit /b 1
)

dotnet --list-sdks | findstr /b "8." >nul
if errorlevel 1 (
  echo.
  echo Attention : aucun SDK .NET 8 detecte. SDK presents :
  dotnet --list-sdks
  echo.
  echo La compilation va quand meme etre tentee.
  echo.
)

REM --- 3. compiler --------------------------------------------
echo.
echo === Compilation en cours, patientez ===
echo Projet : !PROJ!
echo.

dotnet publish "!PROJ!" -c Release -r win-x64 --self-contained true ^
  -p:PublishSingleFile=true ^
  -p:IncludeNativeLibrariesForSelfExtract=true ^
  -p:EnableCompressionInSingleFile=true ^
  -o "%~dp0dist"

if errorlevel 1 (
  echo.
  echo La compilation a echoue. Copiez le message d'erreur ci-dessus.
  pause
  exit /b 1
)

REM --- 4. archive prete a envoyer -----------------------------
powershell -NoProfile -Command "Compress-Archive -Path '%~dp0dist\PhasmoCompanion.exe' -DestinationPath '%~dp0JournalPhasmo.zip' -Force" 2>nul

echo.
echo === Termine ===
echo Dossier pret : %~dp0dist
echo Fichier a partager : dist\PhasmoCompanion.exe
echo Archive prete a envoyer : JournalPhasmo.zip
echo Aucun .NET ni fichier annexe a installer chez vos amis.
echo.
explorer "%~dp0dist"
pause
