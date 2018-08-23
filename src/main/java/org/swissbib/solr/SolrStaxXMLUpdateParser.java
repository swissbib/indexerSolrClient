package org.swissbib.solr;


import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

import java.io.InputStreamReader;
import java.util.ArrayList;


public class SolrStaxXMLUpdateParser {


    private InputStreamReader reader;

    private boolean debugIndexedDocs = false;

    private int numberParsedRecords = 0;

    private ArrayList<SolrInputDocument> solrDocs = new ArrayList<>();

    private static final Logger staxParserLogger = LoggerFactory.getLogger(SolrStreamFilter.class);





    public SolrStaxXMLUpdateParser(InputStreamReader reader) {

        this.reader = reader;
    }

    public void parseFile() {


        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();

        try {

            SolrStreamFilter dF = new SolrStreamFilter();

            XMLStreamReader p = inputFactory.createFilteredReader(inputFactory.createXMLStreamReader
                    (reader), dF);

            //todo read more about Stax StreamParser
            while(p.hasNext() )
            {
                try {
                    p.next();
                } catch (Exception ex){
                    if (this.reader instanceof ExtendedInputStreamReader) {
                        staxParserLogger.error(String.format("Error parsing file %s with StaxParser in while loop" ,
                                ((ExtendedInputStreamReader)
                                this.reader).getFile().getAbsolutePath() ));
                    }

                    staxParserLogger.error("thrown exception",ex );
                    p.next();
                }
            }


            this.solrDocs = dF.getSolrDocs();

        } catch (XMLStreamException streamException) {
            if (this.reader instanceof ExtendedInputStreamReader) {
                staxParserLogger.error(String.format("Error parsing file %s with StaxParser" ,((ExtendedInputStreamReader)
                        this.reader).getFile().getAbsolutePath() ));
            }

            staxParserLogger.error("thrown exception",streamException );
        }


    }


    public void setDebugIndexedDocs(boolean debugIndexedDocs) {
        this.debugIndexedDocs = debugIndexedDocs;
    }

    public ArrayList<SolrInputDocument> getSolrDocs() {
        return solrDocs;
    }

    public int getNumberParsedRecords() {
        return numberParsedRecords;
    }



}
