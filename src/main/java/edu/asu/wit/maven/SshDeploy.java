package edu.asu.wit.maven;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal sshdeploy
 * @author alwold
 */
public class SshDeploy extends AbstractDeployMojo {

	/**
	 * @parameter
	 * @required
	 */
	private String sshHost;

	private ChannelSftp sftp;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			getLog().info("Please enter the host you will be using:");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			sshHost = br.readLine();
			getLog().info("Please enter your username:");
			final String sshUsername = br.readLine();
			getLog().info("Please enter your password:");
			final String sshPassword = br.readLine();
			JSch jsch = new JSch();
			Session session = jsch.getSession(sshUsername, sshHost, 22);
			session.setUserInfo(new UserInfo() {

				public String getPassphrase() {
					return sshPassword;
				}

				public String getPassword() {
					return sshPassword;
				}

				public boolean promptPassword(String message) {
					return true;
				}

				public boolean promptPassphrase(String message) {
					return true;
				}

				public boolean promptYesNo(String message) {
					return true;
				}

				public void showMessage(String message) {
				}
			});
			session.connect();
			sftp = (ChannelSftp) session.openChannel("sftp");
			sftp.connect();
			super.execute();
		} catch (JSchException e) {
			throw new MojoExecutionException("Unable to start SSH session", e);
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to read credentials", e);
		}
	}

	@Override
	public void performCopy(String source, String destination) throws MojoExecutionException {
		try {
			if (File.separatorChar == '\\') {
				// yuck, windows. fix the path with this ghetto hack
				destination = destination.replaceAll("\\\\", "/");
			}
			
			String parent = destination.substring(0, destination.lastIndexOf("/"));
			ensureDirectoryExists(parent);

			getLog().info("copying "+source+" ->");
			getLog().info(destination);
			sftp.put(source, destination);
		} catch (SftpException e) {
			throw new MojoExecutionException("Error copying webapp file " + source, e);
		}
	}

	private void ensureDirectoryExists(String dir) throws SftpException {
		SftpATTRS attrs = null;
		try {
			attrs = sftp.stat(dir);
		} catch (SftpException e) {}
		if (attrs == null) {
			if (dir.lastIndexOf("/") != 0) {
				ensureDirectoryExists(dir.substring(0, dir.lastIndexOf("/")));
			}
			getLog().info("creating "+dir);
			sftp.mkdir(dir);
		}
	}

}
