package org.swissbib.solr;


import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

enum ParserType {

    updateParser, deleteParser, notDefined
}



public class IndexerClient
{

    private static final Logger logger = LogManager.getLogger(IndexerClient.class);
    private static Properties appProperties;

    public static void main( String[] args )
    {

        logger.info("Start indexing");

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



}
