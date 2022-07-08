@REM Compile & run slave member in local environment
cd ..
CALL mvn clean compile assembly:single -Pmember3_test
xcopy .\target\cloud-servant.jar .\members\cloud-servant.jar /Y /F
cd members

docker build -t member3 -f Dockerfile-member3 .

#java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:4003 cloud-servant-master.jar start