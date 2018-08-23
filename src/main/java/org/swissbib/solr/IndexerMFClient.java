package org.swissbib.solr;

//import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
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

import javax.swing.text.html.parser.Parser;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.regex.Pattern;


@Description("Indexes a customized InputStream to Solr ")
@In(ExtendedInputStreamReader.class)
@Out(Void.class)
public class IndexerMFClient <T> implements ConfigurableObjectWriter<T> {

    // is there another possibility as ConfigurableObjectWriter where we have to implement quite
    //a lot of methods we do not support in this context?

    //todo: should this be static?
    private Properties appProperties;
    private ArrayList<SolrClientWrapper> solrServerClientList;

    private int processedNumberFiles = 0;
    private int definedNumberOfFilesForCommit = 0;

    private static final Logger logger = LoggerFactory.getLogger(IndexerClient.class);

    private final ArrayList<Pattern> deleteFilePattern;


    public IndexerMFClient(String pathAppProperties) {

        appProperties = getApplicationProperties(pathAppProperties);
        solrServerClientList = getSolrClients();
        deleteFilePattern = getDeleteFilePattern();


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
                        //todo: besseres verständnis von Lambdas im Zusammenspiel mit Exceptions
                        solrServerClientList.forEach((SolrClientWrapper client) -> {
                            //try {
                                client.deleteById(deleteParser.getIds2Delete());
                                //client.deleteById(
                                //        appProperties.getProperty("collection"), deleteParser.getIds2Delete());
                            //} catch (SolrServerException | IOException e) {
                            //    logger.error("Error in Solr client method deleteByid",e);
                            //}
                        });

                    //client.deleteById(appProperties.getProperty("collection"),deleteParser.getIds2Delete());

                    break;

                case updateParser:

                    //final SolrXMLUpdateParser updateParser = new SolrXMLUpdateParser(reader);

                    final SolrStaxXMLUpdateParser updateParser = new SolrStaxXMLUpdateParser(reader);

                    logger.info("parsing update contentfile: " + reader.getFile().getAbsolutePath());
                    updateParser.setDebugIndexedDocs(checkDebuggingUpdate());
                    updateParser.parseFile();
                    //client.add(appProperties.getProperty("collection"),updateParser.getSolrDocs());
                    solrServerClientList.forEach((SolrClientWrapper client) -> {
                        client.add(updateParser.getSolrDocs());
                        //try {
                            //client.add(
                            //        appProperties.getProperty("collection"), updateParser.getSolrDocs());
                        //} catch (SolrServerException | IOException e) {
                        //    logger.error("Error in Solr client method add",e);
                        //}
                    });

                    break;

                case notDefined:
                    break;
                default:

                    break;
            }

            if (definedNumberOfFilesForCommit != 0 && processedNumberFiles > definedNumberOfFilesForCommit) {
                commitServer();
                processedNumberFiles = 0;
            }




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
        this.solrServerClientList.forEach(SolrClientWrapper::close);

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

        definedNumberOfFilesForCommit = Integer.valueOf(configProps.getProperty("commitAfterNumberOfFiles", "0"));
        return configProps;


    }

    private ArrayList<SolrClientWrapper> getSolrClients() {

        final String solrUrlProp = appProperties.getProperty("solrURL");
        final String zkHostProps = appProperties.getProperty("zkHost", "");
        final String zkChRootProps = appProperties.getProperty("zkChRoot", "");
        final int connectionTimeout = Integer.valueOf(appProperties.getProperty("connectionTimeout", "200000"));
        final int socketTimeout = Integer.valueOf(appProperties.getProperty("socketTimeout", "600000"));

        final String[] collections = appProperties.getProperty("collection").split("##");


        final ArrayList<SolrClientWrapper> clientList = new ArrayList<>();

        //in case we don not configure any zkHosts we assume indexing should be done via a single data node
        if (zkHostProps.equalsIgnoreCase("")) {
            String[] solrUrls = solrUrlProp.split("##");

            int i = 0;
            for (String solrURL : solrUrls) {


                clientList.add(new SolrClientWrapper(solrURL,
                        collections[i],
                        connectionTimeout,
                        socketTimeout));

                i++;
            }
            //clientList.add(new HttpSolrClient.Builder(solrUrl)
            //        .withConnectionTimeout(connectionTimeout)
            //        .withSocketTimeout(socketTimeout)
            //        .build());


        } else {

            String[] zkEnsembles = zkHostProps.split("##");
            String[] zkCHRoots = zkChRootProps.split("##");

            int i = 0;
            for (String zkEnsemble : zkEnsembles) {
                //String[] zkHosts = zkEnsemble.split(",");
                //Optional<String> zkCHRoot =  Optional.of(zkCHRoots[i]);

                //clientList.add( new CloudSolrClient.Builder(Arrays.asList(zkHosts),zkCHRoot).
                //        withConnectionTimeout(connectionTimeout).
                //        withSocketTimeout(socketTimeout).
                //        build());

                clientList.add(new SolrClientWrapper(zkEnsemble.split(","),
                        zkCHRoots[i],
                        collections[i],
                        connectionTimeout,
                        socketTimeout
                ));

                i++;
            }

        }


        return clientList;
    }

    private ArrayList<Pattern> getDeleteFilePattern() {

        //ArrayList<String> al = new ArrayList<>(Arrays.asList(appProperties.getProperty("patternDeleteFiles","").split("##")));
        ArrayList<Pattern> pl = new ArrayList<>();
        //return new ArrayList<>(Arrays.asList(appProperties.getProperty("patternDeleteFiles","").split("##")));
        Arrays.asList(appProperties.getProperty("patternDeleteFiles","").split("##")).
                forEach(s -> pl.add(Pattern.compile(s,Pattern.CASE_INSENSITIVE|Pattern.DOTALL)));
        return pl;


    }

    private ParserType checkParserBasedOnFileName(String fileName) {

        ParserType pt = ParserType.updateParser;
        for (Pattern pattern: deleteFilePattern) {
            if (pattern.matcher(fileName).matches())
                pt = ParserType.deleteParser;
        }

        //if (fileName.contains("idsToDelete"))
        //    return ParserType.deleteParser;
        //else
        //    return ParserType.updateParser;

        return pt;
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

        for (SolrClientWrapper client : solrServerClientList) {
            logger.info("now commit against server");
            client.commit();

        }

    }

}
