@if "%DEBUG%"=="" @echo off
if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Android\Android Studio2\jbr"
call "E:\Android projects\PanShopManager\gradle-8.3\gradle-8.3\bin\gradle.bat" %*
