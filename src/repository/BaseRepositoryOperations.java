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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autodiscovery.MemberDescriptor;
import autodiscovery.MemberType;
import exception.FilePathMaxLengthException;
import main.AppProperties;
import repository.status.AsynchronySearcherStatus;
import repository.status.FileErrorStatus;
import repository.status.RepositoryFileStatus;
import transformer.FilesContextTransformer;
import transformer.LongTransformer;

public class BaseRepositoryOperations {

	private final int smallTimeout;
	
	private Logger logger = LogManager.getRootLogger();
	
	private Path repositoryRoot;
	
	private final static int BATCH_SIZE = 10000;
	
	public final static int HEADER_SIZE = 9;

	private LongTransformer longTransformer;

	private FilesContextTransformer fct;
	
	private String memberId;

	public BaseRepositoryOperations(LongTransformer frameProcessor, 
									FilesContextTransformer fct, 
									AppProperties appProperties) {
		super();
		this.longTransformer = frameProcessor;
		this.fct = fct;

		this.repositoryRoot = appProperties.getRepositoryRoot();
		this.smallTimeout = appProperties.getSmallPoolingTimeout();
		
		this.memberId = appProperties.getMemberId();
	}

	// --------------------------------------------------------------------------------------------------------------------------------
	// Set of unused methods which are suitable for investigation purpose
	// --------------------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("unused")
	@Deprecated
	public long countRecords() throws Exception {
		long counter = -1;

		Path configPath = repositoryRoot.resolve("master.repo");
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
	public void write(int baseAddr, int id, String name, byte status) throws FileNotFoundException, IOException {
		Path configPath = repositoryRoot.resolve("master.repo");
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
	public RepositoryRecord read(int baseAddr) throws FileNotFoundException, IOException {
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
			repositoryRecord.setFileameSize(length);
			repositoryRecord.setStatus((byte) status);
			repositoryRecord.setSize(size);
			repositoryRecord.setMillisCreationDate(millisCreationDateTime);

		}

		return repositoryRecord;
	}

	// --------------------------------------------------------------------------------------------------------------------------------
	// Unused finished
	// --------------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Persists repository status descriptor on disc (in /.sys).
	 * Determines structure of file where repository status descriptor will be stored. 
	 * @throws IOException 
	 */
	public void writeRepositoryStatusDescriptor(RepositoryStatusDescriptor descriptor) throws IOException {
		Path sysPath = repositoryRoot.resolve(".sys").resolve("repo_descriptor.txt");
		try (BufferedWriter bw = Files.newBufferedWriter(sysPath)) {
			bw.write("Data.repo file status: " + descriptor.getRepositoryFileStatus().toString());
			bw.newLine();
			bw.write("Slave repository check/scan date: " + descriptor.getCheckDateTime());
			bw.newLine();
			bw.write("Data.repo file creation date: " + descriptor.getDataRepoDateTime());
			bw.newLine();
			bw.write("Total number of files in slave repository: " + descriptor.getNumberOfFiles());
			bw.newLine();
			bw.write("Total files size in slave repository: " + descriptor.getTotalSize() + " bytes");
			bw.newLine();
			bw.write("Total number of corrupted files in slave repository: " + descriptor.getNumberOfCorruptedFiles());
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
						bw.write("expected file size: " + fd.getRepositoryRecord().getSize()  + " bytes");
						bw.newLine();
						bw.write("  actual file size: " + fd.getActualSize() + " bytes");
						bw.newLine();
						bw.write("----------------------------------------------------");
						bw.newLine();
						break;
						
					case CREATION_DATE_MISMATH:
						ZonedDateTime expectedDateTime = 
							ZonedDateTime.ofInstant(Instant.ofEpochMilli(fd.getRepositoryRecord().getMillisCreationDate()), 
													ZoneId.systemDefault());
						ZonedDateTime actualDateTime = 
								ZonedDateTime.ofInstant(Instant.ofEpochMilli(fd.getMillisActualCreationDateTime()), 
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
	
	@Deprecated
	public void writeMasterIp(String ip) throws IOException {
		Path sysPath = repositoryRoot.resolve(".sys").resolve("nodes.txt");
		try (OutputStream os = Files.newOutputStream(sysPath)) {
			byte[] ipBytes = ip.getBytes("UTF-8");
			os.write(ipBytes, 0, ipBytes.length);
			os.flush();
		}
	}
	
	@Deprecated
	public String readMasterIp() throws IOException {
		Path sysPath = repositoryRoot.resolve(".sys").resolve("nodes.txt");
		
		String s;
		try (BufferedReader is = new BufferedReader(new InputStreamReader(Files.newInputStream(sysPath)))) {
			s = is.readLine();
		}
		return s;
	}
	
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
	 *            - list of repository records, corresponded to master repository file system
	 *            (relative names, sizes, creation dates) to be written into data.repo
	 * @param startId
	 *            - number used as starting to generate(increment sequence)
	 *            unique ids for all files from fileNames
	 * @throws IOException
	 * @throws FilePathMaxLengthException
	 */
	public void writeAll(List<RepositoryRecord> rrs, int startId) throws IOException, FilePathMaxLengthException {
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
				System.arraycopy(byteArray, 0, buffer, offset, RecordConstants.FILE_CREATION_DATETIME);
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
		System.arraycopy(longTransformer.packLong(creationTimestamp), 0, header, offset, RecordConstants.TIMESTAMP);
		offset += RecordConstants.TIMESTAMP;
		
		byte fileStatus = (byte) RepositoryFileStatus.RECEIVE_START.getValue(); 
		header[offset] = fileStatus;
		
		os.write(header);
		os.flush();
	}
	
	/**
	 * Extracts data.repo file creation date time from data.repo header 
	 * 
	 * @return data.repo file creation date time
	 */
	private ZonedDateTime getHeaderCreationTimestamp(byte[] header) {
		byte[] bTimestamp = new byte[RecordConstants.TIMESTAMP];
		System.arraycopy(header, 0, bTimestamp, 0, RecordConstants.TIMESTAMP);
		long timestamp = longTransformer.extractLong(bTimestamp);
		Instant i = Instant.ofEpochMilli(timestamp);
		return ZonedDateTime.ofInstant(i, ZoneOffset.systemDefault());
	}
	
	/**
	 * Extracts data.repo file status from data.repo header 
	 * 
	 * @return data.repo file status
	 */
	private RepositoryFileStatus getHeaderCreationStatus(byte[] header) {
		RepositoryFileStatus status = RepositoryFileStatus.to(header[RecordConstants.TIMESTAMP]);
		return status;
	}
	
	/**
	 * Creates directory relative to the repository root
	 * 
	 * @throws IOException
	 */
	@Deprecated
	public void createDirectoryIfNotExist(Path relativePath) throws IOException {
		if (relativePath != null) {
			Path newPath = repositoryRoot.resolve(relativePath);
			if (!Files.exists(newPath)) {
				Files.createDirectories(newPath);
			}
		}
	}

	/**
	 * Creates directory given by the path
	 * 
	 * @throws IOException
	 */
	public void createDirectoryIfNotExist0(Path path) throws IOException {
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
		size = Files.readAttributes(repositoryRoot.resolve(relativePath), BasicFileAttributes.class).size();
		return size;
	}

	public long getCreationDateTime(Path relativePath) throws IOException {
		long creationDateTime = 0;
		creationDateTime = Files.readAttributes(repositoryRoot.resolve(relativePath), BasicFileAttributes.class)
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
		Files.setAttribute(repositoryRoot.resolve(relativePath), "dos:hidden", Boolean.TRUE,
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
	public void fromTempToRepository(Path relativeFilePath, long creationDateTime) throws IOException {
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
	
	public void readDataRepoStatus(RepositoryFileStatus status) {
		
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
	private void closeDataRepo(InputStream is) throws IOException {
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
	private int next(InputStream is, byte[] buffer) throws IOException {
		int readBytes = 0;
		readBytes = is.read(buffer);
		return readBytes;
	}

	/**
	 * asynchrony - is a file that exists in data.repo and doesn't in slave file
	 * system or vice versa.
	 */
	// TODO(FUTURE): Delete operation isn't implemented
	public class AsynchronySearcher implements Runnable {

		// Records that don't have corresponding file in the repository
		private Queue<RepositoryRecord> asynchronyBuffer;

		// max asynchrony buffer size
		private final int asynchronyBufferMaxSize = 500;

		private AsynchronySearcherStatus asynchronySearcherStatus;

		private String memberId;
		
		private AsynchronySearcher(String memberId) {
			asynchronyBuffer = new ArrayBlockingQueue<>(asynchronyBufferMaxSize);
			asynchronySearcherStatus = AsynchronySearcherStatus.READY;
			
			this.memberId = memberId;
		}

		@Override
		public void run() {
			try{
				asynchronySearcherStatus = AsynchronySearcherStatus.BUSY;
	
				byte[] buffer = new byte[RecordConstants.FULL_SIZE * BATCH_SIZE];
	
				InputStream is = openDataRepo(memberId);
				readDataRepoHeader(is);
				int readBytes = 0;
				while ((readBytes = next(is, buffer)) != -1) {
					List<RepositoryRecord> records = fct.transform(buffer, readBytes);
					for (RepositoryRecord rr : records) {
	
						// Set current record path to be used for file transfer
						if (!existsFile(Paths.get(rr.getFileName()))) {
							
							// if previous read asynchrony isn't consumed wait until
							// it is consumed
							while (bufferFull()) {
								//Waiting for SlaveMasterCommunicationThread to consume a record from a buffer.
								//Wait 1 second to avoid resources overconsumption.
								Thread.sleep(smallTimeout);
							}
							setAsynchrony(rr);
						}
	
						// If file for record exists skip it move to next one
						if (existsFile(Paths.get(rr.getFileName()))) {
							// log as existed one
						}
					}
				}
				closeDataRepo(is);
	
				// Last read. Wait until last read record isn't consumed.
				while (!bufferEmpty()) {
					
					//Waiting for SlaveMasterCommunicationThread to consume all records from a buffer.
					//Wait 1 second to avoid resources overconsumption.
					Thread.sleep(smallTimeout);
				}
	
				asynchronySearcherStatus = AsynchronySearcherStatus.TERMINATED;
			} 
			catch(Exception e) {
				logger.error("[" + this.getClass().getSimpleName() + "] thread fail", e);
			}
		}

		/**
		 * @return true if buffer size exceeds asynchronyBufferMaxSize
		 */
		private boolean bufferFull() {
			return asynchronyBuffer.size() == asynchronyBufferMaxSize;
		}

		/**
		 * @return true when buffer size is zero
		 */
		private boolean bufferEmpty() {
			return asynchronyBuffer.size() == 0;
		}

		public RepositoryRecord nextAsynchrony() {
			return asynchronyBuffer.poll();
		}

		private void setAsynchrony(RepositoryRecord rr) {
			asynchronyBuffer.offer(rr);
		}

		public AsynchronySearcherStatus getStatus() {
			return asynchronySearcherStatus;
		}

	}

	public AsynchronySearcher getAsynchronySearcher(String memberId) {
		return new AsynchronySearcher(memberId);
	}

	/**********************************************************************************************
	 * ============================================================================================
	 *	unused
	 * ============================================================================================
	 **********************************************************************************************/
	public class IpsChunkWriter {
		
		private Path filePath;
		
		private BufferedWriter writer;
		
		public IpsChunkWriter(int chunkId) {
			filePath = repositoryRoot.resolve(".sys").resolve("chunk_" + chunkId + ".txt");
		}
		
		public void open() {
			try {
				writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(filePath)));
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
	/**********************************************************************************************/
	
	/**********************************************************************************************
	 *	Iterates throughout data.repo and returns next record if available until end of
	 *	data.repo is not reached
	 **********************************************************************************************/
	public class DataRepoIterator {
		
		private List<RepositoryRecord> records;
		
		//position of the current record in the array records
		private int iRecord;
		
		private InputStream is;
		
		private byte[] header;
		
		private byte[] buffer = new byte[RecordConstants.FULL_SIZE * BATCH_SIZE];
		
		public DataRepoIterator(String memberId) throws IOException {
			is = openDataRepo(memberId);
			header = readDataRepoHeader(is);
		}
		
		public RepositoryRecord nextRecord() throws IOException {
			return records.get(iRecord++); 
		}
		
		public boolean hasNextRecord() throws IOException {
			if (records == null || iRecord == records.size()) {
				int readBytes = next(is, buffer);

				if (readBytes == -1) {
					closeDataRepo(is);
					return false;
				}

				records = fct.transform(buffer, readBytes);
				iRecord = 0;
			}

			return true;
		}
		
		public void close() throws IOException {
			closeDataRepo(is);
		}

		public byte[] getHeader() {
			return header;
		}
		
	}
	
	public DataRepoIterator dataRepoIterator(String memberId) throws IOException {
		return new DataRepoIterator(memberId);
	}
	
	/**********************************************************************************************
	 * For each record from data.repo looks up for a corresponding file in the slave
	 * repository and checks whether all properties(size, creation date) from the record are identical
	 * to all properties of the file. Returns RepositoryDescriptor with list of files that don't math this 
	 * rule plus some additional info about current repository state(like scan date, current repository date, 
	 * number of files in the repository, their size and so on...)  
	 **********************************************************************************************/
	public class RepositoryConsistencyChecker {
		
		public RepositoryStatusDescriptor check(String memberId) {
			int recordsCounter = 0;
			long totalSize = 0;
			//TODO(FUTURE): In scan is done on empty repo and data.repo list is huge can lead 
			//to memory over consumption. Needs to be implemented lazily.
			List<FileDescriptor> corruptedFiles = new ArrayList<>();
			RepositoryStatusDescriptor repoDescriptor = new RepositoryStatusDescriptor();
			DataRepoIterator iterator = null;
			
			try {
				iterator = dataRepoIterator(memberId);
				RepositoryFileStatus repoFileStatus = getHeaderCreationStatus(iterator.getHeader());
				repoDescriptor.setRepositoryFileStatus(repoFileStatus);
				
				if (repoFileStatus == RepositoryFileStatus.RECEIVE_END) {
					while (iterator.hasNextRecord()) {
						RepositoryRecord rr = iterator.nextRecord();

						// check record
						FileDescriptor fd = check(rr);
						if (fd != null) {
							corruptedFiles.add(fd);
						} else {
							recordsCounter++;
							totalSize += getSize(Paths.get(rr.getFileName()));
						}
					}

					repoDescriptor.setCheckDateTime(ZonedDateTime.now());
					repoDescriptor.setDataRepoDateTime(getHeaderCreationTimestamp(iterator.getHeader()));
					repoDescriptor.setNumberOfFiles(recordsCounter);
					repoDescriptor.setTotalSize(totalSize);
					repoDescriptor.setNumberOfCorruptedFiles(corruptedFiles.size());
					repoDescriptor.setCorruptedFiles(corruptedFiles);
				}
			} catch (IOException e) {
				logger.error("[" + this.getClass().getSimpleName()+ "] i/o exception during slave repository scan", e);
			}
			finally {
				try {
					if(iterator != null) {
						iterator.close();
					}
				} catch (IOException e) {
					logger.error("[" + this.getClass().getSimpleName()+ "] i/o exception during slave repository scan", e);
				}
			}
			
			return repoDescriptor;
		}
		
		/**
		 * 
		 * @return null - if file is valid
		 * 		   FileDescriptor - if file is invalid
		 * @throws IOException 
		 */
		private FileDescriptor check(RepositoryRecord rr) throws IOException {
			FileDescriptor fd = null;
			Path p = Paths.get(rr.getFileName());
			long size = -1;
			long creationDateTime = -1;
			
			if(!existsFile(p)) {
				fd = new FileDescriptor(rr, FileErrorStatus.NOT_EXIST);
			} 
			else if((size = getSize(p)) != rr.getSize()) {
				fd = new FileDescriptor(rr, FileErrorStatus.SIZE_MISMATCH);
				fd.setActualSize(size);
			}
			else if((creationDateTime = getCreationDateTime(p)) != rr.getMillisCreationDate()){
				fd = new FileDescriptor(rr, FileErrorStatus.CREATION_DATE_MISMATH);
				fd.setMillisActualCretionDateTime(creationDateTime);
			}
			return fd;
		}
		
	}
	
	public RepositoryConsistencyChecker repositoryConsistencyChecker() {
		return new RepositoryConsistencyChecker();
	}
	
}
