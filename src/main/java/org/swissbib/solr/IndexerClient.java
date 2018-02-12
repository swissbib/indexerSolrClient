package org.swissbib.solr;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IndexerClient
{

    private static final Logger logger = LogManager.getLogger(IndexerClient.class);

    public static void main( String[] args )
    {

        logger.info("Start indexing");

        Properties appProperties = getApplicationProperties();

        final SolrClient client = getSolrClient(appProperties);

        ContentStreamUpdateRequest cstrur = new ContentStreamUpdateRequest(appProperties.getProperty("urlStreamUpdateRequest"));

        //todo: refactor this based on Streams

        try {

            File[] basePath = new File(appProperties.getProperty("filesBasePath")).listFiles();
            if (basePath != null) {
                for (File subDir : basePath) {
                    if (subDir.isDirectory()) {
                        File[] contentFiles = subDir.listFiles();
                        if (contentFiles != null) {
                            for (File contentFile : contentFiles) {

                                logger.info("adding contentfile: " + contentFile.getName());
                                cstrur.addFile(contentFile, "text/xml");

                            }
                            try {
                                logger.info("processing directory: " + subDir.getName());
                                cstrur.process(client, appProperties.getProperty("collection"));
                                logger.info("commit files: " + subDir.getName());
                                client.commit(appProperties.getProperty("collection"));
                                logger.info("commit done");
                                //todo do we need to create a new ContentStreamUpdateRequest??
                                cstrur = new ContentStreamUpdateRequest(appProperties.getProperty("urlStreamUpdateRequest"));
                            } catch (SolrServerException | IOException exc) {
                                exc.printStackTrace();
                            }

                        }
                    }
                }
            }


            } catch (IOException ioexc) {
                ioexc.printStackTrace();
            }

        logger.info("indexing finished");


    }

    private static SolrClient getSolrClient(Properties appProperties) {


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

}
