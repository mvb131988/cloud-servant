package transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import main.AppProperties;
import repository.BaseRepositoryOperations;
import transfer.constant.MasterStatus;
import transfer.constant.OperationType;
import transfer.context.StatusTransferContext;
import transformer.IntegerTransformer;
import transformer.LongTransformer;

/**
 *  Performs base/primitive transfer operations. All primitive operations are considering in the
 *  context of single file transferring. 
 */
public class BaseTransferOperations {

	private IntegerTransformer it;
	
	private LongTransformer lt;
	
	private BaseRepositoryOperations bro;
	
	private String memberId;

	public BaseTransferOperations(IntegerTransformer it, 
								  LongTransformer lt, 
								  BaseRepositoryOperations bro, 
								  AppProperties appProperties) 
	{
		super();
		this.it = it;
		this.lt = lt;
		this.bro = bro;
		this.memberId = appProperties.getMemberId();
	}

	public void sendOperationType(OutputStream os, OperationType ot) throws IOException {
		os.write(ot.getType());
	}

	public OperationType receiveOperationType(InputStream is) throws IOException {
		int actionType = is.read();
		return OperationType.to(actionType);
	}
	
	public void sendMasterStatus(OutputStream os, MasterStatus ms) throws IOException {
		os.write(ms.getValue());
		os.write(memberId.getBytes().length);
		os.write(memberId.getBytes());
	}

	public StatusTransferContext receiveMasterStatus(InputStream is) throws IOException {
		int masterStatus = is.read();
		int memberIdLength = is.read();
		byte[] bMemberId = new byte[memberIdLength];
		is.read(bMemberId);
		String memberId = new String(bMemberId);
		
		StatusTransferContext stc = 
				new StatusTransferContext(MasterStatus.to(masterStatus), memberId);
		
		return stc;
	}
	
	public OperationType checkOperationType(PushbackInputStream is) throws IOException {
		int operationType = is.read();
		is.unread(operationType);
		return OperationType.to(operationType);
	}

	public void sendLong(OutputStream os, long size) throws IOException {
		os.write(lt.packLong(size));
	}

	public long receiveLong(InputStream is) throws IOException {
		long assembledSize = -1;
		byte[] size = new byte[8];
		is.read(size);
		assembledSize = lt.extractLong(size);
		return assembledSize;
	}
	
	public void sendInteger(OutputStream os, int i) throws IOException {
		os.write(it.pack(i));
	}

	public int receiveInteger(InputStream is) throws IOException {
		int assembledI = -1;
		byte[] i = new byte[4];
		is.read(i);
		assembledI = it.extract(i);
		return assembledI;
	}
	
	public void sendSize(OutputStream os, long size) throws IOException {
		sendLong(os, size);
	}
	
	public long receiveSize(InputStream is) throws IOException {
		return receiveLong(is);
	}
	
	public void sendCreationDateTime(OutputStream os, long creationDateTime) throws IOException {
		sendLong(os, creationDateTime);
	}
	
	public long receiveCreationDateTime(InputStream is) throws IOException {
		return receiveLong(is);
	}
	
	public void sendRelativePath(OutputStream os, Path relativePath) throws IOException {
		//change to Linux compatible slashes
		String sRelativePath = relativePath.toString().replaceAll("\\\\", "\\/");
		
		byte[] b = sRelativePath.getBytes("UTF-8");
		int length = b.length;
		sendInteger(os, length);
		os.write(b);
	}

	public Path receiveRelativePath(InputStream is) throws IOException {
		// relativePathSize
		int rns = receiveInteger(is);
		// relativePath
		byte[] rn = new byte[rns];
		is.read(rn, 0, rns);

		Path p = Paths.get(new String(rn, "UTF-8"));
		return p;
	}

	public void sendFile(OutputStream os, Path absolutePath) throws IOException {
		byte[] buffer = new byte[1024];
		int readBufferSize = -1;
		try (InputStream is = Files.newInputStream(absolutePath);) {
			while ((readBufferSize = is.read(buffer)) != -1) {
				os.write(buffer, 0, readBufferSize);
			}
		}
	}

	public void receiveFile(InputStream is, 
							long size, 
							Path repositoryRoot, 
							Path relativeFilePath, 
							long creationDateTime) throws IOException 
	{
		byte[] buffer = new byte[1024];
		int readBufferSize = -1;
		long remainigSize = size;
		
		Path p = repositoryRoot.resolve(".temp").resolve(relativeFilePath.getFileName());
		try (OutputStream os = Files.newOutputStream(p);) {
			while (remainigSize != 0) {
				readBufferSize = remainigSize >= 1024 ? is.read(buffer, 0, 1024)
						: is.read(buffer, 0, (int) remainigSize);
				remainigSize -= readBufferSize;
				os.write(buffer, 0, readBufferSize);
			}

			bro.createDirectoryIfNotExistR(relativeFilePath.getParent());
			//Move file to actual location
			bro.fromTempToRepository(relativeFilePath, creationDateTime);
		} 
	}

}
