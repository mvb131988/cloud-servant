package protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

import protocol.constant.OperationType;
import protocol.file.FrameProcessor;

/**
 *  Performs base/primitive transfer operations. All primitive operations are considering in the context of single file transfering. 
 */
public class BaseTransferOperations {

	private FrameProcessor fp;

	public BaseTransferOperations(FrameProcessor fp) {
		super();
		this.fp = fp;
	}

	public void sendOperationType(OutputStream os, OperationType ot) {
		try {
			os.write(ot.getType());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public OperationType receiveOperationType(InputStream is) {
		int actionType = -1;
		try {
			actionType = is.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return OperationType.to(actionType);
	}

	public OperationType checkOperationType(PushbackInputStream is) {
		int operationType = -1;
		try {
			operationType = is.read();
			is.unread(operationType);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return OperationType.to(operationType);
	}

	public void sendLong(OutputStream os, long size) {
		try {
			os.write(fp.packSize(size));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public long receiveLong(InputStream is) {
		long assembledSize = -1;
		try {
			byte[] size = new byte[8];
			is.read(size);
			assembledSize = fp.extractSize(size);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return assembledSize;
	}
	
	public void sendSize(OutputStream os, long size) {
		sendLong(os, size);
	}
	
	public long receiveSize(InputStream is) {
		return receiveLong(is);
	}
	
	public void sendCreationDateTime(OutputStream os, long creationDateTime) {
		sendLong(os, creationDateTime);
	}
	
	public long receiveCreationDateTime(InputStream is) {
		return receiveLong(is);
	}
	
	public void sendRelativePath(OutputStream os, Path relativePath) {
		int length = 0;
		byte[] b;
		try {
			b = relativePath.toString().getBytes("UTF-8");
			length = b.length;
			os.write(length);
			os.write(b);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Path receiveRelativePath(InputStream is) {
		Path p = null;
		try {
			// relativePathSize
			int rns = is.read();
			// relativePath
			byte[] rn = new byte[rns];
			is.read(rn, 0, rns);

			p = Paths.get(new String(rn, "UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return p;
	}

	public void sendFile(OutputStream os, Path absolutePath) {
		byte[] buffer = new byte[1024];
		int readBufferSize = -1;
		try (InputStream is = Files.newInputStream(absolutePath);) {
			while ((readBufferSize = is.read(buffer)) != -1) {
				os.write(buffer, 0, readBufferSize);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void receiveFile(InputStream is, long size, Path repositoryRoot, Path relativeFilePath, long creationDateTime) {
		byte[] buffer = new byte[1024];
		int readBufferSize = -1;
		long remainigSize = size;

		Path p = repositoryRoot.resolve(relativeFilePath);

		try (OutputStream os = Files.newOutputStream(p);) {
			while (remainigSize != 0) {
				readBufferSize = remainigSize >= 1024 ? is.read(buffer, 0, 1024)
						: is.read(buffer, 0, (int) remainigSize);
				remainigSize -= readBufferSize;
				os.write(buffer, 0, readBufferSize);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			Files.setAttribute(p, "creationTime", FileTime.fromMillis(creationDateTime));
			Files.setAttribute(p, "lastModifiedTime", FileTime.fromMillis(creationDateTime));
			Files.setAttribute(p, "lastAccessTime", FileTime.fromMillis(creationDateTime));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
