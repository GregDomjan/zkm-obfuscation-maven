package com.zelix.plugins;

/*-----------------------------------------------------------------------------*
 * Copyright 2015-2015 Zelix Pty Ltd (ACN 078 740 093). All  Rights Reserved.  *
 *                                                                             *
 * Licensed under the GNU General Public License, version 3                    *
 * http://www.gnu.org/licenses/gpl-3.0.txt                                     *
 * The license includes the following conditions.                              *
 *                                                                             *
 *  THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY           *
 * APPLICABLE LAW.  EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT      *
 * HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY   *
 * OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO,    *
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR      *
 * PURPOSE.  THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM  *
 * IS WITH YOU.  SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF    *
 * ALL NECESSARY SERVICING, REPAIR OR CORRECTION.                              *
 *                                                                             *
 * IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING       *
 * WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS   *
 * THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY *
 * GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE    *
 * USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF   *
 * DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD  *
 * PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS),    *
 * EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF   *
 * SUCH DAMAGES.                                                               *
 *                                                                             *
 *-----------------------------------------------------------------------------*/

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.*;
import org.apache.maven.artifact.*;
import java.util.*;
import java.io.*;

/**
 * Allows the Zelix KlassMaster obfuscator to be called as part of a Maven
 * build.
 * 
 */
