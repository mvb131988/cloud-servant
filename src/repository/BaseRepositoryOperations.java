package repository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autodiscovery.MemberDescriptor;
import autodiscovery.MemberType;
import exception.FilePathMaxLengthException;
import main.AppProperties;
import repository.status.RepositoryFileStatus;
import transformer.LongTransformer;

public class BaseRepositoryOperations {

	private Logger logger = LogManager.getRootLogger();
	
	private Path repositoryRoot;
	
	private final static int BATCH_SIZE = 10000;
	
	public final static int HEADER_SIZE = 9;

	private LongTransformer longTransformer;

	
	private String memberId;

	public BaseRepositoryOperations(LongTransformer frameProcessor,
									AppProperties appProperties) {
		super();
		this.longTransformer = frameProcessor;
		this.repositoryRoot = appProperties.getRepositoryRoot();
		this.memberId = appProperties.getMemberId();
	}

	// --------------------------------------------------------------------------------------------
	// Set of unused methods which are suitable for investigation purpose
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings("unused")
	@Deprecated
	public long countRecords() throws Exception {
		long counter = -1;

		Path configPath = repositoryRoot.resolve("data.repo");
		long size = Files.readAttributes(configPath, BasicFileAttributes.class).size();
		counter = size / RecordConstants.FULL_SIZE;
		long remainder = size % RecordConstants.FULL_SIZE;

		// File is corrupted
		if (remainder != 0) {
			throw new Exception("File is corrupted");
		}

		return counter;
	}

	@SuppressWarnings("unused")
	@Deprecated
	public void write(int baseAddr, int id, String name, byte status) 
			throws FileNotFoundException, IOException 
	{
		Path configPath = repositoryRoot.resolve("data.repo");
		try (RandomAccessFile file = new RandomAccessFile(configPath.toString(), "rw")) {

			// offset = cursor pos
			int offset = baseAddr;

			// file id
			file.seek(offset);
			file.write(longTransformer.packLong(id));
			offset += 8;

			// file name length
			long length = name.getBytes("UTF-8").length;
			file.write(longTransformer.packLong(length));
			offset += 8;

			// Set maximum number of bytes for file name (200 bytes as example)
			file.write(name.getBytes("UTF-8"));
			offset += 200;

			file.seek(offset);
			file.write(status);
			offset += 1;

		}
	}

