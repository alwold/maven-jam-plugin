package edu.asu.wit.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 *
 * @author alwold
 * @goal clean
 */
public class CleanMojo extends AbstractMojo {
    /**
     * @parameter
     */
    private String appDir;

    public void execute() throws MojoExecutionException, MojoFailureException {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(appDir);
        scanner.setExcludes(new String[]{"build.xml", "WEB-INF/build.xml", "asuthemes/**", "WEB-INF"});
        scanner.scan();
        String[] filesToDelete = scanner.getIncludedFiles();
        getLog().info("Deleting "+filesToDelete.length+" files...");
        for (int i = 0; i < filesToDelete.length; i++) {
            File file = new File(appDir, filesToDelete[i]);
            if (!file.delete()) {
                throw new MojoFailureException("Unable to delete "+file.getAbsolutePath());
            }
        }
		// delete them by greatest number of subdirs first
        String[] directoriesToDelete = scanner.getIncludedDirectories();
		Map<Integer, List<String>> directories = new HashMap<Integer, List<String>>();
		for (String directory: directoriesToDelete) {
			if (!directory.equals("")) {
				int slashCount = 0;
				for (int i = 0; i < directory.length(); i++) {
					if (directory.charAt(i) == '/') {
						slashCount++;
					}
				}
				if (directories.get(slashCount+1) == null) {
					directories.put(slashCount+1, new ArrayList<String>());
				}
				directories.get(slashCount+1).add(directory);
			}
		}
        getLog().info("Deleting "+directoriesToDelete.length+" directories...");
		ArrayList<Integer> depths = new ArrayList<Integer>(directories.keySet());
		Collections.sort(depths);
		for (int i = depths.size()-1; i >= 0; i--) {
			for (String directory: directories.get(depths.get(i))) {
				File file = new File(appDir, directory);
				if (!file.delete()) {
					getLog().error("Failed to delete "+directory);
				}
			}
		}
    }

}
