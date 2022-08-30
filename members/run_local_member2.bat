@REM Compile & run member in test environment
cd ..
CALL mvn clean compile assembly:single -Pmember2_test
xcopy .\target\cloud-servant.jar .\members\cloud-servant.jar /Y /F
cd members

docker build -t member2 -f Dockerfile-member2 .