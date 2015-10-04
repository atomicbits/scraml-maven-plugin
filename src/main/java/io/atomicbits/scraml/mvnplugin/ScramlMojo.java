package io.atomicbits.scraml.mvnplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Created by peter on 4/10/15.
 */
@Mojo(name = "scraml")
public class ScramlMojo extends AbstractMojo {

    /**
     * Scraml file pointer to the RAML specification main file.
     */
    @Parameter(property = "scraml.ramlApi")
    private String ramlApi;

    /**
     * Scraml client source generation output directory.
     */
    @Parameter(property = "scraml.outputDirectory", defaultValue = "target/generated-sources/scraml")
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        


    }

}
