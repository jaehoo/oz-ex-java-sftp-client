package com.test.sftp;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class CustomJschClient {
	
	private JSch jsch;
	
	private String host;
	private String user;
	private byte[] pass;
	private int port;
	private File knownHosts;
	private int sessionTimeout;
	private int channelTimeout;
	private Properties config;
	
	
	public void setHost(String host) {
		this.host = host;
	}


	public void setUser(String user) {
		this.user = user;
	}

	public void setPass(byte[] pass) {
		this.pass = pass;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setKnownHosts(File knownHosts) {
		this.knownHosts = knownHosts;
	}

	public void sessionTimeout(int connectTimeout) {
		this.sessionTimeout = connectTimeout;
	}

	public void setConfig(Properties config) {
		this.config = config;
	}

	public void setChannelTimeout(int channelTimeout) {
		this.channelTimeout = channelTimeout;
	}



	interface JschOperation<T>{
		
		public void execute(T operation) throws JSchException, com.jcraft.jsch.SftpException;
		
	}

	public void withSession(JschOperation<Session> operation) throws JSchException, com.jcraft.jsch.SftpException, IOException {
		
		//Setup SFTP Client
        jsch = new JSch();
        
        if(knownHosts!= null && knownHosts.exists()) {
			jsch.setKnownHosts(knownHosts.getAbsolutePath());
		}
		
        Session session = jsch.getSession(user, host, port);
        session.setPassword(pass);
        
        if(config!=null) {
        	session.setConfig(config);
        }
        
        session.connect(sessionTimeout);
        
		operation.execute(session);
		
	}
	
	
	public void withSftpChannel(JschOperation<ChannelSftp> operation) throws JSchException, SftpException, IOException {
		
		withSession(session ->{
			
			Channel channel = session.openChannel("sftp");
			
			ChannelSftp sftpChannel = (ChannelSftp) channel;
			
			if(channelTimeout!=0) {
				channel.connect(channelTimeout);
			}
			
			operation.execute(sftpChannel);
			
			sftpChannel.exit();
			sftpChannel.disconnect();
			
		});
		
		
	}
}
