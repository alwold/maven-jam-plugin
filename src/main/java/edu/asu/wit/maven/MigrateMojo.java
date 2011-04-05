package edu.asu.wit.maven;

import java.util.HashMap;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal migrate
 */
public class MigrateMojo extends JamScrapeMojo {
	/**
	 * @parameter
	 * @required
	 */
	private String appName;

	public void execute() throws MojoExecutionException, MojoFailureException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("env", "dev");
		performJamAction(appName, "MigrateWarProject", params);
		params.put("env", "qa");
		performJamAction(appName, "RsyncWarProject", params);
	}
}
