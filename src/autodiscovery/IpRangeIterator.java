package autodiscovery;

/**
 * Is used to iterate through an ip range or a set of ip ranges. Each time next method is invoked
 * returns new ip from specific ip range/ip ranges. 
 */
public interface IpRangeIterator {
	
	/**
	 * @return next ip from the input range/ranges
	 */
	String next();

	/**
	 * @return true if exists not visited ip from the input range/ranges
	 */
	boolean hasNext();

	/**
	 * Resets iterator and pushes as an input string with ip range/ranges(in case of multiple ranges ; separator is used)
	 * 
	 * @param input - input ip range/ranges
	 */
	void reset(String input);
}
