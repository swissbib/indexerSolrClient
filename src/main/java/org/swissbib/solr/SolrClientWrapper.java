package org.swissbib.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

class SolrClientWrapper {

    private final SolrClient solrClient;
    private final String collection;
    private static final Logger logger = LoggerFactory.getLogger(SolrClientWrapper.class);
    private final String hosts;


    SolrClientWrapper(String[] zkHosts,
                             String zkCHRoot,
                             String collection,
                             Integer connectionTimeout,
                             Integer socketTimeOut) {

        List<String> zk = Arrays.asList(zkHosts);
        StringBuilder sb = new StringBuilder();
        zk.forEach((String host) -> {

            if (sb.toString().equalsIgnoreCase(""))
                sb.append(host);
            else
                sb.append(",").append(host);
        });
        sb.append(zkCHRoot);
        hosts = sb.toString();

        this.solrClient = new CloudSolrClient.Builder(Arrays.asList(zkHosts), Optional.of(zkCHRoot)).
                withConnectionTimeout(connectionTimeout).
                withSocketTimeout(socketTimeOut).
                build();

        this.collection = collection;

    }


    SolrClientWrapper(String serverNodeURL,
                            String collection,
                             Integer connectionTimeout,
                             Integer socketTimeOut) {

        hosts = serverNodeURL;

        this.solrClient = new HttpSolrClient.Builder(serverNodeURL)
                .withConnectionTimeout(connectionTimeout)
                .withSocketTimeout(socketTimeOut)
                .build();

        this.collection = collection;


    }

    void deleteById(ArrayList<String> deleteIds) {

        //String test = solrClient.toString();
        try {
            solrClient.deleteById(this.collection, deleteIds);
        } catch (SolrServerException | IOException ex) {
            logger.error(String.format("error deleting IDS host: %s",solrClient.toString()),ex);
        }

    }


    void add (ArrayList<SolrInputDocument> docs) {

        try {
            solrClient.add(this.collection, docs);
        } catch (SolrServerException | IOException ex) {
            logger.error(String.format("error add documets host: %s",solrClient.toString()),ex);
        }



    }

    void commit () {

        try {
            logger.info(String.format("commit to %s collection %s",this.hosts,this.collection));

            this.solrClient.commit(this.collection);
        } catch (SolrServerException | IOException ex) {
            logger.error("error in commitServer method", ex);
        }
    }


    void close () {

        try {
            this.solrClient.close();
        } catch ( IOException ex) {
            logger.error("error in commitServer method", ex);
        }
    }



}
