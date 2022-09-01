package com.test.sftp;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Properties;
import java.util.Vector;

import org.apache.sshd.server.SshServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import util.SshServerFactory;

@RunWith(BlockJUnit4ClassRunner.class)
public class CustomJschClientTest {

	private static final Logger log = LoggerFactory.getLogger(CustomJschClientTest.class);

	private static SshServer server;
	private CustomJschClient client;
	private String host = "localhost";
	private String user = "dummyuser";
	private String pass = "dummypass";
	private Calendar cal = Calendar.getInstance();

	@BeforeClass
	public static void setup() throws IOException {

		Path dummyHome = Paths.get("target/home");
		if (!Files.exists(dummyHome)) {
			Files.createDirectories(dummyHome);
		}

		File keyFile = new File("target/home", "hostkey");
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
	public void init() throws InterruptedException {

		client = new CustomJschClient();
		client.setHost(host);
		client.setPort(server.getPort());
		client.setUser(user);
		client.setPass(pass.getBytes());

		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no"); // yes to check host key, taken from known_hosts
		client.setConfig(config);
		client.sessionTimeout(300000);
		client.setChannelTimeout(10000);

	}

	@Test
	public void getServerInfo() throws JSchException, SftpException, IOException {
		client.withSession(session -> {

			log.info(session.getServerVersion());
			log.info("{}", session.getHostKey());
			log.info("{}", session.getUserName());

		});
	}

	@Test(expected = Test.None.class)
	public void testListFiles() throws JSchException, SftpException, IOException {

		client.withSftpChannel(channel -> {

			String pwd = channel.lpwd();
			log.info(pwd);

			@SuppressWarnings("unchecked")
			Vector<ChannelSftp.LsEntry> files = channel.ls(".");
			files.forEach(file -> log.info("{}", file));

		});

	}

	@Test(expected = Test.None.class)
	public void testCreateDir() throws JSchException, SftpException, IOException {

		client.withSftpChannel(sftp -> {

			String dummyDir = getDummyDir();
			sftp.mkdir(dummyDir);

			String pwd = sftp.lpwd();
			log.info(pwd);

			sftp.lstat(dummyDir);

		});

	}

	@Test
	public void testUploadAndDownloadFile() throws JSchException, SftpException, IOException {

		// Create dummy dir
		String dummyDir = "target/fileSet";
		Files.createDirectories(Paths.get(dummyDir));

		// Creating dummy File
		String dummyFile = "initialFile." + cal.getTimeInMillis();
		Path path = Paths.get(dummyDir, dummyFile);
		Files.createFile(path);
		Files.write(path, "dummy content".getBytes());

		String newfileName = "uploadedFile." + cal.getTimeInMillis();

		client.withSftpChannel(sftp -> {

			// Upload file
			FileInputStream fileToUpload;

			try {
				fileToUpload = new FileInputStream(path.toFile());
				sftp.put(fileToUpload, newfileName);
			} catch (FileNotFoundException e) {
				log.info("File not found:{}", dummyFile);
			}

			// Download file
			String downloadedFile = dummyDir + "/downloadedFile." + cal.getTimeInMillis();
			sftp.get(newfileName, downloadedFile);
			assertTrue(Files.exists(Paths.get(downloadedFile)));
		});

	}

	private String getDummyDir() {
		return "dummy" + cal.getTimeInMillis();
	}
}
