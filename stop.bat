@echo off

@REM
@REM -- to start execute  
@REM javaw -jar -Dcloud-servant cloud-servant.jar start
@REM where D is unused parameter listed by jps tool
@REM

for /F "tokens=1,3*" %%G in ('jps -v') do (
	
	@REM
	@REM G contains PID
 	@REM H marker pararmeter -Dcloud-servant
	@REM
	
	call :subroutine "%%G" "%%H"
)
GOTO :eof


:subroutine
 
 echo %1 %2

 echo.%2 | findstr /C:"-Dcloud-servant">nul
 if not errorlevel 1 (
 	echo. Killing PID=%1
 	taskkill /f /pid %1
 )
	 
GOTO :eof

