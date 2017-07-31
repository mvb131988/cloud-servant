package file.repository;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import protocol.file.FrameProcessor;

// See MappedByteBuffer to improve performance
public class RepositoryManager {

	private Path repositoryRoot = Paths.get("D:\\temp");

	private FrameProcessor frameProcessor = new FrameProcessor();

	public static void main(String[] args) {
		RepositoryManager repositoryManager = new RepositoryManager();
		List<String> fileNames = repositoryManager.scanRepository();
		
		int base = 0;
		int recordSize = 8+8+200+1;
		int id = 0;
		for(String name: fileNames) {
			repositoryManager.write(base, ++id, name, (byte)10);
			base += recordSize;
		}
		
		RepositoryRecord repoRecord = repositoryManager.read(113*recordSize);
		
		System.out.println("Done");
		
//		repositoryManager.read(base2);
	}

	public List<String> scanRepository() {
		RepositoryVisitor repoVisitor = new RepositoryVisitor();
		try {
			Files.walkFileTree(repositoryRoot, repoVisitor);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return repoVisitor.getFilesList();
	}
	
	public void init() {
		Path configPath = repositoryRoot.resolve("master.repo");
		try (OutputStream os = Files.newOutputStream(configPath);) {

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void write(int baseAddr, int id, String name, byte status) {
		Path configPath = repositoryRoot.resolve("master.repo");
		try (RandomAccessFile file = new RandomAccessFile(configPath.toString(), "rw")) {
			
			// offset = cursor pos
			int offset = baseAddr;

			// file id
			file.seek(offset);
			file.write(frameProcessor.packSize(id));
			offset += 8;

			// file name length
			long length = name.getBytes("UTF-8").length;
			file.write(frameProcessor.packSize(length));
			offset += 8;

			// Set maximum number of bytes for file name (200 bytes as example)
			file.write(name.getBytes("UTF-8"));
			offset += 200 ;

			file.seek(offset);
			file.write(status);
			offset += 1;
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	public RepositoryRecord read(int baseAddr) {
		RepositoryRecord repositoryRecord = new RepositoryRecord();

		int offset = baseAddr;

		Path configPath = Paths.get("D:\\temp\\master.repo");
		try (RandomAccessFile file = new RandomAccessFile(configPath.toString(), "r")) {
			// file id
			file.seek(offset);
			byte[] bId = new byte[8];
			file.read(bId, 0, 8);
			long id = frameProcessor.extractSize(bId);
			offset += 8;

			// file name length
			byte[] bLength = new byte[8];
			int l = file.read(bLength, 0, 8);
			long length = frameProcessor.extractSize(bLength);
			offset += 8;
			
			byte[] bName = new byte[200];
			file.read(bName, 0, 200);
			String name = new String(bName, 0, (int)length, "UTF-8");
			offset += 200;
			
			int status = file.read();
			offset++;
			
			repositoryRecord.setId(id);
			repositoryRecord.setFileName(name);
			repositoryRecord.setFileameSize(length);
			repositoryRecord.setStatus((byte)status);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return repositoryRecord;
	}

}
