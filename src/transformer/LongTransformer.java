package transformer;

/**
 * File size value limitation 2^63 byte 
 */
public class LongTransformer {

	private final int BYTE_IN_BITS = 8;
	
	private final int INT_EXTENSION = 0xff;
	
	private long ZERO_BYTE = 0xff;
	private long FIRST_BYTE = ZERO_BYTE << BYTE_IN_BITS;
	private long SECOND_BYTE = FIRST_BYTE << BYTE_IN_BITS;
	private long THIRD_BYTE = SECOND_BYTE << BYTE_IN_BITS;
	private long FOURTH_BYTE = THIRD_BYTE << BYTE_IN_BITS;
	private long FIFTH_BYTE = FOURTH_BYTE << BYTE_IN_BITS;
	private long SIXTH_BYTE = FIFTH_BYTE << BYTE_IN_BITS;
	//Don't want to touch sign bit to avoid negative long
	//TODO: review this
	private long SEVENTH_BYTE = SIXTH_BYTE << BYTE_IN_BITS - 1;

	public long extractLong(byte[] disassembledSize) {
		long size = 0;
		
		size = ((long) disassembledSize[0] & INT_EXTENSION) + 
			   ((long) (disassembledSize[1] & INT_EXTENSION) << BYTE_IN_BITS) +
			   ((long) (disassembledSize[2] & INT_EXTENSION) << 2*BYTE_IN_BITS) +
			   ((long) (disassembledSize[3] & INT_EXTENSION) << 3*BYTE_IN_BITS) +
			   ((long) (disassembledSize[4] & INT_EXTENSION) << 4*BYTE_IN_BITS) +
			   ((long) (disassembledSize[5] & INT_EXTENSION) << 5*BYTE_IN_BITS) +
			   ((long) (disassembledSize[6] & INT_EXTENSION) << 6*BYTE_IN_BITS) +
			   ((long) (disassembledSize[7] & INT_EXTENSION) << 7*BYTE_IN_BITS);
			   
		return size;
	}

	public byte[] packLong(long size) {
		byte[] disassembledSize = new byte[8];
		
		disassembledSize[0] = (byte)(size & ZERO_BYTE);
		disassembledSize[1] = (byte)((size & FIRST_BYTE) >> BYTE_IN_BITS);
		disassembledSize[2] = (byte)((size & SECOND_BYTE)>> 2*BYTE_IN_BITS);
		disassembledSize[3] = (byte)((size & THIRD_BYTE)>> 3*BYTE_IN_BITS);
		disassembledSize[4] = (byte)((size & FOURTH_BYTE)>> 4*BYTE_IN_BITS);
		disassembledSize[5] = (byte)((size & FIFTH_BYTE)>> 5*BYTE_IN_BITS);
		disassembledSize[6] = (byte)((size & SIXTH_BYTE)>> 6*BYTE_IN_BITS);
		disassembledSize[7] = (byte)((size & SEVENTH_BYTE)>> 7*BYTE_IN_BITS);
		
		return disassembledSize;
	}

}
