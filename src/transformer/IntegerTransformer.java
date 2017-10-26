package transformer;

public class IntegerTransformer {

private final int BYTE_IN_BITS = 8;
	
	private final int INT_EXTENSION = 0xff;
	
	private int ZERO_BYTE = 0xff;
	private int FIRST_BYTE = ZERO_BYTE << BYTE_IN_BITS;
	private int SECOND_BYTE = FIRST_BYTE << BYTE_IN_BITS;
	private int THIRD_BYTE = SECOND_BYTE << BYTE_IN_BITS;
	
	public int extract(byte[] disassembledSize) {
		int size = 0;
		
		size = ((int) disassembledSize[0] & INT_EXTENSION) + 
			   ((int) (disassembledSize[1] & INT_EXTENSION) << BYTE_IN_BITS) +
			   ((int) (disassembledSize[2] & INT_EXTENSION) << 2*BYTE_IN_BITS) +
			   ((int) (disassembledSize[3] & INT_EXTENSION) << 3*BYTE_IN_BITS);
			   
		return size;
	}
	
	public byte[] pack(int size) {
		byte[] disassembledSize = new byte[4];
		
		disassembledSize[0] = (byte)(size & ZERO_BYTE);
		disassembledSize[1] = (byte)((size & FIRST_BYTE) >> BYTE_IN_BITS);
		disassembledSize[2] = (byte)((size & SECOND_BYTE)>> 2*BYTE_IN_BITS);
		disassembledSize[3] = (byte)((size & THIRD_BYTE)>> 3*BYTE_IN_BITS);
		
		return disassembledSize;
	}
	
}
