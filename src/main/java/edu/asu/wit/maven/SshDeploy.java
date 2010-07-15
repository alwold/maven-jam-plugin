package edu.asu.wit.maven;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * @goal sshdeploy
 * @author alwold
 */
public class SshDeploy extends AbstractMojo {
    /**
     * @parameter
     */
    private String appDir;

	/**
	 * @parameter
	 * @required
	 */
	private String sshHost;

    /**
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String outputDirectory;

    /**
     * The name of the generated WAR.
     *
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    private String warName;

    /**
     * @parameter expression="${project.compileSourceRoots}"
     * @required
     */
    private List compileSourceRoots;

	private String sshUsername = "";
	private String sshPassword = "";

	public void execute() throws MojoExecutionException, MojoFailureException {
		JSch jsch = new JSch();
		try {
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
			ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
			sftp.connect();
			getLog().info("Deploying to "+appDir);
			// copy all of the generated WAR stuff, except WEB-INF/classes
			DirectoryScanner scanner = new DirectoryScanner();
			File webappBase = new File(outputDirectory, warName);
			scanner.setBasedir(webappBase);
			// exclude jetty-env.xml because it might include passwords and it isn't needed
			// TODO is this working on WEB-INF/classes?
			scanner.setExcludes(new String[] { "WEB-INF/classes", "WEB-INF/jetty-env.xml", "WEB-INF" });
			scanner.scan();
			String[] files = scanner.getIncludedFiles();
			boolean foundDevAppProperties = false;
			for (int i = 0; i < files.length; i++) {
				String name = files[i];
				String destination = appDir + "/" + name;

				File source = new File(webappBase, name);
				File destinationFile = new File(destination);

				try {
					SftpATTRS attrs = sftp.stat(destinationFile.getParent());
					if (!attrs.isDir()) {
						sftp.mkdir(destinationFile.getParent());
					}

					getLog().info("copying "+source.getAbsolutePath()+" ->");
					getLog().info(destinationFile.getAbsolutePath());
					sftp.put(source.getAbsolutePath(), destination);
				} catch (SftpException e) {
					throw new MojoExecutionException("Error copying webapp file " + source, e);
				}
			}
			// copy everything from WEB-INF/classes that isn't a class file to WEB-INF/src
			scanner = new DirectoryScanner();
			File base = new File(webappBase, "WEB-INF");
			base = new File(base, "classes");
			scanner.setBasedir(base);
			scanner.setExcludes(new String[]{"**/*.class"});
			scanner.scan();
			files = scanner.getIncludedFiles();
			for (int i = 0; i < files.length; i++) {
				String name = files[i];
				String destination = appDir + "/WEB-INF/src/" + name;

				getLog().info(name);
				if (foundDevAppProperties && name.equals("application.properties")) {
					// if we already found a specific application.properties for dev, skip this
					continue;
				} else if (name.equals("application.properties.dev")) {
					destination = appDir + "/WEB-INF/src/application.properties";
					foundDevAppProperties = true;
				}

				File source = new File(base, name);
				File destinationFile = new File(destination);

				try {
					SftpATTRS attrs = sftp.stat(destinationFile.getParent());
					if (!attrs.isDir()) {
						sftp.mkdir(destinationFile.getParent());
					}

					getLog().info("copying "+source.getAbsolutePath()+" to "+destinationFile.getAbsolutePath());
					sftp.put(source.getAbsolutePath(), destination);
				} catch (SftpException e) {
					throw new MojoExecutionException("Error copying webapp file " + source, e);
				}
			}
			// now get the source code
			for (Iterator i = compileSourceRoots.iterator(); i.hasNext(); ) {
				String root = (String)i.next();
				scanner = new DirectoryScanner();
				scanner.setBasedir(root);
				scanner.setIncludes(new String[]{"**/*.java"});
				scanner.scan();
				files = scanner.getIncludedFiles();
				for (int j = 0; j < files.length; j++) {
					String name = files[j];
					String destination = appDir + "/WEB-INF/src/" + name;

					File source = new File(root, name);
					File destinationFile = new File(destination);

					if (!destinationFile.getParentFile().exists()) {
						destinationFile.getParentFile().mkdirs();
					}

					try {
						getLog().info("copying "+source.getAbsolutePath()+" ->");
						getLog().info(destinationFile.getAbsolutePath());
						sftp.put(source.getAbsolutePath(), destination);
					} catch (SftpException e) {
						throw new MojoExecutionException("Error copying webapp file " + source, e);
					}
				}
			}
		} catch (JSchException e) {
			throw new MojoExecutionException("Unable to start SSH session", e);
		}
	}

}
