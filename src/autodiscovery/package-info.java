/**
 * Autodiscovery process looks the following way:
 * 
 * case1:
 * On startup CLOUD member is going to scan through provided range of ip addresses and using 
 * specific autodiscovery port. Autodiscovery protocol will be used by each CLOUD member to find
 * all other CLOUD/SOURE members. The whole autodiscovery process is aimed at founding 
 * CLOUD/SOURE members.
 * 
 * Important: SOURCE member could be discovered only by CLOUD member that is running in the same
 * 			  local network. It is inaccessible from external.
 * 
 * case2:
 * CLOUD member uses local scan to find SOURCE member. In there is no SOURCE member in the CLOUD
 * member's local network this process continues infinitely.
 * CLOUD member uses global scan to find all other CLOUD members. If at least one CLOUD member not
 * found global scan would be restarted after some period.
 * 
 * Scheduling:
 * + If CLOUD member doesn't know any SOURCE member, local scan is scheduled
 * + If CLOUD member doesn't know all other CLOUD members, global scan is scheduled
 * + If number of failed attempts to connect to CLOUD/any SOURCE member is reached local/global
 *   scan is scheduled  
 * 
 * Scheduling is done using baseTime and autodiscovery timeout. When one of the criteria above is
 * reached together with autodicovery timeout, autodiscovery process would be started.
 * 
 * 
 * Locally persisted data:
 * + memberId of the given member
 * + memberIds of all SOURCE/CLOUD members
 * + ip addresses of discovered SOURCE/CLOUD members
 * 
 * If at startup CLOUD/SOURCE any member isn't null, then it's ip address might be used to 
 * establish a connection. In the same time if exists non-discovered member, then autodiscovery 
 * process might be started in parallel with already established communication connections.
 * 
 * Note: local scan is done fast, only 256 addresses to check. On the other side global scan is 
 * 		 time consuming (about 500K addresses for moldtelecom) and takes few days to finish. 
 */
package autodiscovery;