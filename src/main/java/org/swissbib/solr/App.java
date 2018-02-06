package org.swissbib.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {


        final SolrClient client = getSolrClient();

        //File file = new File("/swissbib_index/solrDocumentProcessing/MarcToSolr/data/outputfilesFrequent/20180206080049_Bulkupdate_2SearchDocs/solrout000004.xml");
        //File file = new File("/swissbib_index/solrDocumentProcessing/MarcToSolr/data/outputfilesFrequent/20180206081038_ZIdsToDelete/idsToDelete.20180206081043.xml");


        ContentStreamUpdateRequest cstrur = new ContentStreamUpdateRequest("/update");
        try {

            Stream<Path> paths = Files.walk(Paths.get("/swissbib_index/solrDocumentProcessing/MarcToSolr/data/outputfilesFrequent/20180206080049_Bulkupdate_2SearchDocs"));
                paths
                        .filter(Files::isRegularFile)
                        .forEach( item -> {

                                try {
                                    cstrur.addFile(item.toFile(), "text/xml");

                                } catch (IOException exception) {
                                    exception.printStackTrace();
                                }
                            }


                        );



            cstrur.process(client, "green");
            client.commit("green");

            System.out.println();

        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (SolrServerException serverException) {
            serverException.printStackTrace();
        }






    }


    public static SolrClient getSolrClient() {



        final String solrUrl = "http://localhost:8080/solr";
        return new HttpSolrClient.Builder(solrUrl)
                .withConnectionTimeout(200000)
                .withSocketTimeout(600000)
                .build();

    }
}
