package com.test.sftp;
import java.io.IOException;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.apache.sshd.sftp.client.impl.DefaultSftpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomSshdClient {
	
	private static final Logger log = LoggerFactory.getLogger(CustomSshdClient.class);
	
	private HostConfigEntry hostConfigEntry;
	private String identityPassword;
	
	public void setHostConfigEntry(HostConfigEntry hostConfigEntry) {
		this.hostConfigEntry = hostConfigEntry;
	}

	public void setIdentityPassword(String identityPassword) {
		this.identityPassword = identityPassword;
	}

	interface CustomOperation<T> {
		
		public void execute(T operation) throws IOException;
	}

	
	public void withRemoteFileSystem(CustomOperation<SftpFileSystem> operation) throws IOException {
		
		withSession((session) -> {
			
			try (SftpFileSystem sftpFileSystem = DefaultSftpClientFactory.INSTANCE.createSftpFileSystem(session)) {
			
				operation.execute(sftpFileSystem);
				
			}
		});
		
	}
	
	public void withSession(CustomOperation<ClientSession> operation) throws IOException {

		withSshClient(sshClient -> {
			
			try (ClientSession session = sshClient.connect(hostConfigEntry).verify().getClientSession()) {

				session.addPasswordIdentity(identityPassword);
				AuthFuture authFuture = session.auth().verify();

				log.debug("is client auth success: {}", authFuture.isSuccess());
				log.debug("client connect address: {}", session.getConnectAddress().toString());

				operation.execute(session);
			}

		});

	}
	
	public void withSftpClient(CustomOperation<SftpClient> operation) throws IOException {
		
		withSession((session) -> {
			
			try(SftpClient sftp = DefaultSftpClientFactory.INSTANCE.createSftpClient(session)) {
				operation.execute(sftp);
			} 
			
		});
		
	}
	
	public void withSshClient(CustomOperation<SshClient> operation) throws IOException {
		
		try (SshClient sshClient = SshClient.setUpDefaultClient()) {
			sshClient.start();
			
			operation.execute(sshClient);
		}
	}
	
}