	/**
	 * For test purposes. Could read any single record
	 * 
	 * @param baseAddr
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	public RepositoryRecord read(int baseAddr) 
			throws FileNotFoundException, IOException 
	{
		RepositoryRecord repositoryRecord = new RepositoryRecord();

		int offset = baseAddr;

		Path configPath = repositoryRoot.resolve(memberId + "_data.repo");
		try (RandomAccessFile file = new RandomAccessFile(configPath.toString(), "r")) {
			// file id
			file.seek(offset);
			byte[] bId = new byte[8];
			file.read(bId, 0, 8);
			long id = longTransformer.extractLong(bId);
			offset += 8;

			// file name length
			byte[] bLength = new byte[8];
			int l = file.read(bLength, 0, 8);
			long length = longTransformer.extractLong(bLength);
			offset += 8;

			byte[] bName = new byte[RecordConstants.NAME_SIZE];
			file.read(bName, 0, RecordConstants.NAME_SIZE);
			String name = new String(bName, 0, (int) length, "UTF-8");
			offset += RecordConstants.NAME_SIZE;

			int status = file.read();
			offset++;
			
			//file size
			byte[] bSize = new byte[RecordConstants.FILE_SIZE];
			file.read(bSize, 0, RecordConstants.FILE_SIZE);
			long size = longTransformer.extractLong(bSize);
			offset += RecordConstants.FILE_SIZE;
			
			//file creation date time
			byte[] bCreationDateTime = new byte[RecordConstants.FILE_CREATION_DATETIME];
			file.read(bCreationDateTime, 0, RecordConstants.FILE_CREATION_DATETIME);
			long millisCreationDateTime = longTransformer.extractLong(bCreationDateTime);
			offset += RecordConstants.FILE_CREATION_DATETIME;

			repositoryRecord.setId(id);
			repositoryRecord.setFileName(name);
			repositoryRecord.setFileNameSize(length);
			repositoryRecord.setStatus((byte) status);
			repositoryRecord.setSize(size);
			repositoryRecord.setMillisCreationDate(millisCreationDateTime);

		}

		return repositoryRecord;
	}

	// -------------------------------------------------------------------------------------------
	// Unused finished
	// -------------------------------------------------------------------------------------------
	
	/**
	 * Persists repository status descriptor on disc (in /.sys). Checks neighbour member 
	 * repository state identified by it's memberId (identifies which data.repo file to traverse) 
	 * with local member repository. Reports difference in file properties (between local and
	 * remote member) 
	 * @throws IOException 
	 */
	public void writeRepositoryStatusDescriptor(RepositoryStatusDescriptor descriptor, 
												String memberId) throws IOException 
	{
		Path sysPath = repositoryRoot.resolve(".sys").resolve(memberId + "_repo_descriptor.txt");
		try (BufferedWriter bw = Files.newBufferedWriter(sysPath)) {
			bw.write(memberId + "_data.repo file status: " 
					+ descriptor.getRepositoryFileStatus().toString());
			bw.newLine();
			bw.write("Local member repository check/scan date: " 
					+ descriptor.getCheckDateTime());
			bw.newLine();
			bw.write(memberId + "_data.repo file creation date: " 
					+ descriptor.getDataRepoDateTime());
			bw.newLine();
			bw.write("Total number of files in " + memberId + " repository: " 
					+ descriptor.getNumberOfFiles());
			bw.newLine();
			bw.write("Total files size in " + memberId + " repository: " 
					+ descriptor.getTotalSize() + " bytes");
			bw.newLine();
			bw.write("Total number of corrupted files in local member repository comparing to "
					+ memberId + " repository: " + descriptor.getNumberOfCorruptedFiles());
			bw.newLine();
			
			bw.write("====================================================");
			bw.newLine();
			
			if(descriptor.getNumberOfCorruptedFiles() > 0) {
				for(FileDescriptor fd: descriptor.getCorruptedFiles()) {
					switch(fd.getFileErrorStatus()) {
					
					case NOT_EXIST:
						bw.write("file not found: " + fd.getRepositoryRecord().getFileName());
						bw.newLine();
						bw.write("----------------------------------------------------");
						bw.newLine();
						break;
						
					case SIZE_MISMATCH:
						bw.write("file name: " + fd.getRepositoryRecord().getFileName());
						bw.newLine();
						bw.write("expected file size: " 
								+ fd.getRepositoryRecord().getSize()  + " bytes");
						bw.newLine();
						bw.write("  actual file size: " + fd.getActualSize() + " bytes");
						bw.newLine();
						bw.write("----------------------------------------------------");
						bw.newLine();
						break;
						
					case CREATION_DATE_MISMATH:
						ZonedDateTime expectedDateTime = 
							ZonedDateTime.ofInstant(
									Instant.ofEpochMilli(
											fd.getRepositoryRecord().getMillisCreationDate()),
									ZoneId.systemDefault());
						ZonedDateTime actualDateTime = 
							ZonedDateTime.ofInstant(
									Instant.ofEpochMilli(
											fd.getMillisActualCreationDateTime()), 
									ZoneId.systemDefault());
						bw.write("file name: " + fd.getRepositoryRecord().getFileName());
						bw.newLine();
						bw.write("expected creation date time: " + expectedDateTime);
						bw.newLine();
						bw.write("  actual creation date time: " + actualDateTime);
						bw.newLine();
						bw.write("----------------------------------------------------");
						bw.newLine();
						break;
						
					default: break;	
					
					}
				}
			}
		}
	}
	
