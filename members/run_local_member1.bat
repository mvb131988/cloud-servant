@REM Compile & run slave member in local environment
cd ..
CALL mvn clean compile assembly:single -Pmember1_test
xcopy .\target\cloud-servant.jar .\members\cloud-servant.jar /Y /F
cd members

docker build -t member1 -f Dockerfile-member1 .

#java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:4002 cloud-servant-master.jar start