/**
 * Autodiscovery process looks the following way:
 * 
 * case1:
 * On startup slave node is going to scan through provided range of ip addresses and using specific autodiscovery port.
 * Autodiscovery protocol will be used to check if newly founded node is master or slave. The whole autodiscovery process
 * is aimed at founding master node.
 * 
 * case2:
 * On master-node communication failure slave node is trying to reestablish connection with master. If after specific 
 * time range it doesn't succeed new autodiscovery process is set. It looks the same way as in case 1.
 * 
 * Scheduling:
 * At first, if master-node communication fails slave sets failure date to slave autodiscovery scheduler. If subsequent attempts
 * also fail failure date isn't changed, remains as firstly set. If one of subsequent attemps succeed failure date is reset to 
 * initial value(null). Subsequent attempts continue until either master-slave connection is established or scheduler decides to
 * initiate new autodiscovery process. It makes a decision based on first failure date and time period, specified as input.
 * 
 * Locally persisted data:
 * 	- master ip
 *  - first fail date
 * have to be saved locally. If at startup master ip isn't null, then it could be used to establish a connection and to skip
 * autodiscovery process. First fail date is used to understand how long master-slave communication wasn't established using
 * previously discovered master ip. The main concern is that master is going to be up only a couple of hours per day, so major
 * part of the time master-slave communication will be down. However this isn't an indicator that master ip has been changed
 * and hence no autodiscovery process is required. Only if during a relevant period of time(week/month) successful connection 
 * wasn't established this could be a signal that ip has been changed and autodiscovery process has to be set. 
 * 
 * These reasoning could be ignored for local scanning. Each time(one time in a day) master is going up new ip could be assigned
 * by router. If stick to suggestion above it's likely master-slave communication will be established only once in a week/month.
 * Local scan is not time consuming and could be done frequently(once in 10/30 minutes). This guarantees that once in a day master 
 * will be found independently of was its ip changed or not. 
 * For global scanning exist a key assumption(ip address isn't changed frequently). This means that as soon as master ip is found
 * reconnection process could go on during a big period of time(week/month) without initiating of autodiscovery process.
 * Note that global scanning is very time consuming and it has to be avoided done frequently.     
 */
package autodiscovery;