	//TODO: pass memberId as parameter
	/**
	 * Writes the list of files into the data.repo Much faster than random
	 * access file
	 *
	 * Record format(defined by RecordConstants)
	 * -----------------------------------------------------------------------------------------
	 * | 8bytes| 8 bytes 		  | 500 bytes| 	   1 byte |	 8 bytes |                 8 bytes |
	 * -----------------------------------------------------------------------------------------
	 * |fileId | size of fileName | fileName | fileStatus | fileSize | file creation date time |
	 * -----------------------------------------------------------------------------------------
	 *
	 * @param rrs
	 *            - list of repository records, corresponded to member's repository file system
	 *            (relative names, sizes, creation dates) to be written into data.repo
	 * @param startId
	 *            - number used as starting to generate(increment sequence)
	 *            unique ids for all files from fileNames
	 * @throws IOException
	 * @throws FilePathMaxLengthException
	 */
	public void writeAll(List<RepositoryRecord> rrs, int startId) 
			throws IOException, FilePathMaxLengthException 
	{
		byte[] buffer = new byte[RecordConstants.FULL_SIZE * BATCH_SIZE];
		int offset = 0;

		int id = startId;

		Path configPath = repositoryRoot.resolve(memberId + "_data.repo");

		try (OutputStream os = Files.newOutputStream(configPath);) {
			
			//write header
			writeHeader(os);
			
			// write body
			for (RepositoryRecord rr : rrs) {
				
				// file id
				byte[] byteArray = longTransformer.packLong(++id);
				System.arraycopy(byteArray, 0, buffer, offset, RecordConstants.ID_SIZE);
				offset += RecordConstants.ID_SIZE;

				// file name length
				String fileName = rr.getFileName();
				long length = fileName.getBytes("UTF-8").length;
				byteArray = longTransformer.packLong(length);
				System.arraycopy(byteArray, 0, buffer, offset, RecordConstants.NAME_LENGTH_SIZE);
				offset += RecordConstants.NAME_LENGTH_SIZE;

				// Set maximum number of bytes for file name
				// In the worst case file path could contain
				// RecordConstants.NAME_SIZE/2 cyrillic symbols(2 bytes per
				// cyrilic symbol)
				byte[] bFileName = fileName.getBytes("UTF-8");
				if (bFileName.length > RecordConstants.NAME_SIZE) {
					throw new FilePathMaxLengthException();
				}
				System.arraycopy(bFileName, 0, buffer, offset, (int) length);
				offset += RecordConstants.NAME_SIZE;

				// Set record status code
				byte status = 1;
				buffer[offset] = status;
				offset += RecordConstants.STATUS_SIZE;
				
				// file size
				long fileSize = rr.getSize();
				byteArray = longTransformer.packLong(fileSize);
				System.arraycopy(byteArray, 0, buffer, offset, RecordConstants.FILE_SIZE);
				offset += RecordConstants.FILE_SIZE;
				
				// file creation date time
				long creationDateTime = rr.getMillisCreationDate();
				byteArray = longTransformer.packLong(creationDateTime);
				System.arraycopy(byteArray, 
								 0, 
								 buffer, 
								 offset, 
								 RecordConstants.FILE_CREATION_DATETIME);
				offset += RecordConstants.FILE_CREATION_DATETIME;

				if (offset == buffer.length) {

					// flush full buffer
					os.write(buffer, 0, offset);
					os.flush();

					buffer = new byte[RecordConstants.FULL_SIZE * BATCH_SIZE];
					offset = 0;
				}
			}

			// final flush
			// from 0 to offset - 1
			os.write(buffer, 0, offset);
			os.flush();
		}

	}

	private void writeHeader(OutputStream os) throws IOException {
		int offset = 0;
		
		//write header
		byte[] header = new byte[HEADER_SIZE];
		
		long creationTimestamp = ZonedDateTime.now().toInstant().toEpochMilli();
		System.arraycopy(longTransformer.packLong(creationTimestamp), 
						 0, 
						 header, 
						 offset, 
						 RecordConstants.TIMESTAMP);
		offset += RecordConstants.TIMESTAMP;
		
		byte fileStatus = (byte) RepositoryFileStatus.RECEIVE_START.getValue(); 
		header[offset] = fileStatus;
		
		os.write(header);
		os.flush();
	}
	
