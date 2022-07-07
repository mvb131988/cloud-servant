package autodiscovery.ipscanner;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Take input and output streams of the external member (external member to whom local member 
 * is connected). Return member id of the external member.	 
 */
public interface MemberIdFinder {

	/**
	 * If input/output streams are associated with wrong member (this might be any arbitrary
	 * server that is not part of cloud-servant cluster) it returns null.
	 * If member is valid and member id is received returns member id.
	 * 
	 * @param os
	 * @param is
	 * @return
	 */
	String memberId(OutputStream os, InputStream is);
	
}
