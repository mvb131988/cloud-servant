@REM Compile & run member in test environment
cd ..
CALL mvn clean compile assembly:single -Pmember1_test
xcopy .\target\cloud-servant.jar .\members\cloud-servant.jar /Y /F
cd members

docker build -t member1 -f Dockerfile-member1 .