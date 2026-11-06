@echo off
REM ============================================================
REM  Cree la cle de signature de Journal Phasmo, une seule fois.
REM
REM  A LIRE AVANT DE LANCER :
REM  Cette cle est l'identite de votre application pour Google Play.
REM  Si vous la perdez, vous ne pourrez PLUS JAMAIS publier de mise
REM  a jour. Si quelqu'un l'obtient, il peut signer une mise a jour
REM  a votre place. Sauvegardez le fichier .jks ET son mot de passe
REM  ailleurs que sur cet ordinateur.
REM ============================================================

setlocal enabledelayedexpansion
cd /d "%~dp0"
chcp 65001 >nul 2>nul

if exist "%~dp0keystore.properties" (
  echo.
  echo  Une cle existe deja : keystore.properties est present.
  echo  Si vous en creez une autre, l'application ne pourra plus
  echo  mettre a jour celle deja publiee. Rien n'a ete modifie.
  echo.
  pause
  exit /b 0
)

REM --- retrouver keytool, livre avec tout JDK -----------------
set "KT="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\keytool.exe" set "KT=%JAVA_HOME%\bin\keytool.exe"
if not defined KT if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\keytool.exe" set "KT=%ProgramFiles%\Android\Android Studio\jbr\bin\keytool.exe"
if not defined KT (
  for /d %%v in ("%ProgramFiles%\Unity\Hub\Editor\*") do (
    if exist "%%v\Editor\Data\PlaybackEngines\AndroidPlayer\OpenJDK\bin\keytool.exe" (
      set "KT=%%v\Editor\Data\PlaybackEngines\AndroidPlayer\OpenJDK\bin\keytool.exe"
    )
  )
)
if not defined KT (
  echo  keytool est introuvable. Installez Android Studio, ou cochez
  echo  "Android Build Support" dans Unity Hub.
  pause
  exit /b 1
)

echo.
echo ============================================================
echo  Creation de la cle de signature
echo ============================================================
echo.
echo  Choisissez un mot de passe et NOTEZ-LE AILLEURS maintenant.
echo  Il n'est recuperable nulle part : ni par moi, ni par Google.
echo.
set /p MDP=Mot de passe (12 caracteres ou plus) : 
if "!MDP!"=="" (
  echo  Mot de passe vide : rien n'a ete fait.
  pause
  exit /b 1
)

"!KT!" -genkeypair -v ^
  -keystore "%~dp0journalphasmo.jks" ^
  -alias journalphasmo ^
  -keyalg RSA -keysize 4096 -validity 10000 ^
  -storepass "!MDP!" -keypass "!MDP!" ^
  -dname "CN=Journal Phasmo, OU=Journal Phasmo, O=Journal Phasmo, C=FR"

if errorlevel 1 (
  echo.
  echo  La creation a echoue. Rien n'a ete ecrit.
  pause
  exit /b 1
)

> "%~dp0keystore.properties" echo storeFile=journalphasmo.jks
>>"%~dp0keystore.properties" echo storePassword=!MDP!
>>"%~dp0keystore.properties" echo keyAlias=journalphasmo
>>"%~dp0keystore.properties" echo keyPassword=!MDP!

echo.
echo === Cle creee ===
echo.
echo  Deux fichiers viennent d'apparaitre, et ni l'un ni l'autre ne
echo  partira sur GitHub ^(ils sont dans .gitignore^) :
echo.
echo     journalphasmo.jks        la cle elle-meme
echo     keystore.properties      le mot de passe
echo.
echo  SAUVEGARDEZ-LES MAINTENANT ailleurs qu'ici : cle USB, disque
echo  externe, gestionnaire de mots de passe. Sans eux, plus aucune
echo  mise a jour de l'application n'est possible, jamais.
echo.
echo  publier-android.cmd produira desormais une version signee,
echo  avec en plus le .aab a envoyer sur Google Play.
echo.
pause
