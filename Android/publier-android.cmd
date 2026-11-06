@echo off
REM ============================================================
REM  Fabrique une version installable de Journal Phasmo pour Android.
REM  A LANCER DEPUIS LE DOSSIER EXTRAIT, pas depuis l'interieur du .zip.
REM  Resultat : le dossier "dist" avec le .apk a mettre sur le telephone.
REM ============================================================

setlocal enabledelayedexpansion
cd /d "%~dp0"

if not exist "%~dp0gradlew.bat" (
  echo.
  echo ============================================================
  echo  gradlew.bat est introuvable a cote de ce script.
  echo ============================================================
  echo.
  echo  Cause la plus frequente : le script a ete lance DEPUIS LE ZIP.
  echo  Extrayez l'archive, puis relancez publier-android.cmd dans le
  echo  dossier obtenu.
  echo.
  pause
  exit /b 1
)

REM --- 1. retrouver le SDK Android -----------------------------
REM On prefere un SDK ou l'on peut ECRIRE : celui d'Unity est dans
REM Program Files, donc en lecture seule sans droits administrateur.
set "SDK="
set "SDKLECTURESEULE=0"

if exist "%LOCALAPPDATA%\Android\Sdk\platforms" set "SDK=%LOCALAPPDATA%\Android\Sdk"
if not defined SDK if defined ANDROID_HOME     if exist "%ANDROID_HOME%\platforms"     set "SDK=%ANDROID_HOME%"
if not defined SDK if defined ANDROID_SDK_ROOT if exist "%ANDROID_SDK_ROOT%\platforms" set "SDK=%ANDROID_SDK_ROOT%"

if not defined SDK (
  for /d %%v in ("%ProgramFiles%\Unity\Hub\Editor\*") do (
    if exist "%%v\Editor\Data\PlaybackEngines\AndroidPlayer\SDK\platforms" (
      set "SDK=%%v\Editor\Data\PlaybackEngines\AndroidPlayer\SDK"
    )
  )
)

if not defined SDK (
  echo.
  echo ============================================================
  echo  Le SDK Android est introuvable.
  echo ============================================================
  echo.
  echo  Installez Android Studio, il s'occupe de tout :
  echo     https://developer.android.com/studio
  echo.
  echo  Ou, si vous avez Unity : Unity Hub ^> Installs ^> votre version
  echo  ^> la roue ^> Add modules ^> cocher "Android Build Support".
  echo.
  pause
  exit /b 1
)
echo SDK Android   : !SDK!

REM le SDK est-il accessible en ecriture ?
>"!SDK!\.essai-ecriture" echo x 2>nul
if exist "!SDK!\.essai-ecriture" (
  del "!SDK!\.essai-ecriture" >nul 2>nul
) else (
  set "SDKLECTURESEULE=1"
  echo                 ^(lecture seule : on construira avec ce qui est deja dedans^)
)

REM --- 2. que contient-il ? -----------------------------------
REM On construit avec le niveau d'API et les build-tools DEJA presents.
REM Sans ca, Gradle veut les telecharger, et il ne peut ni ecrire dans
REM Program Files ni y enregistrer l'acceptation des licences.
set "MAXAPI=0"
for /d %%p in ("!SDK!\platforms\android-*") do (
  set "N=%%~nxp"
  set "N=!N:android-=!"
  echo !N!| findstr /r "^[0-9][0-9]*$" >nul && (
    if !N! GTR !MAXAPI! set "MAXAPI=!N!"
  )
)

set "OUTILS="
set "MAXMAJ=0"
for /d %%b in ("!SDK!\build-tools\*") do (
  for /f "tokens=1 delims=." %%m in ("%%~nxb") do (
    echo %%m| findstr /r "^[0-9][0-9]*$" >nul && (
      if %%m GEQ !MAXMAJ! (
        set "MAXMAJ=%%m"
        set "OUTILS=%%~nxb"
      )
    )
  )
)

if "!MAXAPI!"=="0" (
  echo.
  echo ============================================================
  echo  Aucune plateforme Android installee dans ce SDK.
  echo ============================================================
  echo.
  echo  Dossier regarde : !SDK!\platforms
  echo.
  echo  Le plus simple : installer Android Studio, lancer son
  echo  "SDK Manager" et laisser faire.
  echo     https://developer.android.com/studio
  echo.
  pause
  exit /b 1
)

echo Niveau d'API  : !MAXAPI!
if defined OUTILS echo Build-tools   : !OUTILS!

if !MAXAPI! LSS 35 (
  echo.
  echo  Note : l'application sera construite pour le niveau d'API !MAXAPI!.
  echo  Google Play impose un niveau minimum pour les nouvelles applications,
  echo  et ce minimum monte chaque annee : verifiez-le dans la console avant
  echo  de publier. Pour essayer sur votre telephone, cela n'a aucune importance.
  echo.
)

