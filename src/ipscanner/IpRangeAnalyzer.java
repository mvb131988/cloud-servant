package ipscanner;

import autodiscovery.IpRangeIterator;

/**
 * Inspects single ip range. 
 * Range is represented by string in the following format:
 * iii.iii.iii.iii/pp, where iii.iii.iii.iii base address, pp mask(shows how many bits are occupied)
 */
public class IpRangeAnalyzer implements IpRangeIterator {

	private final long chunk8Mask = 255;
	private final long chunk16Mask = 65280;
	private final long chunk24Mask = 16711680;
	private final long chunk32Mask = 4278190080l;
	
	private long start;
	private long end;
	private long current;
	
	/**
	 * @return next ip address from the given range 
	 */
	@Override
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
	
	/**
	 * @return true if in the given range exists not visited ip
	 */
	@Override
	public boolean hasNext() {
		return current <= end;
	}
	
	/**
	 * Sets start, end, current to initial state
	 * 
	 * @param ipRange - ip range to be analyzed
	 */
	@Override
	public void reset(String ipRange) {
		String[] ipStartAndMask = ipRange.split("/");
		
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
	
}
