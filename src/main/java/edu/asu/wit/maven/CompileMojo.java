package edu.asu.wit.maven;

import java.util.HashMap;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 *
 * @author alwold
 * @goal compile
 */
public class CompileMojo extends JamScrapeMojo {
    /**
     * @parameter
	 * @required
     */
	private String appName;

	public void execute() throws MojoExecutionException, MojoFailureException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("action", "deploy");
		params.put("env", "dev");
		performJamAction(appName, "BuildWarProject", params);
	}
}