@Mojo(name = "obfuscate", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ZelixKlassMasterMojo extends AbstractMojo {

	/**
	 * If true then do not obfuscate.
	 */
	@Parameter(property="obfuscate.skip", defaultValue="false")
	private boolean isSkip;

	/**
	 * A ZKM Script file name.
	 */
	@Parameter(property = "obfuscation.scriptFile", defaultValue = "${project.basedir}/src/main/zkm/zkm.txt", required = true)
	private File scriptFile;

	/**
	 * Set the log file name.
	 * <P>
	 * The file name can be relative or absolute. This parameter is optional.
	 */
	@Parameter(property = "obfuscation.logFileName", defaultValue = "${project.build.directory}/zkm/ZKM_Log.txt")
	private String logFileName;

	/**
	 * Set the trim log file name.
	 * <P>
	 * The file name can be relative or absolute. This parameter is optional.
	 */
	@Parameter(property = "obfuscation.trimLogFileName", defaultValue = "${basedir}/ZKM_TrimLog.txt")
	private String trimLogFileName;

	/**
	 * Set the 'default exclusions' file name.
	 * <P>
	 * The file name can be relative or absolute. This parameter is optional.
	 * <P>
	 * By default Zelix KlassMaster will look for a file named
	 * "defaultExclude.txt".
	 */
	@Parameter
	private String defaultExcludeFileName;

	/**
	 * Set the 'default trim exclusions' file name.
	 * <P>
	 * The file name can be relative or absolute. This parameter is optional.
	 * <P>
	 * By default Zelix KlassMaster will look for a file named
	 * "defaultTrimExclude.txt".
	 */
	@Parameter
	private String defaultTrimExcludeFileName;

	/**
	 * Set the name of the default directory.
	 * <P>
	 * The directory name can be relative or absolute. This parameter is
	 * optional.
	 * <P>
	 * If any of the log file, trim log file, 'default exclusion' file and the
	 * 'default trim exclusions' file names are relative then they will be
	 * relative to this miscellaneous files directory.
	 * <P>
	 * The directory name will default to that of the "user.dir" system
	 * property.
	 */
	@Parameter
	private String defaultDirectoryName;

	/**
	 * Should zkm/zkm-plugin have verbose output.
	 */
	@Parameter(property = "obfuscation.isVerbose", defaultValue = "false")
	private boolean isVerbose;

	/**
	 * Should ZKM only validate the scriptFile.
	 * <P>
	 * If true then Zelix KlassMaster will parse the ZKM Script but not execute
	 * it.
	 */
	@Parameter(property = "obfuscation.isParseOnly", defaultValue = "false")
	private boolean isParseOnly;

	/**
	 * Optional. Any values will be used to be a classpath string which will be
	 * stored as the "classpathLibs" property for use in the ZKM Script via a
	 * System Variable.
	 */
	@Parameter
	private File[] classpathLibs;

	/**
	 * The Maven project reference where the plugin is currently being executed.
	 * The default value is populated from maven.
	 * https://maven.apache.org/ref/3.2.3/apidocs/org/apache/maven/project/
	 * MavenProject.html
	 */
	@Parameter(property = "project", readonly = true, required = true)
	protected MavenProject mavenProject;

	/**
	 * The Maven project helper.
	 */
	@Component
	private MavenProjectHelper projectHelper;

	/**
	 * Project values for use in zkm properties.
	 */
	/**
	 */
	@Parameter(property = "project.build.finalName", readonly = true, required = true)
	protected String buildFile;

	/**
	 */
	@Parameter(property = "project.artifactId", readonly = true, required = true)
	protected String projectArtifactId;

	/**
	 */
	@Parameter(property = "project.packaging", readonly = true, required = true)
	protected String projectPackaging;

	/**
	 */
	@Parameter(property = "project.build.outputDirectory", readonly = true, required = true)
	protected String outputDirectory;

	/**
	 */
	@Parameter(property = "project.build.directory", readonly = true, required = true)
	protected String buildDirectory;

	/**
	 */
	@Parameter(property = "project.version", readonly = true, required = true)
	protected String projectVersion;

	/**
	 * pom.basedir mvn2, basedir mvn3, project.basedir mvn3.1
	 */
	@Parameter(property = "basedir", readonly = true, required = true)
	protected String basedir;

	/**
	 * The plugin dependencies.
	 */
	@Parameter(property = "project.artifacts", readonly = true, required = true)
	protected Set<Artifact> pluginArtifacts;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (isSkip) {
			getLog().info("Skipping Zelix KlassMaster obfuscation because isSkip is set to 'true'");
			return;
		}
		if (scriptFile == null || scriptFile.length() == 0) {
			throw new MojoFailureException("ZKM Script file is null");
		}
		if (!scriptFile.exists() || scriptFile.isDirectory()) {
			throw new MojoFailureException("'" + scriptFile.getAbsolutePath() + "' does not exist or is a directory");
		}

		Properties extraProperties = new Properties();
		// add project properties to extraProperties
		for (Map.Entry entry : mavenProject.getProperties().entrySet()) {
			extraProperties.put(entry.getKey(), entry.getValue());
		}
		// add configuration parameters to extraProperties mainly for debugging
		// purposes
		extraProperties.put("isSkip", isSkip);
		if (scriptFile != null)
			extraProperties.put("scriptFile", scriptFile.getAbsolutePath());
		if (logFileName != null)
			extraProperties.put("logFileName", logFileName);
		if (trimLogFileName != null)
			extraProperties.put("trimLogFileName", trimLogFileName);
		if (defaultExcludeFileName != null)
			extraProperties.put("defaultExcludeFileName", defaultExcludeFileName);
		if (defaultTrimExcludeFileName != null)
			extraProperties.put("defaultTrimExcludeFileName", defaultTrimExcludeFileName);
		if (defaultDirectoryName != null)
			extraProperties.put("defaultDirectoryName", defaultDirectoryName);
		extraProperties.put("isVerbose", isVerbose);
		extraProperties.put("isParseOnly", isParseOnly);
		// add project parameters
		extraProperties.put("project.basedir", basedir);
		extraProperties.put("project.build.finalName", buildFile);
		extraProperties.put("project.build.directory", buildDirectory);
		extraProperties.put("project.packaging", projectPackaging);
		extraProperties.put("project.build.outputDirectory", outputDirectory);
		extraProperties.put("project.artifactId", projectArtifactId);
		extraProperties.put("project.version", projectVersion);
		extraProperties.put("project.artifact.fileName", mavenProject.getArtifact().getFile().getAbsolutePath());
		
		StringBuilder libBuilder = new StringBuilder();
		// build a classpath string from classpathLibs
		if (classpathLibs != null && classpathLibs.length > 0) {
			for (File lib : classpathLibs) {
				if (libBuilder.length() > 0) {
					libBuilder.append(File.pathSeparator);
				}
				libBuilder.append(lib.getAbsolutePath());
			}
			extraProperties.put("classpathLibs", libBuilder.toString());
		}
		// build a classpath string from compile artifacts
		StringBuilder compileArtifactBuilder = new StringBuilder();
		for (Object compileArtifact : mavenProject.getCompileArtifacts()) {
			File file = ((Artifact) compileArtifact).getFile();
			if ((file != null) && (file.exists())) {
				if (compileArtifactBuilder.length() > 0) {
					compileArtifactBuilder.append(File.pathSeparator);
				}
				compileArtifactBuilder.append(file.getAbsolutePath());
			}
		}
		extraProperties.put("compileArtifactPath", compileArtifactBuilder.toString());
		// build a classpath string from plugin artifacts
		StringBuilder pluginArtifactBuilder = new StringBuilder();
		Set artifactPathSet = new HashSet();
		for (Iterator i = pluginArtifacts.iterator(); i.hasNext();) {
			String artifactPath = ((Artifact) i.next()).getFile().getAbsolutePath();
			if (artifactPathSet.add(artifactPath)) {
				if (pluginArtifactBuilder.length() > 0) {
					pluginArtifactBuilder.append(File.pathSeparator);
				}
				pluginArtifactBuilder.append(artifactPath);
			}
		}
		extraProperties.put("pluginArtifactPath", pluginArtifactBuilder.toString());

		File logFile = new File(logFileName);
		logFile.getParentFile().mkdirs();

		if (isVerbose) {
			getLog().info("Extra properties");
			for (Map.Entry<?, ?> entry : extraProperties.entrySet()) {
				String key = (String) entry.getKey();
				String value = entry.getValue().toString();
				getLog().info("(" + key + ") \"" + value + "\"");
			}
		}

		try {
			com.zelix.ZKM.run(scriptFile.getAbsolutePath(), logFileName, trimLogFileName, defaultExcludeFileName,
					defaultTrimExcludeFileName, defaultDirectoryName, isVerbose, isParseOnly, extraProperties);
		} catch (Exception ex) {
			getLog().info(ex);
			throw new MojoFailureException(ex.toString());
		}
	}
}
