@echo off
set JLINK_VM_OPTIONS=
set DIR=%~dp0
"%DIR%\java" %JLINK_VM_OPTIONS% -m coderodde.WikiGameKillerFXJava/com.github.coderodde.wikipedia.game.killer.fx.WikiGameKillerFX %*