set "OPTS=-PsdkCible=!MAXAPI! -Pandroid.suppressUnsupportedCompileSdk=!MAXAPI!"
if defined OUTILS set "OPTS=!OPTS! -PoutilsCible=!OUTILS!"

REM --- 3. retrouver un JDK 17 ou plus --------------------------
set "JDK="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JDK=%JAVA_HOME%"
if not defined JDK if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\javac.exe" set "JDK=%ProgramFiles%\Android\Android Studio\jbr"
if not defined JDK (
  for /d %%v in ("%ProgramFiles%\Unity\Hub\Editor\*") do (
    if exist "%%v\Editor\Data\PlaybackEngines\AndroidPlayer\OpenJDK\bin\javac.exe" (
      set "JDK=%%v\Editor\Data\PlaybackEngines\AndroidPlayer\OpenJDK"
    )
  )
)
if not defined JDK (
  echo  Aucun JDK trouve. Android Studio et Unity en fournissent un.
  pause
  exit /b 1
)
echo Java          : !JDK!
set "JAVA_HOME=!JDK!"

REM --- 4. dire a Gradle ou est le SDK --------------------------
set "ECHAPPE=!SDK:\=\\!"
> "%~dp0local.properties" echo sdk.dir=!ECHAPPE!

REM --- 5. LE TEST D'ABORD -------------------------------------
echo.
echo === Verification du moteur d'identification ===
echo     1300 etats releves sur la page web sont rejoues.
echo     Si un seul differe, on ne construit rien.
echo.
if exist "%~dp0app\build\reports\tests\testDebugUnitTest" rd /s /q "%~dp0app\build\reports\tests\testDebugUnitTest" 2>nul

call "%~dp0gradlew.bat" --console=plain !OPTS! test
if errorlevel 1 (
  echo.
  if exist "%~dp0app\build\reports\tests\testDebugUnitTest\index.html" (
    echo ============================================================
    echo  LE TEST A ECHOUE : on ne construit pas l'application.
    echo ============================================================
    echo.
    echo  Un ecart ici veut dire qu'une entite serait mal identifiee
    echo  dans le jeu. Le rapport detaille :
    echo     app\build\reports\tests\testDebugUnitTest\index.html
  ) else (
    echo ============================================================
    echo  LA CONSTRUCTION A ECHOUE AVANT LE TEST.
    echo ============================================================
    echo.
    echo  Le moteur n'a pas ete mis en cause : Gradle s'est arrete plus tot.
    echo  Relisez le message ci-dessus. Les deux causes habituelles :
    echo.
    echo   - "licences have not been accepted" ou "SDK is read-only" :
    echo     le SDK ne contient pas ce qu'il faut et ne peut pas etre
    echo     complete. Installez Android Studio, ouvrez son SDK Manager
    echo     et installez une plateforme ^(ex. Android 15^) :
    echo        https://developer.android.com/studio
    echo     Ce script prendra alors ce SDK-la automatiquement.
    echo.
    echo   - une erreur de compilation Kotlin : copiez le message et
    echo     envoyez-le, il indique le fichier et la ligne.
  )
  echo.
  pause
  exit /b 1
)
echo.
echo Moteur conforme a la page web.

REM --- 6. construire ------------------------------------------
echo.
if exist "%~dp0keystore.properties" (
  echo === Construction de la version signee ===
  call "%~dp0gradlew.bat" --console=plain !OPTS! assembleRelease bundleRelease
  set "SORTIE=app\build\outputs\apk\release\app-release.apk"
  set "PAQUET=app\build\outputs\bundle\release\app-release.aab"
) else (
  echo === Construction de la version de debogage ===
  echo     ^(aucune cle de signature : voir creer-cle.cmd pour la version signee^)
  call "%~dp0gradlew.bat" --console=plain !OPTS! assembleDebug
  set "SORTIE=app\build\outputs\apk\debug\app-debug.apk"
  set "PAQUET="
)
if errorlevel 1 (
  echo.
  echo La construction a echoue. Copiez le message d'erreur ci-dessus.
  pause
  exit /b 1
)

REM --- 7. rassembler le resultat ------------------------------
if not exist "%~dp0dist" mkdir "%~dp0dist"
copy /y "%~dp0!SORTIE!" "%~dp0dist\JournalPhasmo.apk" >nul
if defined PAQUET if exist "%~dp0!PAQUET!" copy /y "%~dp0!PAQUET!" "%~dp0dist\JournalPhasmo.aab" >nul

echo.
echo === Termine ===
echo Fichier a mettre sur le telephone : dist\JournalPhasmo.apk
if exist "%~dp0dist\JournalPhasmo.aab" echo Fichier pour Google Play          : dist\JournalPhasmo.aab
echo.
echo Sur le telephone : ouvrez le .apk depuis le gestionnaire de fichiers
echo et autorisez l'installation depuis cette source quand Android le demande.
echo.
explorer "%~dp0dist"
pause
