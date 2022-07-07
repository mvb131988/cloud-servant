package autodiscovery.ipscanner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import transfer.HealthCheckOperation;
import transfer.context.StatusTransferContext;

/**
 * Checks list of candidate ips. Tries to connect to each candidate from the given list and initiate
 * a health check routine. If candidate is indeed cloud-servant node, would successfully complete health check routine. 
 */
public class IpValidator {

  private Logger logger = LogManager.getRootLogger();
  
  private HealthCheckOperation hco;
  
  private int masterPort;
  
  private int soTimeout;
  
  public IpValidator(HealthCheckOperation hco, int masterPort, int soTimeout) {
    super();
    this.hco = hco;
    this.masterPort = masterPort;
    this.soTimeout = soTimeout;
  }

//  public List<String> getValid(List<String> masterIpCandidates) {
//    List<String> masterIps = masterIpCandidates.stream()
//                                               .filter(this::isValid)
//                                               .collect(Collectors.toList());
//    return masterIps;
//  }
  
  public StatusTransferContext isValid(String ip) {
	StatusTransferContext stc = null;
	  
	boolean result = false;
    
    Socket s = new Socket();
    InputStream is = null;
    OutputStream os = null;
    try {
      s.setSoTimeout(soTimeout);
      s.connect(new InetSocketAddress(ip, masterPort), 1000);
      is = s.getInputStream();
      os = s.getOutputStream();
      
      stc = hco.executeAsSlave(os, is);
      result = true;
    } catch (Exception e) {
      logger.info("Invalid candidate, ip " + ip + " is non cloud-servant node");
    }
    finally {
      if(is != null) {try {is.close();} catch (IOException e) {}}
      if(os != null) {try {os.close();} catch (IOException e) {}}
      if(s != null) {try {s.close();} catch (IOException e) {}}
    }
    
    return stc;
  }
  
}
