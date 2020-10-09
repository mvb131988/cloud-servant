@REM Run as a javaw process outside command line
@REM D is unused parameter listed by jps tool, however it is used by stop.bat script to find jvm that has to be stopped
@REM 
@REM To run it on windows startup, but before log on use: 
@REM Control Panel -> Administrative Tools-> Task Scheduler  
@REM and create a task that starts before log on. During task creation set path to launch.bat  

@REM Change to the program path
cd C:\Users\vminacov\eclipse-workspace\cloud-servant\target

start javaw -jar -Dcloud-servant cloud-servant.jar start
exit


@REM Or
start full_path_to\javaw -jar -Dcloud-servant full_path_to\cloud-servant.jar start
exit