package com.test.sftp;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.scp.client.ScpClient;
import org.apache.sshd.scp.client.ScpClientCreator;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.apache.sshd.sftp.client.fs.SftpPath;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.SshServerFactory;

@RunWith(BlockJUnit4ClassRunner.class)
public class CustomSshdClientTest {

	private static final Logger log = LoggerFactory.getLogger(CustomSshdClientTest.class);
	
	private CustomSshdClient consumer;
	private static SshServer server;
	private Calendar cal= Calendar.getInstance();
    
    @BeforeClass
    public static void setup() throws IOException {
    	
    	Path dummyHome= Paths.get("target/home");
    	if(!Files.exists(dummyHome)) {
    		Files.createDirectories(dummyHome);
    	}
    	
    	File keyFile= new File("target","hostkey");
    	server = SshServerFactory.getInstance(keyFile, "localhost", dummyHome);
    
    	log.info("Staring ssh server");
        server.start();
    }
	
	@AfterClass
    public static void tearDown() throws IOException {
		log.info("Stopping ssh server");
		server.stop();
    }
	
    
	@Before
	public void init() throws InterruptedException{
		
        HostConfigEntry config = new HostConfigEntry();
		config.setHostName("localhost");
		config.setPort(server.getPort());
		config.setUsername("dummyUser");
		
		consumer = new CustomSshdClient();
		consumer.setIdentityPassword("dummyPass");
		consumer.setHostConfigEntry(config);
	}
	
	@Test
	public void testGettingInfo() throws IOException {

		consumer.withRemoteFileSystem((remoteFileSystem) -> {

			ClientSession session = remoteFileSystem.getClientSession();

			// getting local info
			Path home = session.getFactoryManager().getFileSystemFactory().getUserHomeDir(session);
			FileSystem localFileSystem = session.getFactoryManager().getFileSystemFactory().createFileSystem(session);

			log.info("local home:{}, provider:{}", home.toAbsolutePath(), localFileSystem.provider());

			// getting remote info
			SftpPath homeDir = remoteFileSystem.getDefaultDir();
			log.info("remote home:{} \n, provider:{} \n, key:{} \n, address:{} \n, id:{}", homeDir
					, remoteFileSystem.provider()
					, session.getServerKey().getFormat()
					, session.getConnectAddress()
					, remoteFileSystem.getId());

		});

	} 
	
	
	@Test
	public void testCreateDir() throws IOException {
		
		String tempDir= getDummyDir();
		
		consumer.withSftpClient(client ->{
			client.mkdir(tempDir);
			assertTrue(exists(client, tempDir));
		});
	}
	
	@Test
	public void testDeleteDir() throws IOException {
		
		String tempDir= getDummyDir();
		
		consumer.withSftpClient(client ->{
			client.mkdir(tempDir);
			client.rmdir(tempDir);
			assertFalse(exists(client, tempDir));
		});
	}
	
	@Test
	public void testDeleteFile() throws IOException {
		
		String dummyFile="dummy."+cal.getTimeInMillis();
		
		consumer.withSftpClient(client ->{
			
			writeTextFile(client, dummyFile,"dummy");
			client.remove(dummyFile);
			assertFalse(exists(client, dummyFile));
		});
	}
	
	@Test
	public void testCreateFile() throws IOException {
		
		String dummyFile="dummy.txt";
		String content="hello dummy file!";
		log.debug("writting file: {}", dummyFile);
		
		consumer.withSftpClient(client ->{
			writeTextFile(client, dummyFile, content);
			assertTrue(exists(client, dummyFile));
		});
	}

	@Test
	public void testReadFile() throws IOException {
		
		String dummyFile="dummy.txt";
		String content="hello dummy file!";
		
		consumer.withSftpClient(client ->{

			writeTextFile(client, dummyFile,content);
			
			log.debug("reading file: {}", dummyFile);
			try(InputStream is=client.read(dummyFile)){
				
				 String text = new BufferedReader(
					      new InputStreamReader(is, StandardCharsets.UTF_8))
					        .lines()
					        .collect(Collectors.joining("\n"));
				 
				 assertEquals( content, text);
			}
			
		});
	}
	
