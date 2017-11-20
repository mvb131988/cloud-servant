package ipscanner;

import java.util.ResourceBundle;

/**
 * TODO: Is applicable only for one range. 
 * Wrap into IpRangesAnalyzer
 */
public class IpRangeAnalyzer {

	private String ISP_HOME = "homelocal";
	
	private final long chunk8Mask = 255;
	private final long chunk16Mask = 65280;
	private final long chunk24Mask = 16711680;
	private final long chunk32Mask = 4278190080l;
	
	private String ranges;
	
	private long start;
	private long end = 0;
	
	private long current;
	
	public IpRangeAnalyzer() {
		//TODO: Pass range as parameter
		//Load home range. Contains single range
		ranges = ResourceBundle.getBundle("ipranges").getString(ISP_HOME);
		
		String[] homeLocal = ranges.split(";");
		String[] ipStartAndMask = homeLocal[0].split("/");
		
		String startIp = ipStartAndMask[0];
		int maskNumbers = Integer.parseInt(ipStartAndMask[1]);
		
		String[] ipChunks = startIp.split("\\.");
		start = (Long.parseLong(ipChunks[3]) 
			  + (Long.parseLong(ipChunks[2]) << 8) 
			  + (Long.parseLong(ipChunks[1]) << 16) 
			  + (Long.parseLong(ipChunks[0]) << 24));
		
		current = start;
		
		//calculate end
		long highPartMask = 0;
		for(int i = 0; i < maskNumbers; i++) {
			highPartMask = (highPartMask << 1) + 1;
		}
		highPartMask = highPartMask << (32- maskNumbers); 
		
		//lowPartMask = lowPartNumber
		long lowPartMask = 0;
		for(int i = 0; i < 32 - maskNumbers; i++) {
			lowPartMask = (lowPartMask << 1) + 1;
		}
				
		end = (highPartMask & start) + lowPartMask;
	}
	
	public String next() {
		String nextIp = ((current & chunk32Mask) >> 24) 
				+ "." + ((current & chunk24Mask) >> 16) 
				+ "." + ((current & chunk16Mask) >> 8) 
				+ "." + (current & chunk8Mask);
		
		if(current <= end) {
			current ++;
			return nextIp;
	    }
		
		return null;
	}
	
	public boolean hasNext() {
		return current <= end;
	}
	
	public void reset() {
		current = start;
	}
	
}
