package edu.asu.wit.maven;

import java.util.HashMap;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 *
 * @author alwold
 * @goal restartjvmqa
 */
public class RestartJvmQaMojo extends JamScrapeMojo {
    /**
     * @parameter
	 * @required
     */
	private String appName;
	/**
	 * @parameter
	 * @required
	 */
	private String appJvm;

	public void execute() throws MojoExecutionException, MojoFailureException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("jvm", appJvm);
		params.put("env", "qa");
		performJamAction(appName, "RestartWarJVM", params);
	}
}
