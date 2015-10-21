/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License for more details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.mvnplugin;

import io.atomicbits.scraml.generator.ScramlGenerator;
import io.atomicbits.scraml.mvnplugin.util.ListUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by peter on 4/10/15.
 */
@Mojo(name = "scraml")
public class ScramlMojo extends AbstractMojo {

    @Parameter(defaultValue="${project}", readonly=true, required=true)
    private MavenProject project;

    /**
     * Scraml file pointer to the RAML specification main file.
     */
    @Parameter(property = "scraml.ramlApi", defaultValue = "")
    private String ramlApi;

    /**
     * Scraml base directory to find the RAML files.
     */
    @Parameter(property = "scraml.resourceDirectory", defaultValue = "src/main/resources")
    private String resourceDirectory;

    /**
     * Scraml client source generation output directory.
     */
    @Parameter(property = "scraml.outputDirectory", defaultValue = "target/generated-sources/scraml")
    private String outputDirectory;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (!ramlApi.isEmpty()) {

            File ramlBaseDir;
            File ramlSource;
            if(resourceDirectory.startsWith("/")) {
                ramlBaseDir = new File(resourceDirectory);
                ramlSource = new File(ramlBaseDir, ramlApi);
            } else {
                ramlBaseDir = new File(project.getBasedir(), resourceDirectory);
                ramlSource = new File(ramlBaseDir, ramlApi);
            }

            String[] apiPackageAndClass = packageAndClassFromRamlPointer(ramlApi);
            String apiPackageName = apiPackageAndClass[0];
            String apiClassName = apiPackageAndClass[1];

            Map<String, String> generatedFiles;
            try {
                generatedFiles = ScramlGenerator.generateJavaCode(ramlSource.toURI().toURL().toString(), apiPackageName, apiClassName);
            } catch (MalformedURLException | NullPointerException e) {
                feedbackOnException(ramlBaseDir, ramlApi, ramlSource);
                throw new RuntimeException("Could not generate RAML client.", e);
            }

            File outputDirAsFile;
            if(outputDirectory.startsWith("/")) {
                outputDirAsFile = new File(outputDirectory);
            } else {
                outputDirAsFile = new File(project.getBasedir(), outputDirectory);
            }
            outputDirAsFile.mkdirs();

            try {
                for (Map.Entry<String, String> entry : generatedFiles.entrySet()) {
                    String filePath = entry.getKey();
                    String content = entry.getValue();
                    File fileInDst = new File(outputDirAsFile, filePath);
                    fileInDst.getParentFile().mkdirs();
                    FileWriter writer = new FileWriter(fileInDst);
                    writer.write(content);
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Could not generate RAML client.", e);
            }

            project.addCompileSourceRoot(outputDirectory);
        }

    }


    private String escape(char ch) {
        return "\\Q" + ch + "\\E";
    }


    private String[] packageAndClassFromRamlPointer(String pointer) {
        String[] parts = pointer.split(escape('/'));
        if (parts.length == 1) {
            return new String[]{"io.atomicbits", cleanFileName(parts[0])};
        } else {
            String className = cleanFileName(parts[parts.length - 1]);
            List<String> firstParts = Arrays.asList(parts).subList(0, parts.length - 1); // toIndex is exclusive
            String packageParts = ListUtils.mkString(firstParts, ".");
            return new String[]{packageParts, className};
        }
    }


    private String cleanFileName(String fileName) {
        String[] nameSplit = fileName.split(escape('.'));
        String withOutExtension;
        if (nameSplit.length == 0) {
            withOutExtension = fileName;
        } else {
            withOutExtension = nameSplit[0];
        }

        // capitalize after special characters and drop those characters along the way
        List<Character> dropChars = Arrays.asList('-', '_', '+', ' ');
        String cleanedDropChars = withOutExtension;
        for (Character dropChar : dropChars) {
            List<String> items = removeEmpty(Arrays.asList(cleanedDropChars.split(escape(dropChar))));
            List<String> capitalized = new ArrayList<>();
            for (String item : items) {
                capitalized.add((capitalize(item)));
            }
            cleanedDropChars = ListUtils.mkString(capitalized, "");
        }

        // capitalize after numbers 0 to 9, but keep the numbers
        List<Character> numbers = Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9');
        // Make sure we don't drop the occurrences of numbers at the end by adding a space and removing it later.
        String cleanedNumbers = cleanedDropChars + " ";
        for (Character number : numbers) {
            List<String> items = Arrays.asList(cleanedNumbers.split(escape(number))); // it's important NOT to remove the empty strings here
            List<String> capitalized = new ArrayList<>();
            for (String item : items) {
                capitalized.add((capitalize(item)));
            }
            cleanedNumbers = ListUtils.mkString(capitalized, number.toString());
        }

        // final cleanup of all strange characters
        return cleanedNumbers.replaceAll("[^A-Za-z0-9]", "").trim();
    }


    private String capitalize(String dirtyName) {
        char[] chars = dirtyName.toCharArray();
        if (chars.length > 0) {
            chars[0] = Character.toUpperCase(chars[0]);
        }
        return new String(chars);
    }


    private List<String> removeEmpty(List<String> items) {
        List<String> emptied = new ArrayList<>();
        for (String item : items) {
            if (!item.isEmpty()) {
                emptied.add(item);
            }
        }
        return emptied;
    }


    private void feedbackOnException(File ramlBaseDir,
                                     String ramlPointer,
                                     File ramlSource) {
        System.out.println(
                "Exception during RAMl parsing, possibly caused by a wrong RAML path.\n" +
                        "Are you sure the following values are correct (non-null)?\n\n" +
                        "- - - - - - - - - - - - - - - - - - - - - - -\n" +
                        "RAML base path: " + ramlBaseDir + "\n" +
                        "RAML relative path: " + ramlPointer + "\n" +
                        "RAML absolute path" + ramlSource + "\n" +
                        "- - - - - - - - - - - - - - - - - - - - - - -\n\n" +
                        "In case the relative path is wrong or null, check your project settings and" +
                        "make sure the 'scramlRamlApi in scraml in Compile' value points to the main" +
                        "raml file in your project's (or module's) resources directory."
        );
    }

}
