@REM Compile & run slave member in local environment
CALL mvn clean compile assembly:single -Pslave_windows
xcopy .\target\cloud-servant.jar .\cloud-servant-slave.jar /Y /F
java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:4001 cloud-servant-slave.jar start