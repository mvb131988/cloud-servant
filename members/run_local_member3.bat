@REM Compile & run member in test environment
cd ..
CALL mvn clean compile assembly:single -Pmember3_test
xcopy .\target\cloud-servant.jar .\members\cloud-servant.jar /Y /F
cd members

docker build -t member3 -f Dockerfile-member3 .

# for local start
# docker run -it -p 4003:4003 member3
# java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:4003 /tmp/cloud-servant.jar start