	/**
	 * Creates directory relative to the repository root (relative path is passed)
	 * 
	 * @throws IOException
	 */
	public void createDirectoryIfNotExistR(Path relativePath) throws IOException {
		if (relativePath != null) {
			Path newPath = repositoryRoot.resolve(relativePath);
			if (!Files.exists(newPath)) {
				Files.createDirectories(newPath);
			}
		}
	}

	/**
	 * Creates directory given by the path (absolute path is passed)
	 * 
	 * @throws IOException
	 */
	public void createDirectoryIfNotExistA(Path path) throws IOException {
		if (path != null) {
			if (!Files.exists(path)) {
				logger.info("Path : " + path + " does not exist");
				
				Files.createDirectories(path);
				
				logger.info("Path : " + path + " is created");
			}
		}
	}
	
	/**
	 * Creates file relative to the repository root
	 * 
	 * @throws IOException
	 */
	public void createFileIfNotExist(Path relativePath) throws IOException {
		if (relativePath != null) {
			Path newPath = repositoryRoot.resolve(relativePath);
			if (!Files.exists(newPath)) {
				Files.createFile(newPath);
			}
		}
	}
	
	/**
	 * Two similar files exist: members.properties and members.txt
	 * members.properties - is internal default read-only config in classpath
	 * 						contains no info about ips
	 * 
	 * members.txt - external editable config with the same structure and content
	 * 				 as members.properties, but with ip addresses to each remote
	 * 				 member(or empty if address is not found yet)
	 * 
	 * Creates members.txt file given by the path. Initializes members.txt file
	 * with local member id and list of remote members configuration.
	 * 
	 * @param path - path to the member.txt file, located in .sys dir
	 * @param memberId - local member id
	 * @param ds - remote members descriptors
	 * @throws IOException
	 */
	public void createMembersFileIfNotExist(Path path, 
											String memberId, 
											List<MemberDescriptor> ds) throws IOException
	{
		if (path != null) {
			if (!Files.exists(path)) {
				
				logger.info("Members.txt file : " + path + " does not exist");
				
				createMembersFile(path, memberId, ds);
				
				logger.info("Members.txt file : " + path + " is created");
			}
		}
	}
	
	private void createMembersFile(Path path, 
								   String memberId, 
								   List<MemberDescriptor> ds) throws IOException 
	{
		String s = "memberId=" + memberId + System.lineSeparator();
		s += System.lineSeparator();
		
		for(int i=0; i<ds.size()-1; i++) {
			String ip = ds.get(i).getIpAddress() != null ? ds.get(i).getIpAddress(): "";
			s += ds.get(i).getMemberId() + ":" +
				 ds.get(i).getMemberType() + ":" + 
				 ip +
				 System.lineSeparator();
		}
		if(ds.size() > 0) {
			String ip = ds.get(ds.size()-1).getIpAddress() != null ? 
						ds.get(ds.size()-1).getIpAddress() : 
						"";
			s += ds.get(ds.size()-1).getMemberId() + ":" +
				 ds.get(ds.size()-1).getMemberType() + ":" +
				 ip;
		}
		
		Files.createFile(path);
		Files.write(path, s.getBytes());
	}
	
	/**
	 * Load local member id from members.txt (members.txt is outside of the .jar file)
	 * 
	 * @param path - path to members.txt
	 * @return
	 * @throws IOException
	 */
	public String loadLocalMemberId(Path path) throws IOException {
		List<String> lines = Files.readAllLines(path);
		//memberId of the local member
		String memberId = (lines.get(0).split("="))[1];
		return memberId;
	}
	
	/**
	 * Load local member id from members.properties located in classpath (inside the .jar file)
	 * 
	 * @param name - members.properties file name in classpath
	 * @return
	 * @throws IOException
	 */
	public String loadLocalMemberId(String name) throws IOException {
		List<String> lines = loadAsLinesFromClasspath(name);
		//memberId of the local member
		String memberId = (lines.get(0).split("="))[1];
		return memberId;
	}
	
