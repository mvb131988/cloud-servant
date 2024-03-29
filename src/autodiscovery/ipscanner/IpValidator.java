package autodiscovery.ipscanner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Checks list of candidate ips. Tries to connect to each candidate from the given list and 
 * initiate a health check routine. If candidate is indeed cloud-servant node, would successfully
 * complete health check routine.
 */
public class IpValidator {

	private Logger logger = LogManager.getRootLogger();
  
	private MemberIdFinder mif;
  
	public IpValidator(MemberIdFinder mif) {
		super();
		this.mif = mif;
	}
	
	public IpValidatorResult isValid(Socket s, String ip) {
		IpValidatorResult result = null;
	    
	    InputStream is = null;
	    OutputStream os = null;
	    try {
	    	is = s.getInputStream();
	    	os = s.getOutputStream();
	      
	    	String memberId = mif.memberId(os, is);
	    	result = memberId != null ? new IpValidatorResult(true, memberId) 
	    		  					  : new IpValidatorResult(false, null);
	    } catch (Exception e) {
	    	result = new IpValidatorResult(false, null);
	    	logger.error("Invalid candidate, ip " + ip + " is non cloud-servant node");
	    }
	    finally {
	    	if(is != null) {try {is.close();} catch (IOException e) {}}
	    	if(os != null) {try {os.close();} catch (IOException e) {}}
	    }
  
	    return result;
	}
  
}