	@Test
	public void testRenameFile() throws IOException {
		
		String dummyDir= getDummyDir();
		String oldFile=dummyDir+"/fileOne."+cal.getTimeInMillis();
		String newFile=dummyDir+"/fileTwo."+cal.getTimeInMillis();
		
		consumer.withSftpClient(client ->{

			client.mkdir(dummyDir);
			writeTextFile(client, oldFile,"dummy");
						
			log.debug("renaming file: {}  to: {}", oldFile, newFile);
			client.rename(oldFile, newFile);
			assertTrue(exists(client, newFile));
						
		});
	}
	
	@Test
	public void testListDir() throws IOException {

		consumer.withSftpClient((client) -> {

			List<DirEntry> entries = client.readDir(client.openDir("."));

			// List<DirEntry>
			entries.forEach(entry -> { 
				log.debug("{}\t {}", entry.getAttributes().getCreateTime(), entry.getFilename());
			});

			// Iterable<DirEntry>
			client.readDir(".").forEach((entry) -> { 
				log.debug("{}\t {}", entry.getAttributes().getCreateTime(), entry.getFilename());
			});

		});
	}
	
	@Test
	public void testRemoteFileSystemList() throws IOException {

		consumer.withRemoteFileSystem(fileSystem->{
			
			Path remotePath=fileSystem.getPath(".");

			try(DirectoryStream<Path> ds = Files.newDirectoryStream(remotePath)){
				
				for(Path entry: ds) {
					BasicFileAttributes attributes = Files.readAttributes(entry, BasicFileAttributes.class);
			    	log.debug("isDir: {}, {}",attributes.isDirectory(),entry);
				}
			}
			
			assertTrue(remotePath.getNameCount()>0);
			 
		});
		
	}
	
	@Test
	public void testRemoteFileSystemCreateDir() throws IOException {

		consumer.withRemoteFileSystem(fileSystem->{
			
			Path remotePath=fileSystem.getPath("./path/to/test");
			Files.createDirectories(remotePath);
			assertTrue(Files.exists(remotePath));
			
		});
	}
	
	@Test
	public void testRemoteFileSystemCreateFile() throws IOException {

		consumer.withRemoteFileSystem(fileSystem->{
			
			String filename = "./nioFile."+cal.getTimeInMillis();
			Path remotePath=fileSystem.getPath(filename);
			Files.createFile(remotePath);
			
			Files.write(remotePath, "dummy text".getBytes());
						
			assertTrue(Files.exists(remotePath));
			
		});
	}
	
	@Test
	public void testRemoteFileSystemDeleteFile() throws IOException {

		consumer.withRemoteFileSystem(fileSystem->{
			
			String filename = "./nioFile."+cal.getTimeInMillis();
			Path remotePath=fileSystem.getPath(filename);
			Files.createFile(remotePath);
			assertTrue(Files.exists(remotePath));
			
			Files.delete(remotePath);
			assertFalse(Files.exists(remotePath));
			
		});
	}
	
	@Test
	public void testUploadAndDownload() throws IOException {
		
		consumer.withSession(session->{
			
			//upload
	    	ScpClientCreator creator = ScpClientCreator.instance();
	    	ScpClient scpClient = creator.createScpClient(session);
	    	
	    	String uploadedFile= "upload."+cal.getTimeInMillis();
	    	
	    	scpClient.upload(Paths.get(new File("target","hostkey").getAbsolutePath())
	    			, uploadedFile);
	    	
	    	String downloadedFile= "download."+cal.getTimeInMillis();
	    	
	    	Path destinationPath=Paths.get("target", downloadedFile);
	    	scpClient.download(uploadedFile, destinationPath);
			
		});
	}

	
	private boolean exists(SftpClient client, String path) throws IOException {
		try {
			return null != client.stat(path);
		} catch (SftpException e) {
			if (e.getStatus() != SftpConstants.SSH_FX_NO_SUCH_FILE) {
				log.error("Error finding file {}", path);
				throw e;
			}
			return false;
		}
	}
	
	private String getDummyDir() {
		return "dummy"+cal.getTimeInMillis();
	}
	
	private void writeTextFile(SftpClient client, String fileName, String content) throws IOException {
		try(OutputStream os= client.write(fileName)){
			os.write(content.getBytes());
		}
	}

}
