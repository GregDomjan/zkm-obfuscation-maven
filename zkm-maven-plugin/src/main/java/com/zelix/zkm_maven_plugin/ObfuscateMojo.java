package com.zelix.zkm_maven_plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Goal which obfuscates the main artifact file.
 * TODO: code below includes some inputs for newer versions of zkm taken from zkmtask - not fully activated
 * 
 * @goal obfuscate
 * @phase package
 * @requiresProject true
 * @requiresDependencyResolution compile
 */
public class ObfuscateMojo extends AbstractMojo {

	/**
	 * A ZKM Script file
	 * TODO: might want to use all files in zkm... change to include/exclude?
	 * 
	 * @parameter expression="${project.build.directory}/${project.artifactId}-${project.version}.${project.packaging}"
	 * @required
	 */
	private String jarFile;

	/**
	 * A ZKM Script file
	 * TODO: might want to use all files in zkm... change to include/exclude?
	 * 
	 * @parameter expression="${project.basedir}/src/main/zkm/zkm.txt"
	 * @required
	 */
	private String scriptFileName;

	/**
	 * Set the log file name. 
	 * 
	 * @parameter expression="${project.build.directory}/zkm/ZKM_Log.txt"
	 */
	private String logFileName;

	/**
	 * Set the log file name. 
	 * 
	 * @parameter expression="${project.build.directory}/zkm/zkm.txt"
	 */
	private String generatedScriptFileName;
	
	/**
	 * Set the trim log file name. The trim log file name will default to "ZKM_TrimLog.txt".
	 * @ parameter expression="${project.build.directory}/zkm/ZKM_TrimLog.txt"
	 */
//	private String trimLogFileName;
	
	/**
	 * Set the 'default exclusions' file name. The file name can be relative or
	 * absolute. This parameter is optional. The 'default exclusions' file name
	 * will default to "defaultExclude.txt".
	 */
//	private String defaultExcludeFileName;
	
	/**
	 * Set the 'default trim exclusions' file name. The file name can be
	 * relative or absolute. This parameter is optional. The 'default trim
	 * exclusions' file name will default to "defaultTrimExclude.txt".
	 */
//	private String defaultTrimExcludeFileName;
	
	/**
	 * Set the name of the default directory . The directory name can be
	 * relative or absolute. If any of the log file, trim log file, 'default
	 * exclusion' file and the 'default trim exclusions' file names are relative
	 * then they will be relative to this miscellaneous files directory. This
	 * parameter is optional. The directory name will default to that of the
	 * current working directory.
	 */
//	private String defaultDirectoryName;

	/**
	 * Skip execution of this goal
	 * 
	 * @parameter expression="false"
	 */
	private boolean skip = false;

	/**
	 * Verbose?
	 * 
	 * @parameter expression="false"
	 */
	private boolean isVerbose = false;

	/**
	 * Parse Only?
	 * 
	 * @parameter expression="false"
	 */
	private boolean isParseOnly = false;

	/**
	 * @parameter expression="${project}"
	 * @readonly
	 * @required
	 */
	protected MavenProject mavenProject;


	public void execute() throws MojoExecutionException {
		if( skip )
			return;
		
		if (scriptFileName == null || scriptFileName.length() == 0) {
			throw new MojoExecutionException("Missing or empty ZKM Script file name");
		}
        File srcScript = new File(scriptFileName);
        if( ! srcScript.canRead() )
			throw new MojoExecutionException("Missing/Unreadable Script file named " + srcScript.toString() );

		// if (sysproperties != null) {
		// Properties systemProperties = System.getProperties();
		// for (Iterator it=sysproperties.iterator(); it.hasNext(); ) {
		// Sysproperty sysproperty = (Sysproperty)it.next();
		// systemProperties.put(sysproperty.getKey(), sysproperty.getValue());
		// }
		// }

        File genScript = new File(generatedScriptFileName);
        genScript.mkdirs();
        if( genScript.exists() )
        	genScript.delete();
        
        try {
			prepareScriptFile(srcScript, genScript);
		} catch (FileNotFoundException e) {
			throw new MojoExecutionException("Missing Script file named " + scriptFileName, e );
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to generate zkm script " + generatedScriptFileName, e );
		} catch (DependencyResolutionRequiredException e) {
			throw new MojoExecutionException("Plugin failed to find dependencies in project ", e );
		}
        
        File logFile = new File(logFileName);
        logFile.getParentFile().mkdirs();
        
		try {
			
			com.zelix.ZKM.run(generatedScriptFileName, logFileName, isVerbose, isParseOnly );
//			com.zelix.ZKM.run(scriptFileName, logFileName, trimLogFileName,
//					defaultExcludeFileName, defaultTrimExcludeFileName,
//					defaultDirectoryName, isVerbose, isParseOnly,
//					project.getProperties());
		} catch (Exception ex) {
			throw new MojoExecutionException(
					"Error running ZKM obfuscation script", ex);
		}
	}

	protected void prepareScriptFile(File srcScript, File genScript)
			throws FileNotFoundException, IOException, DependencyResolutionRequiredException {
		InputStream in = new FileInputStream(srcScript);

        //For Overwrite the file.
        OutputStream out = new FileOutputStream(genScript);

        String classpath = getRuntimeClasspathElements();
        
        if( classpath != null && !classpath.isEmpty() ){
	        String classpathCmd = "classpath \"" + classpath + "\";\n\n";
	        out.write(classpathCmd.getBytes());
        }
        
        File inJarFile = new File( jarFile );
        
        String jarFileCmd = "open        \""+inJarFile.getCanonicalPath() + "\";\n\n";
        out.write( jarFileCmd.getBytes() );

        
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0){
          out.write(buf, 0, len);
        }
        in.close();

        out.write( "saveAll \"target\\zkm\";\n".getBytes() );

        out.close();
	}

	// nabbed from mavenProject
	public String getRuntimeClasspathElements()
		throws DependencyResolutionRequiredException 
	{
		StringBuilder build = new StringBuilder();
		boolean first = true;
		for (Iterator i = mavenProject.getArtifacts().iterator(); i.hasNext();) {
			Artifact a = (Artifact) i.next();

			if (a.getArtifactHandler().isAddedToClasspath()) {
				// TODO: let the scope handler deal with this
				if (Artifact.SCOPE_COMPILE.equals(a.getScope())
						|| Artifact.SCOPE_RUNTIME.equals(a.getScope())) {
					File file = a.getFile();
					if (file == null) {
						throw new DependencyResolutionRequiredException(a);
					}
					if( !first )
						build.append(File.pathSeparatorChar);
					else 
						first = false;
					build.append(file.getPath());
				}
			}
		}
		return build.toString();
	}
	// ArrayList sysproperties = new ArrayList();
	//
	// public Sysproperty createSysproperty() {
	// Sysproperty sysproperty = new Sysproperty();
	// sysproperties.add(sysproperty);
	// return sysproperty;
	// }
	//
	// public class Sysproperty {
	// private String key;
	// private String value;
	//
	// public void setKey(String key) {
	// this.key = key;
	// }
	//
	// public String getKey() {
	// return key;
	// }
	//
	// public void setValue(String value) {
	// this.value = value;
	// }
	//
	// public String getValue() {
	// return value;
	// }
	// }

}
