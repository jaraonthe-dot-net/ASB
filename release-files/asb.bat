@REM ASB loader for windows
@echo off

set asbpath=%~dp0
set asbbasename=ASB.jar
set asbfile=%asbpath%%asbbasename%

REM Concats all arguments to pass them to java
set args=
:argactionstart
if -%1-==-- goto argactionend
set args=%args% %1
shift
goto argactionstart
:argactionend

java -jar "%asbfile%" -C%args%
set args=
