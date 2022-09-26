#Deprecated

#
# Initial setup. Before running this script install ssh certificates on
# each remote member/node:
# 
# ssh-copy-id -i ~/.ssh/ssh-keys/my-git/.ssh pi@92.115.183.17 -p 51313
# ssh-copy-id -i ~/.ssh/ssh-keys/my-git/.ssh pi@95.65.61.110 -p 51313
# ssh-copy-id -i ~/.ssh/ssh-keys/my-git/.ssh pi@188.237.1.27 -p 51313

printf "\n### Init ssh agent ###\n"
eval $(ssh-agent -s)
printf "######\n\n"

printf "### Add ssh key ###\n"
SSH_ASKPASS=./echo-secret.sh 
ssh-add ~/.ssh/ssh-keys/my-git/.ssh <<< "Quoine13Quoine13"
printf "######\n\n"

# TODO:
# build cloud-servant for node 1 using profile -Pmember1
# copy .jar to /usr/cloud-servant
# reboot machine

#printf "### Connecting to remote host1 ###\n\n"
#ssh pi@92.115.183.17 -p 51313
#printf "######\n\n"

#printf "### Connecting to remote host2 ###\n\n"
#ssh pi@95.65.61.110 -p 51313 "pwd; cd /usr/cloud-servant; ls -la; top"
#printf "######\n\n"

printf "### Connecting to remote host3 ###\n\n"
ssh pi@188.237.1.27 -p 51313
printf "######\n\n"