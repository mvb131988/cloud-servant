package autodiscovery.ipscanner;

import autodiscovery.IpRangeIterator;

/**
 * Inspects multiple ip ranges. 
 */
public class IpRangesAnalyzer implements IpRangeIterator {

	private IpRangeAnalyzer ira;
	
	// each element of the array is an ip range
	private String[] aIpRanges;
	private int endIndex;
	private int index;
	
	public IpRangesAnalyzer(IpRangeAnalyzer ira) {
		this.ira = ira;
	}
	
	@Override
	public String next() {
		return ira.next();
	}
	
	@Override
	public boolean hasNext() {
		//current range isn't explored till the end
		if(ira.hasNext()) {
			return true;
		}
		
		// current range explored, try next one, until first unexplored range isn't found
		if(index < endIndex - 1) {
			ira.reset(aIpRanges[++index]);
			return ira.hasNext();
		}
		
		//if all ranges doesn't contain at least one range, no candidate exits
		return false;
	}
	
	@Override
	public void reset(String ipRanges) {
		aIpRanges = ipRanges.split(";");
		endIndex = aIpRanges.length;
		index = 0;
		ira.reset(aIpRanges[0]);
	}
	
}
