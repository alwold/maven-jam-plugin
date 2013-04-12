package edu.asu.wit.maven;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;

/**
 * @goal deploy
 */
public class DeployMojo extends AbstractDeployMojo {
	@Override
	public void performCopy(String source, String destination) throws MojoExecutionException {
		File destinationFile = new File(destination);
		if (!destinationFile.getParentFile().exists()) {
			destinationFile.getParentFile().mkdirs();
		}

		try {
			getLog().info("copying "+source+" ->");
			getLog().info(destinationFile.getAbsolutePath());
			FileUtils.copyFile(new File(source), destinationFile);
		} catch (IOException e) {
			throw new MojoExecutionException("Error copying webapp file " + source, e);
		}
	}

}
