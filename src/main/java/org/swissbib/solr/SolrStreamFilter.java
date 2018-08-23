package org.swissbib.solr;

import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.ArrayList;

public class SolrStreamFilter implements StreamFilter {

    private ArrayList<SolrInputDocument> solrDocs = new ArrayList<>();
    private boolean docStartet = false;
    private SolrInputDocument currentSolrDoc = null;
    private boolean debugIndexedDocs = false;
    private int numberParsedRecords = 0;

    private static final Logger filterlogger = LoggerFactory.getLogger(SolrStreamFilter.class);







    @Override
    public boolean accept(XMLStreamReader reader) {

        boolean parse = false;


        final int eventType = reader.getEventType();
        try {
            switch (eventType) {
                case XMLStreamConstants.ATTRIBUTE:
                case XMLStreamConstants.CDATA:
                case XMLStreamConstants.CHARACTERS:
                case XMLStreamConstants.COMMENT:
                case XMLStreamConstants.DTD:
                case XMLStreamConstants.ENTITY_DECLARATION:
                case XMLStreamConstants.ENTITY_REFERENCE:
                case XMLStreamConstants.NAMESPACE:
                case XMLStreamConstants.NOTATION_DECLARATION:
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                case XMLStreamConstants.START_DOCUMENT:
                    break;


                case XMLStreamConstants.END_ELEMENT:
                    if (reader.getLocalName().equalsIgnoreCase("doc")) {

                        this.docStartet = false;

                        this.solrDocs.add(this.currentSolrDoc);
                        if (this.debugIndexedDocs) {
                            System.out.println("now debugging");
                            //updatelogger.debug("created SolrInputDocument: " + this.currentSolrDoc.toString());
                        }
                        this.currentSolrDoc = null;

                    }


                    break;

                case XMLStreamConstants.START_ELEMENT:
                    if (reader.getLocalName().equalsIgnoreCase("doc")) {
                        this.numberParsedRecords++;
                        currentSolrDoc = new SolrInputDocument();
                        this.docStartet = true;
                    } else if (reader.getLocalName().equalsIgnoreCase("field")) {
                        String fieldName = reader.getAttributeValue(0);
                        this.currentSolrDoc.addField(fieldName,reader.getElementText());
                        parse = true;
                    }
                    break;

            }
        } catch (XMLStreamException strEx) {
            filterlogger.error("Exception in accept Filter method of SolrStreamFilter",strEx);
            strEx.printStackTrace();
        }

        return parse;
    }

    public ArrayList<SolrInputDocument> getSolrDocs() {
        return solrDocs;
    }

}