	/**
	 * Load external member descriptor from members.txt (members.txt is outside of the .jar file)
	 * 
	 * @param path - path to members.txt
	 * @return
	 * @throws IOException
	 */
	public List<MemberDescriptor> loadRemoteMembers(Path path) throws IOException {
		List<String> lines = Files.readAllLines(path);
		return toRemoteMembersDescriptors(lines);
	}
	
	/**
	 * Load external member descriptor from members.properties located in classpath
	 * (inside the .jar file)
	 * 
	 * @param name - members.properties file name in classpath
	 * @return
	 * @throws IOException
	 */
	public List<MemberDescriptor> loadRemoteMembers(String name) throws IOException {
		List<String> lines = loadAsLinesFromClasspath(name);
		return toRemoteMembersDescriptors(lines);
	}
	
	/**
	 * Convert lines from members.txt or members.properties into member descriptors
	 * 
	 * @param lines - lines from members.txt or members.properties
	 * @return
	 */
	private List<MemberDescriptor> toRemoteMembersDescriptors(List<String> lines) {
		//skip local memberId & empty line

		List<MemberDescriptor> ds = new ArrayList<>();
		for(int i=2; i<lines.size(); i++) {
			String[] parts = lines.get(i).split(":");
			MemberDescriptor d = 
					new MemberDescriptor(parts[0], 
										 MemberType.valueOf(parts[1]), 
										 parts.length == 3 ? parts[2] : null);
			ds.add(d);
		}		
		
		return ds;
	}
	
	/**
	 * Load members.properties located in classpath (inside the .jar file) in form of lines,
	 * no transformation to local member id or external member descriptors happens here.   
	 * 
	 * @param name
	 * @return
	 * @throws IOException
	 */
	private List<String> loadAsLinesFromClasspath(String name) throws IOException {
		List<String> lines = new ArrayList<>();
		try(InputStream is = getClass().getClassLoader().getResourceAsStream(name);
			BufferedReader reader = new BufferedReader(new InputStreamReader(is))) 
		{
			while(reader.ready()) {
				lines.add(reader.readLine());
			}
		}
		return lines;
	}
	
	/**
	 * Persists members descriptors by rewriting memebers.txt configuration file. 
	 * 
	 * @param path
	 * @param memberId
	 * @param ds
	 * @throws IOException
	 */
	public void persistMembersDescriptors(Path path, 
										  String memberId, 
										  List<MemberDescriptor> ds) throws IOException
	{
		Path tmpPath = path.getParent().resolve("members_tmp.txt");
		createMembersFile(tmpPath, memberId, ds);
		Files.write(path, Files.readAllBytes(tmpPath));
		Files.delete(tmpPath);
	}
	
	public long getSize(Path relativePath) throws IOException {
		long size = 0;
		size = Files.readAttributes(repositoryRoot.resolve(relativePath),
									BasicFileAttributes.class)
					.size();
		return size;
	}

	public long getCreationDateTime(Path relativePath) throws IOException {
		long creationDateTime = 0;
		creationDateTime = Files.readAttributes(repositoryRoot.resolve(relativePath),
												BasicFileAttributes.class)
								.creationTime().toMillis();
		return creationDateTime;
	}

	/**
	 * Assign hidden attribute to the directory
	 * @throws IOException 
	 */
	// TODO(NORMAL): os dependent operation. Execute only on windows. On linux
	// dir started with '.' is already hidden.
	public void hideDirectory(Path relativePath) throws IOException {
		Files.setAttribute(repositoryRoot.resolve(relativePath), 
						   "dos:hidden", 
						   Boolean.TRUE,
						   LinkOption.NOFOLLOW_LINKS);
	}

