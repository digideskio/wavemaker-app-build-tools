/**
 * Copyright © 2013 - 2017 WaveMaker, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wavemaker.app.build.maven.plugin.mojo;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import com.wavemaker.app.build.maven.plugin.handler.AppBuildHandler;
import com.wavemaker.app.build.maven.plugin.handler.PageMinFileGenerationHandler;
import com.wavemaker.app.build.maven.plugin.handler.SwaggerDocGenerationHandler;
import com.wavemaker.app.build.maven.plugin.handler.VariableServiceDefGenerationHandler;
import com.wavemaker.commons.WMRuntimeException;
import com.wavemaker.commons.io.Folder;
import com.wavemaker.commons.io.local.LocalFolder;

/**
 * Created by saddhamp on 12/4/16.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class AppBuildMojo extends AbstractMojo {

    public static final String ENCODING = "UTF-8";
    public static final String MAVEN_RESOURCES_PLUGIN = "maven-resources-plugin";
    private static final String NON_FILTERED_FILE_EXTENSIONS = "nonFilteredFileExtensions";

    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Parameter(property = "basedir", required = true, readonly = true)
    private String baseDirectory;
    ;

    @Parameter(name = "pages-directory", defaultValue = "src/main/webapp/pages/")
    private String pagesDirectory;

    @Parameter(name = "services-directory", defaultValue = "services")
    private String servicesDirectory;

    @Parameter(name = "outputDirectory", defaultValue = "target/classes")
    private String outputDirectory;

    @Parameter(defaultValue = "${session}")
    private MavenSession session;

    @Component
    private MavenResourcesFiltering mavenResourcesFiltering;

    private List<AppBuildHandler> appBuildHandlers;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        initializeHandlers();

        for (AppBuildHandler appBuildHandler : appBuildHandlers) {
            appBuildHandler.handle();
        }

        final Build build = project.getBuild();
        final List<String> nonFilteredFileExtensions = getNonFilteredFileExtensions(build.getPlugins());


        MavenResourcesExecution mavenResourcesExecution =
                new MavenResourcesExecution(build.getResources(), new File(outputDirectory), project,
                        ENCODING, build.getFilters(), nonFilteredFileExtensions, session);

        try {
            mavenResourcesFiltering.filterResources(mavenResourcesExecution);
        } catch (MavenFilteringException e) {
            throw new WMRuntimeException("Failed to execute resource filtering ", e);
        }
    }

    private void initializeHandlers() throws MojoFailureException {
        if (appBuildHandlers == null) {
            appBuildHandlers = new ArrayList<AppBuildHandler>();
            Folder rootFolder = new LocalFolder(baseDirectory);

            Folder pagesFolder = rootFolder.getFolder(pagesDirectory);
            if (pagesFolder.exists()) {
                appBuildHandlers.add(new PageMinFileGenerationHandler(pagesFolder));
            }


            Folder servicesFolder = rootFolder.getFolder(servicesDirectory);
            if (servicesFolder.exists()) {
                URL[] runtimeClasspathElements = getRuntimeClasspathElements();
                appBuildHandlers.add(new SwaggerDocGenerationHandler(servicesFolder, runtimeClasspathElements));
                appBuildHandlers.add(new VariableServiceDefGenerationHandler(rootFolder));
            }
        }
    }

    private URL[] getRuntimeClasspathElements() throws MojoFailureException {
        URL[] runtimeUrls = null;
        try {
            List<String> compileClasspathElements = project.getCompileClasspathElements();
            List<String> runtimeClasspathElements = project.getRuntimeClasspathElements();
            Set<String> allClassPathElements = new LinkedHashSet<>(compileClasspathElements.size());
            allClassPathElements.addAll(compileClasspathElements);
            allClassPathElements.addAll(runtimeClasspathElements);
            runtimeUrls = new URL[allClassPathElements.size()];
            int index=0;
            for (String s: allClassPathElements ) {
                runtimeUrls[index++] = new File(s).toURI().toURL();
            }
        } catch (Exception exception) {
            throw new MojoFailureException("Failed resolve project dependencies", exception);
        }

        return runtimeUrls;
    }

    private List<String> getNonFilteredFileExtensions(List<Plugin> plugins) {
        for (Plugin plugin : plugins) {
            if (MAVEN_RESOURCES_PLUGIN.equals(plugin.getArtifactId())) {
                final Object configuration = plugin.getConfiguration();
                if (configuration != null) {
                    Xpp3Dom xpp3Dom = (Xpp3Dom) configuration;
                    final Xpp3Dom nonFilteredFileExtensions = xpp3Dom.getChild(NON_FILTERED_FILE_EXTENSIONS);
                    List<String> nonFilteredFileExtensionsList = new ArrayList<>();
                    final Xpp3Dom[] children = nonFilteredFileExtensions.getChildren();
                    for (Xpp3Dom child : children) {
                        nonFilteredFileExtensionsList.add(child.getValue());
                    }
                    return nonFilteredFileExtensionsList;
                }
            }
        }
        return null;
    }
}

