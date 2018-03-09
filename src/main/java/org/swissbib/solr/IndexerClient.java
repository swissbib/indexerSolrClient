package org.swissbib.solr;


import org.antlr.runtime.RecognitionException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.metafacture.commons.ResourceUtil;
import org.metafacture.flux.FluxCompiler;
import org.metafacture.flux.parser.FluxProgramm;
import org.metafacture.runner.util.DirectoryClassLoader;

import static org.apache.logging.log4j.core.util.Loader.getClassLoader;
import static org.metafacture.runner.Flux.PLUGINS_DIR_PROPERTY;
import static org.metafacture.runner.Flux.PROVIDED_DIR_PROPERTY;

enum ParserType {

    updateParser, deleteParser, notDefined
}



public class IndexerClient
{

    private static final Logger logger = LogManager.getLogger(IndexerClient.class);
    private static Properties appProperties;
//    private static final Pattern VAR_PATTERN = Pattern.compile("([^=]*)=(.*)");
//    private static final String SCRIPT_HOME = "FLUX_DIR";


    public static void main( String[] args )
    {

        logger.info("Start indexing");

/*
        loadCustomJars();
        final File fluxFile = new File(args[0]);
        if (!fluxFile.exists()) {
            System.err.println("File not found: " + args[0]);
            System.exit(1);
            return;
        }

        final Map<String, String> vars = new HashMap<String, String>();
        vars.put(SCRIPT_HOME, fluxFile.getAbsoluteFile().getParent()
                + System.getProperty("file.separator"));

        for (int i = 1; i < args.length; ++i) {
            final Matcher matcher = VAR_PATTERN.matcher(args[i]);
            if (!matcher.find()) {
                FluxProgramm.printHelp(System.err);
                return;
            }
            vars.put(matcher.group(1), matcher.group(2));
        }


        try {
            FluxCompiler.compile(ResourceUtil.getStream(fluxFile), vars).start();
        } catch ( IOException  |RecognitionException aE) {
            aE.printStackTrace();
            return;
        }
*/


        appProperties = getApplicationProperties();

        final SolrClient client = getSolrClient();





        //todo: Refactor this based on Java 8 Streams
        File[] subDirsbasePath = new File(appProperties.getProperty("filesBasePath")).listFiles();
        if (! Objects.isNull(subDirsbasePath)) {
            Arrays.sort(subDirsbasePath);
            for (File subDir : subDirsbasePath) {
                if (subDir.isDirectory()) {
                    File[] contentFiles = subDir.listFiles();

                    if (!Objects.isNull(contentFiles)) {
                        Arrays.sort(contentFiles);
                        for (File contentFile : contentFiles) {

                            try {

                                ParserType pt = checkParserBasedOnFileName(contentFile.getName());
                                switch (pt) {

                                    case deleteParser:

                                        final SolrXMLDeleteParser deleteParser = new SolrXMLDeleteParser(contentFile);
                                        logger.info("parsing delete contentfile: " + contentFile.getName());
                                        deleteParser.setDebugIndexedDocs(checkDebuggingDelete());
                                        deleteParser.parseFile();
                                        client.deleteById(appProperties.getProperty("collection"),deleteParser.getIds2Delete());

                                        break;

                                    case updateParser:

                                        final SolrXMLUpdateParser updateParser = new SolrXMLUpdateParser(contentFile);
                                        logger.info("parsing update contentfile: " + contentFile.getName());
                                        updateParser.setDebugIndexedDocs(checkDebuggingUpdate());
                                        updateParser.parseFile();
                                        client.add(appProperties.getProperty("collection"),updateParser.getSolrDocs());

                                        break;

                                    default:
                                        logger.error("no parser-type for filename: " + contentFile.getName());
                                        break;
                                }


                            } catch (IOException  | SolrServerException clientException) {

                                logger.error("error parsing file: " + subDir.getAbsolutePath() +  contentFile.getName());
                                for (StackTraceElement ste :  clientException.getStackTrace()) {
                                    logger.error( ste);
                                }


                            } catch (Exception throwable) {

                                logger.error("error parsing file: " + subDir.getAbsolutePath() +  contentFile.getName());
                                for (StackTraceElement ste :  throwable.getStackTrace()) {
                                    logger.error( ste);
                                }
                            }

                        }
                        try {
                            logger.info("commit files: " + subDir.getName());
                            client.commit(appProperties.getProperty("collection"));
                            logger.info("commit done");
                        } catch (SolrServerException | IOException exc) {
                            for (StackTraceElement ste :  exc.getStackTrace()) {
                                logger.error( ste);
                            }
                        }

                    }
                }
            }
        }



        logger.info("indexing finished");


    }

    private static SolrClient getSolrClient() {


        final String solrUrl = appProperties.getProperty("solrURL");
        return new HttpSolrClient.Builder(solrUrl)
                .withConnectionTimeout(200000)
                .withSocketTimeout(600000)
                .build();

    }

    private static Properties getApplicationProperties () {

        //Optional<Properties> props = null;
        Properties configProps = new Properties();

        try {
            File appProperties = new File(System.getProperty("app.properties", "app.properties"));
            FileInputStream fi = new FileInputStream(appProperties);
            configProps.load(fi);

        } catch (IOException | NullPointerException loadException ) {
            loadException.printStackTrace();
        }

        return configProps;

    }


    private static ParserType checkParserBasedOnFileName(String fileName) {

        //todo: very simple check - use configuration for better differentiation
        if (fileName.contains("idsToDelete"))
            return ParserType.deleteParser;
        else
            return ParserType.updateParser;

    }

    private static boolean checkDebuggingDelete() {

       String debug  =appProperties.getProperty("debugParsingDelete", "false");
       return Boolean.valueOf(debug);

    }

    private static boolean checkDebuggingUpdate() {

        String debug  =appProperties.getProperty("debugParsingUpdate", "false");
        return Boolean.valueOf(debug);

    }

/*
    private static void loadCustomJars() {
        final DirectoryClassLoader dirClassLoader = new DirectoryClassLoader(getClassLoader());

        final String pluginsDir = System.getProperty(PLUGINS_DIR_PROPERTY);
        if (pluginsDir != null) {
            dirClassLoader.addDirectory(new File(pluginsDir));
        }
        final String providedDir = System.getProperty(PROVIDED_DIR_PROPERTY);
        if (providedDir != null) {
            dirClassLoader.addDirectory(new File(providedDir));
        }
        Thread.currentThread().setContextClassLoader(dirClassLoader);

    }
*/


}
