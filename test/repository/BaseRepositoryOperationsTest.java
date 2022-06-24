package repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import autodiscovery.MemberDescriptor;
import autodiscovery.MemberType;
import main.AppProperties;

public class BaseRepositoryOperationsTest {

	@Test
	public void testCreateMembersFileIfNotExist() throws IOException {
		Path pathSys = Paths.get("").resolve("test-repo-root")
									.resolve(".sys")
			  	 				 	.toAbsolutePath();
		
		if (Files.exists(pathSys)) {
			Files.walk(pathSys)
			     .sorted(Comparator.reverseOrder())
			     .map(Path::toFile)
			     .forEach(File::delete);
			Files.deleteIfExists(pathSys);
		}
		
		Files.createDirectories(pathSys);
		
		Path pathMembers = pathSys.resolve("members.txt");
		String memberId = "member1";
		List<MemberDescriptor> ds = new ArrayList<>();
		ds.add(new MemberDescriptor("member2", MemberType.CLOUD, null));
		ds.add(new MemberDescriptor("member3", MemberType.CLOUD, null));
		ds.add(new MemberDescriptor("member4", MemberType.CLOUD, null));
		ds.add(new MemberDescriptor("member5", MemberType.SOURCE, null));
		
		AppProperties appPropertiesMock = mock(AppProperties.class);
		BaseRepositoryOperations bro = new BaseRepositoryOperations(null, null, appPropertiesMock);
		bro.createMembersFileIfNotExist(pathMembers, memberId, ds);
		
		Path expectedPathMembers = Paths.get("").resolve("test-resources")
												.resolve("base-repository-operations")
											  	.resolve("expected-members.txt")
											  	.toAbsolutePath();
		
		List<String> actual = Files.lines(pathMembers).collect(Collectors.toList());
		List<String> expected = Files.lines(expectedPathMembers).collect(Collectors.toList());
		
		assertAll("membersFile",
				() -> assertEquals(expected.size(), actual.size()),
				() -> assertEquals(expected.get(0), actual.get(0)),
				() -> assertEquals(expected.get(1), actual.get(1)),
				() -> assertEquals(expected.get(2), actual.get(2)),
				() -> assertEquals(expected.get(3), actual.get(3)),
				() -> assertEquals(expected.get(4), actual.get(4)));
	}
	
	@Test
	public void testLoadLocalMemberId1() throws IOException, URISyntaxException {
		Path pathSys = Paths.get("").resolve("test-repo-root")
									.resolve(".sys")
									.toAbsolutePath();
					
		if (Files.exists(pathSys)) {
			Files.walk(pathSys)
				 .sorted(Comparator.reverseOrder())
				 .map(Path::toFile)
				 .forEach(File::delete);
			Files.deleteIfExists(pathSys);
		}
		
		Files.createDirectories(pathSys);
							
		Path pathMembers = pathSys.resolve("members.txt");
		try(FileWriter fileWriter = new FileWriter(pathMembers.toFile());
		  PrintWriter printWriter = new PrintWriter(fileWriter);){
		  printWriter.print("memberId=member1\r\n"
					  	  + "\r\n"
					  	  + "member2:CLOUD:\r\n"
					  	  + "member3:CLOUD:\r\n"
					  	  + "member4:CLOUD:\r\n"
					  	  + "member5:SOURCE:");
		}
		
		AppProperties appPropertiesMock = mock(AppProperties.class);
		BaseRepositoryOperations bro = new BaseRepositoryOperations(null, null, appPropertiesMock);
		String memberId = bro.loadLocalMemberId(pathMembers);
		
		assertEquals("member1", memberId);
	}
	
	@Test
	public void testLoadLocalMemberId2() throws IOException, URISyntaxException {
		AppProperties appPropertiesMock = mock(AppProperties.class);
		BaseRepositoryOperations bro = new BaseRepositoryOperations(null, null, appPropertiesMock);
		String memberId = bro.loadLocalMemberId("members-test.properties");
		
		assertEquals("member1", memberId);
	}
	
	@Test
	public void loadRemoteMembers1() throws URISyntaxException, IOException {
		Path pathSys = Paths.get("").resolve("test-repo-root")
									.resolve(".sys")
									.toAbsolutePath();

		if (Files.exists(pathSys)) {
			Files.walk(pathSys)
				 .sorted(Comparator.reverseOrder())
				 .map(Path::toFile)
				 .forEach(File::delete);
			Files.deleteIfExists(pathSys);
		}
		
		Files.createDirectories(pathSys);
				
		Path pathMembers = pathSys.resolve("members.txt");
		try(FileWriter fileWriter = new FileWriter(pathMembers.toFile());
		PrintWriter printWriter = new PrintWriter(fileWriter);){
		printWriter.print("memberId=member1\r\n"
		  	  + "\r\n"
		  	  + "member2:CLOUD:\r\n"
		  	  + "member3:CLOUD:\r\n"
		  	  + "member4:CLOUD:\r\n"
		  	  + "member5:SOURCE:");
		}
		
		AppProperties appPropertiesMock = mock(AppProperties.class);
		BaseRepositoryOperations bro = new BaseRepositoryOperations(null, null, appPropertiesMock);
		List<MemberDescriptor> ds = bro.loadRemoteMembers(pathMembers);
		
		assertAll("memberDescriptors",
				() -> assertEquals(4, ds.size()),
				() -> assertEquals("member2", ds.get(0).getMemberId()),
				() -> assertEquals(MemberType.CLOUD, ds.get(0).getMemberType()),
				() -> assertEquals(null, ds.get(0).getIpAddress()),
				() -> assertEquals("member3", ds.get(1).getMemberId()),
				() -> assertEquals(MemberType.CLOUD, ds.get(1).getMemberType()),
				() -> assertEquals(null, ds.get(1).getIpAddress()),
				() -> assertEquals("member4", ds.get(2).getMemberId()),
				() -> assertEquals(MemberType.CLOUD, ds.get(2).getMemberType()),
				() -> assertEquals(null, ds.get(2).getIpAddress()),
				() -> assertEquals("member5", ds.get(3).getMemberId()),
				() -> assertEquals(MemberType.SOURCE, ds.get(3).getMemberType()),
				() -> assertEquals(null, ds.get(3).getIpAddress()));
	}
	
	@Test
	public void loadRemoteMembers2() throws URISyntaxException, IOException {
		AppProperties appPropertiesMock = mock(AppProperties.class);
		BaseRepositoryOperations bro = new BaseRepositoryOperations(null, null, appPropertiesMock);
		List<MemberDescriptor> ds = bro.loadRemoteMembers("members-test.properties");
		
		assertAll("memberDescriptors",
				() -> assertEquals(4, ds.size()),
				() -> assertEquals("member2", ds.get(0).getMemberId()),
				() -> assertEquals(MemberType.CLOUD, ds.get(0).getMemberType()),
				() -> assertEquals(null, ds.get(0).getIpAddress()),
				() -> assertEquals("member3", ds.get(1).getMemberId()),
				() -> assertEquals(MemberType.CLOUD, ds.get(1).getMemberType()),
				() -> assertEquals(null, ds.get(1).getIpAddress()),
				() -> assertEquals("member4", ds.get(2).getMemberId()),
				() -> assertEquals(MemberType.CLOUD, ds.get(2).getMemberType()),
				() -> assertEquals(null, ds.get(2).getIpAddress()),
				() -> assertEquals("member5", ds.get(3).getMemberId()),
				() -> assertEquals(MemberType.SOURCE, ds.get(3).getMemberType()),
				() -> assertEquals(null, ds.get(3).getIpAddress()));
	}
	
}
