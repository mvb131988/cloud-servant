package file.repository;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import protocol.file.FrameProcessor;

public class RepositoryManager {

	private Path repositoryRoot = Paths.get("C:\\temp");

	private FrameProcessor frameProcessor = new FrameProcessor();

	public static void main(String[] args) {
		RepositoryManager repositoryManager = new RepositoryManager();
		repositoryManager.init();
		repositoryManager.read();
	}

	public void init() {
		Path configPath = repositoryRoot.resolve("master.repo");
		try (OutputStream os = Files.newOutputStream(configPath);) {

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try (RandomAccessFile file = new RandomAccessFile(configPath.toString(), "rw")) {
			long id = 12345;
			String name = "C:\\temp\\file.txt";
			byte status = 10;

			// offset = cursor pos
			int offset = 0;

			// file id
			file.seek(0);
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

	public RepositoryRecord read() {
		RepositoryRecord r = new RepositoryRecord();

		int offset = 0;

		Path configPath = Paths.get("C:\\temp\\master.repo");
		try (RandomAccessFile file = new RandomAccessFile(configPath.toString(), "r")) {
			// file id
			file.seek(0);
			byte[] bId = new byte[8];
			file.read(bId, offset, 8);
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

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

}
