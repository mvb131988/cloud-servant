@REM Compile & run slave member in local environment
CALL mvn clean compile assembly:single -Pmaster_windows
xcopy .\target\cloud-servant.jar .\cloud-servant-master.jar /Y /F
java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:4002 cloud-servant-master.jar start