package org.swissbib.solr;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class SolrXMLUpdateParser extends DefaultHandler {

    private static final Logger updatelogger = LogManager.getLogger(SolrXMLUpdateParser.class);

    private File file2parse;


    private HashMap<String,ArrayList<String>> fieldsOfDoc = new HashMap<>();
    private boolean docStartet = false;

    private String lastFieldName = "";

    private int numberParsedRecords = 0;

    private ArrayList<SolrInputDocument> solrDocs = new ArrayList<>();

    private SolrInputDocument currentSolrDoc = null;

    private boolean debugIndexedDocs = false;



    SolrXMLUpdateParser(File file2parse) {

        this.file2parse = file2parse;
    }


    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        if (qName.equals("doc")) {

            this.numberParsedRecords++;
            currentSolrDoc = new SolrInputDocument();
            this.docStartet = true;

        } else if (this.docStartet && qName.equals("field")) {

            this.lastFieldName = attributes.getValue("name");

            if (this.debugIndexedDocs && this.lastFieldName.equals( "id")) {
                updatelogger.debug("parsing doc with id: " + this.lastFieldName);
            }

        }

    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (qName.equals("doc")) {

            this.docStartet = false;
            this.solrDocs.add(this.currentSolrDoc);
            if (this.debugIndexedDocs) {
                updatelogger.debug("created SolrInputDocument: " + this.currentSolrDoc.toString());
            }
            this.currentSolrDoc = null;

        }

    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {

        final StringBuilder buffer = new StringBuilder();
        if (! lastFieldName.equals("") && this.currentSolrDoc != null) {

            for (int position = 0; position < length; position++)
                buffer.append(ch[position + start]);


            this.currentSolrDoc.addField(this.lastFieldName,buffer.toString());
            this.lastFieldName = "";

        }

    }


    public void parseFile() {

        final SAXParserFactory factory = SAXParserFactory.newInstance();
        try
        {
            final SAXParser parser = factory.newSAXParser();
            parser.parse(this.file2parse, this);
        }
        catch (final ParserConfigurationException | SAXException | IOException e)
        {
            updatelogger.error("Error parsing file: " + this.file2parse.getName());
            updatelogger.error(e.getStackTrace());

        }
    }

    public ArrayList<SolrInputDocument> getSolrDocs() {
        return solrDocs;
    }

    public int getNumberParsedRecords() {
        return numberParsedRecords;
    }

    public void setDebugIndexedDocs(boolean debugIndexedDocs) {
        this.debugIndexedDocs = debugIndexedDocs;
    }
}
