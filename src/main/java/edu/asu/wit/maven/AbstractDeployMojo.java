package edu.asu.wit.maven;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 *
 * @author alwold
 */
public abstract class AbstractDeployMojo extends AbstractMojo {
    /**
     * @parameter
     */
    private String appDir;

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

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Deploying to "+appDir);
		// copy all of the generated WAR stuff, except WEB-INF/classes
		DirectoryScanner scanner = new DirectoryScanner();
		File webappBase = new File(outputDirectory, warName);
		scanner.setBasedir(webappBase);
		// exclude jetty-env.xml because it might include passwords and it isn't needed
		scanner.setExcludes(new String[] { "WEB-INF/classes/**", "WEB-INF/jetty-env.xml", "WEB-INF" });
		scanner.scan();
		String[] files = scanner.getIncludedFiles();
		boolean foundDevAppProperties = false;
		for (int i = 0; i < files.length; i++) {
			String name = files[i];
			String destination = appDir + "/" + name;

			File source = new File(webappBase, name);

			performCopy(source.getAbsolutePath(), destination);
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

			performCopy(source.getAbsolutePath(), destination);
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

				performCopy(source.getAbsolutePath(), destination);
			}
		}
	}

	public abstract void performCopy(String source, String destination) throws MojoExecutionException;

}
