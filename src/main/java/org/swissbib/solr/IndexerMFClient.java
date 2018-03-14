package org.swissbib.solr;

//import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.metafacture.framework.MetafactureException;
import org.metafacture.framework.annotations.Description;
import org.metafacture.framework.annotations.In;
import org.metafacture.framework.annotations.Out;
import org.metafacture.io.ConfigurableObjectWriter;
import org.metafacture.io.FileCompression;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;


@Description("Indexes a customized InputStream to Solr ")
@In(ExtendedInputStreamReader.class)
@Out(Void.class)
public class IndexerMFClient <T> implements ConfigurableObjectWriter<T> {

    // is there another possibility as ConfigurableObjectWriter where we have to implement quite
    //a lot of methods we do not support in this context?

    //todo: should this be static?
    private Properties appProperties;
    private SolrClient client;

    private int processedNumberFiles = 0;
    private int definedNumberOfFilesForCommit = 0;

    private static final Logger logger = LoggerFactory.getLogger(IndexerClient.class);


    public IndexerMFClient(String pathAppProperties) {

        appProperties = getApplicationProperties(pathAppProperties);
        client = getSolrClient();


    }


    @Override
    public String getEncoding() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEncoding(String encoding) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileCompression getCompression() {

        return null;
    }


    @Override
    public void setCompression(FileCompression compression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCompression(String compression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHeader() {

        throw new UnsupportedOperationException();
    }

    @Override
    public void setHeader(String header) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFooter() {

        throw new UnsupportedOperationException();
    }

    @Override
    public void setFooter(String footer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSeparator() {

        throw new UnsupportedOperationException();
    }

    @Override
    public void setSeparator(String separator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void process(T obj) {


        try {
            ExtendedInputStreamReader reader = (ExtendedInputStreamReader) obj;
            processedNumberFiles++;

            ParserType pt = checkParserBasedOnFileName(reader.getFile().getName());
            switch (pt) {

                case deleteParser:

                    final SolrXMLDeleteParser deleteParser = new SolrXMLDeleteParser(reader);
                    logger.info("parsing delete contentfile: " + reader.getFile().getAbsolutePath());
                    deleteParser.setDebugIndexedDocs(checkDebuggingDelete());
                    deleteParser.parseFile();
                    client.deleteById(appProperties.getProperty("collection"),deleteParser.getIds2Delete());

                    break;

                case updateParser:

                    final SolrXMLUpdateParser updateParser = new SolrXMLUpdateParser(reader);
                    logger.info("parsing update contentfile: " + reader.getFile().getAbsolutePath());
                    updateParser.setDebugIndexedDocs(checkDebuggingUpdate());
                    updateParser.parseFile();
                    client.add(appProperties.getProperty("collection"),updateParser.getSolrDocs());

                    break;

                default:

                    break;
            }

            if (definedNumberOfFilesForCommit != 0 && definedNumberOfFilesForCommit > processedNumberFiles) {
                commitServer();
                processedNumberFiles = 0;
            }



        } catch (IOException  | SolrServerException clientException) {

            clientException.printStackTrace();

        } catch (Exception throwable) {

            throwable.printStackTrace();
        }

    }

    @Override
    public void resetStream() {

    }

    @Override
    public void closeStream() {

        //todo: is this a good idea to commit in closeStream event'
        //or something like commit every 'xx' file?
        //or something else?
        //analyse the automatique commit behaviour of SOLR
        commitServer();

    }

    private Properties getApplicationProperties (String appPropPath) {

        //Optional<Properties> props = null;
        Properties configProps = new Properties();

        try {
            File appProperties = new File(appPropPath);
            FileInputStream fi = new FileInputStream(appProperties);
            configProps.load(fi);

        } catch (IOException | NullPointerException loadException ) {
            throw new MetafactureException("couldn't load mandatory property file for Solr Indexer client");
        }

        definedNumberOfFilesForCommit = Integer.getInteger(configProps.getProperty("commitAfterNumberOfFiles", "0"));

        return configProps;


    }

    private SolrClient getSolrClient() {

        final String solrUrl = appProperties.getProperty("solrURL");
        return new HttpSolrClient.Builder(solrUrl)
                .withConnectionTimeout(200000)
                .withSocketTimeout(600000)
                .build();

    }

    private ParserType checkParserBasedOnFileName(String fileName) {

        //todo: very simple check - use configuration for better differentiation
        if (fileName.contains("idsToDelete"))
            return ParserType.deleteParser;
        else
            return ParserType.updateParser;

    }

    private boolean checkDebuggingDelete() {

        String debug  = appProperties.getProperty("debugParsingDelete", "false");
        return Boolean.valueOf(debug);

    }

    private boolean checkDebuggingUpdate() {

        String debug  = appProperties.getProperty("debugParsingUpdate", "false");
        return Boolean.valueOf(debug);

    }

    private void commitServer () {

        try {
            client.commit(appProperties.getProperty("collection"));
            logger.info("now commit against server");
        } catch (SolrServerException  | IOException sE) {
            logger.error("error while commiting", sE);
        }

    }



}
