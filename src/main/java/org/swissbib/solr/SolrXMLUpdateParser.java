package org.swissbib.solr;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.apache.solr.common.SolrInputDocument;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class SolrXMLUpdateParser extends DefaultHandler {

    private static final Logger updatelogger = LoggerFactory.getLogger(SolrXMLUpdateParser.class);

    private InputStreamReader file2parse;


    private HashMap<String,ArrayList<String>> fieldsOfDoc = new HashMap<>();
    private boolean docStartet = false;

    private String lastFieldName = "";

    private int numberParsedRecords = 0;

    private ArrayList<SolrInputDocument> solrDocs = new ArrayList<>();

    private SolrInputDocument currentSolrDoc = null;

    private boolean debugIndexedDocs = false;

    private String id = "defaultvalue";



    SolrXMLUpdateParser(InputStreamReader file2parse) {

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

        if (! lastFieldName.equals("") && !Objects.isNull(this.currentSolrDoc)) {

            for (int position = 0; position < length; position++)
                buffer.append(ch[position + start]);

            if (this.lastFieldName.equals( "id")) {
                this.id = buffer.toString();
                if (this.debugIndexedDocs) {
                    updatelogger.debug("parsing doc with id: " + buffer.toString());
                }
            }

            //todo: we have problems with parsing the freshness field. It happens that our sax parser is going
            //to shorten the value - don't know why at the moment.
            //because it's not so often but wehen it happens the complete package will be neglected by the SOLR server
            //I implement this quick patch where I use a placeholder for the freshness field for these specific records
            if (lastFieldName.equals("freshness") && buffer.toString().length() != 20) {
                this.currentSolrDoc.addField(this.lastFieldName,"1999-01-01T00:00:00Z");
                updatelogger.info("problems with the freshness field " + buffer.toString() + " in doc: " + this.id);

            } else if (lastFieldName.equals("time_processed")) {

                TimeZone tz = TimeZone.getTimeZone("UTC");
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
                df.setTimeZone(tz);
                String nowAsISO = df.format(new Date());


                this.currentSolrDoc.addField("time_processed",nowAsISO);
            } else {
                this.currentSolrDoc.addField(this.lastFieldName,buffer.toString());
            }



            this.lastFieldName = "";

        }

    }


    public void parseFile() {

        final SAXParserFactory factory = SAXParserFactory.newInstance();
        try
        {
            final SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource( this.file2parse), this);
        }
        catch (final ParserConfigurationException | SAXException | IOException e)
        {
            //updatelogger.error("Error parsing file: " + this.file2parse.getName());
            for (StackTraceElement ste :  e.getStackTrace()) {
                updatelogger.error("parser error", ste);
            }

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
