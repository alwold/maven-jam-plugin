package edu.asu.wit.maven;

import java.util.HashMap;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 *
 * @author alwold
 * @goal compileqa
 */
public class CompileQaMojo extends JamScrapeMojo {
    /**
     * @parameter
	 * @required
     */
	private String appName;

	public void execute() throws MojoExecutionException, MojoFailureException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("action", "deploy");
		params.put("env", "qa");
		performJamAction(appName, "BuildWarProject", params);
	}
}
