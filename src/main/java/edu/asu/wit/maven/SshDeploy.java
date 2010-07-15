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
 * @execute phase="install"
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
			getLog().info("Please enter your username:");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
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
	public void performCopy(String source, File destinationFile) throws MojoExecutionException {
		try {
			ensureDirectoryExists(destinationFile.getParentFile());

			getLog().info("copying "+source+" ->");
			getLog().info(destinationFile.getAbsolutePath());
			sftp.put(source, destinationFile.getAbsolutePath());
		} catch (SftpException e) {
			throw new MojoExecutionException("Error copying webapp file " + source, e);
		}
	}

	private void ensureDirectoryExists(File dir) throws SftpException {
		SftpATTRS attrs = null;
		try {
			attrs = sftp.stat(dir.getAbsolutePath());
		} catch (SftpException e) {}
		if (attrs == null) {
			if (dir.getParentFile() != null) {
				ensureDirectoryExists(dir.getParentFile());
			}
			getLog().info("creating "+dir.getAbsolutePath());
			sftp.mkdir(dir.getAbsolutePath());
		}
	}

}
