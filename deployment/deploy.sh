#!/bin/bash

echo deploying member [$1] on remote [$2]

printf "\n### Build .jar ###\n"
cd ..
mvn clean compile assembly:single -P$1
cd deployment
printf "######\n\n"

printf "\n### Init ssh agent ###\n"
eval $(ssh-agent -s)
printf "######\n\n"

printf "### Add ssh key ###\n"
SSH_ASKPASS=./echo-secret.sh 
ssh-add ~/.ssh/ssh-keys/my-git/.ssh <<< "Quoine13Quoine13"
printf "######\n\n"

printf "### Copy .jar to remote ###\n"
cp ../target/cloud-servant.jar cloud-servant.jar
scp -P 51313 cloud-servant.jar pi@$2:/usr/cloud-servant/cloud-servant.jar

printf "### Remote reboot ###\n"
ssh pi@$2 -p 51313 "sudo reboot"
rm cloud-servant.jar