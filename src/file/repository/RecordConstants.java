package file.repository;

/**
 *	Size of file record frame in bytes
 */
public class RecordConstants {

	public static final int ID_SIZE = 8;
	
	public static final int NAME_LENGTH_SIZE = 8;
	
	public static final int NAME_SIZE = 500;
	
	public static final int STATUS_SIZE = 1;
	
	public static final int FULL_SIZE = ID_SIZE + NAME_LENGTH_SIZE + NAME_SIZE + STATUS_SIZE;

}