	/**
	 * Move newly received file form temp directory to actual location
	 * 
	 * @param relativeFilePath
	 *            - file name with intermediate directories. When moved to
	 *            repository file will be saved in intermediate directories
	 * @param creationDateTime
	 * @throws IOException 
	 */
	public void fromTempToRepository(Path relativeFilePath, long creationDateTime) 
			throws IOException 
	{
		Path src = repositoryRoot.resolve(".temp").resolve(relativeFilePath.getFileName());
		Path dstn = repositoryRoot.resolve(relativeFilePath).normalize();
		Files.copy(src, dstn, StandardCopyOption.REPLACE_EXISTING);
		Files.delete(src);

		// If service crashes here creation date could be lost
		Files.setAttribute(dstn, "creationTime", FileTime.fromMillis(creationDateTime));
		Files.setAttribute(dstn, "lastModifiedTime", FileTime.fromMillis(creationDateTime));
		Files.setAttribute(dstn, "lastAccessTime", FileTime.fromMillis(creationDateTime));
	}

	public boolean existsFile(Path relativePath) {
		return Files.exists(repositoryRoot.resolve(relativePath));
	}

	/**
	 * Open data.repo main repository config and return input stream associated
	 * with it.
	 * @throws IOException 
	 */
	public InputStream openDataRepo(String memberId) throws IOException {
		Path configPath = repositoryRoot.resolve(memberId + "_data.repo");
		InputStream is = null;
		is = Files.newInputStream(configPath);
		return is;
	}

	/**
	 * Read data.repo header. Header lies between 0 and HEADER_SIZE bytes.
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public byte[] readDataRepoHeader(InputStream is) throws IOException {
		byte[] header = new byte[HEADER_SIZE];
		is.read(header);
		return header;
	}
	
	/**
	 * Updates status of data.repo file 
	 * @param status - data.repo file status to be set
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public void updateDataRepoStatus(RepositoryFileStatus status, String memberId) 
			throws FileNotFoundException, IOException
	{
		Path configPath = repositoryRoot.resolve(memberId + "_data.repo");
		long offset = RecordConstants.TIMESTAMP;
		try (RandomAccessFile file = new RandomAccessFile(configPath.toString(), "rw")) {
			file.seek(offset);
			file.write(status.getValue());
		}
	}
	
	/**
	 * Open input stream associated with data.repo main repository config.
	 * @throws IOException 
	 */
	public void closeDataRepo(InputStream is) throws IOException {
		is.close();
	}

	/**
	 * Returns next bytes chunk(it contains exactly number of bytes
	 * corresponding to batch_size RepositoryRecords) from the input stream
	 * associated with data.repo main repository config.
	 * 
	 * Returns number of read bytes or -1 if end of stream reached
	 * @throws IOException 
	 */
	public int next(InputStream is, byte[] buffer) throws IOException {
		int readBytes = 0;
		readBytes = is.read(buffer);
		return readBytes;
	}


	/*****************************************************************************************
	 * =======================================================================================
	 *	unused
	 * =======================================================================================
	 ****************************************************************************************/
	public class IpsChunkWriter {
		
		private Path filePath;
		
		private BufferedWriter writer;
		
		public IpsChunkWriter(int chunkId) {
			filePath = repositoryRoot.resolve(".sys").resolve("chunk_" + chunkId + ".txt");
		}
		
		public void open() {
			try {
				writer = new BufferedWriter(
						new OutputStreamWriter(Files.newOutputStream(filePath)));
			} catch (IOException e) {
				logger.error("[" + this.getClass().getSimpleName() + "] thread fail", e);
			}
		}
		
		public void write(String s) {
			try {
				writer.write(s);
				writer.newLine();
			} catch (IOException e) {
				logger.error("[" + this.getClass().getSimpleName() + "] thread fail", e);
			}
		}
		
		public void close() {
			try {
				writer.close();
			} catch (IOException e) {
				logger.error("[" + this.getClass().getSimpleName() + "] thread fail", e);
			}
		}
		
	}

	public IpsChunkWriter getIpsChunkWriter(int chunkId) {
		IpsChunkWriter ipsChunkWriter = new IpsChunkWriter(chunkId);
		ipsChunkWriter.open();
		return ipsChunkWriter;
	}
	/*******************************************************************************************/
	
}
