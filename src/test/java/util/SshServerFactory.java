package util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.StaticPasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

public class SshServerFactory {
	
	private SshServerFactory(){
	}
	
	public static SshServer getInstance(File keyFile, String host, Path path) {
		return build(keyFile,0, host, path);
	}
	
	public static SshServer getInstance(File keyFile, int port, String host, Path path) {
		return build(keyFile, port, host, path);
	}
	
	private static SshServer build(File keyFile, int port, String host, Path path) {
		
		SshServer sshd = SshServer.setUpDefaultServer();
		
        SimpleGeneratorHostKeyProvider hostKeyProvider = new SimpleGeneratorHostKeyProvider(keyFile.toPath());
        hostKeyProvider.setKeySize(2048);
        hostKeyProvider.setAlgorithm("RSA");
        
        sshd.setKeyPairProvider(hostKeyProvider);
        //sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshd.setPasswordAuthenticator(new StaticPasswordAuthenticator(true));
        sshd.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
        //sshd.setUserAuthFactories(Collections.singletonList(UserAuthPasswordFactory.INSTANCE));
        
        sshd.setCommandFactory(new ScpCommandFactory());
        
        
        //Setup Virtual File System (VFS) to specific path
        sshd.setFileSystemFactory(new VirtualFileSystemFactory(
        		Paths.get(path.toFile().getAbsolutePath())
		));
        //sshd.setFileSystemFactory(new NativeFileSystemFactory());
        
        //Add SFTP support
        sshd.setSubsystemFactories(Collections.singletonList(
    			new SftpSubsystemFactory.Builder().build()
		));
	    //sshd.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        
        sshd.setHost(host);
        
        if (port!=0) { 
        	sshd.setPort(port);
    	}
        
        return sshd;
	}


	public static SshServer defaultInstance() {
		SshServer sshd = SshServer.setUpDefaultServer();
	    sshd.setPort(22);
	    sshd.setHost("localhost");
	    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
	    sshd.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
	    sshd.setPasswordAuthenticator(new StaticPasswordAuthenticator(true));
	    
	    //Thread.currentThread().sleep(36000l);
	    return sshd;
	    
	}
	
	
}
