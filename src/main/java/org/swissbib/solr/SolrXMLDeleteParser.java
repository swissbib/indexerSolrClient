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
import java.lang.reflect.Array;
import java.util.ArrayList;


public class SolrXMLDeleteParser extends DefaultHandler{


    private static final Logger deletelogger = LoggerFactory.getLogger(SolrXMLDeleteParser.class);

    private InputStreamReader file2parse;
    private int numberParsedRecords = 0;

    private boolean hasDeleteTag = false;

    private String lastTagName = "";

    private ArrayList<String> ids2Delete = new ArrayList<>();

    private boolean debugIndexedDocs = false;


    SolrXMLDeleteParser(InputStreamReader file2parse) {

        this.file2parse = file2parse;

    }


    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        if (qName.equals("delete")) {

            this.hasDeleteTag = true;

        } else if (this.hasDeleteTag && qName.equals("id")) {

            this.lastTagName = qName;
        }

    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (qName.equals("id")) {
            this.lastTagName = "";
        }

        if (qName.equals("delete") && this.debugIndexedDocs) {

            this.ids2Delete.forEach((String id) -> {
                deletelogger.debug( String.format("%s will be deleted",id));
            });



        }

    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        final StringBuilder buffer = new StringBuilder();
        if (lastTagName.equals("id")) {

            for (int position = 0; position < length; position++)
                buffer.append(ch[position + start]);


            this.ids2Delete.add(buffer.toString());
            this.lastTagName = "";

        }
    }


    public void parseFile() {

        final SAXParserFactory factory = SAXParserFactory.newInstance();
        try
        {
            final SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(this.file2parse), this);
        }
        catch (final ParserConfigurationException | SAXException | IOException e)
        {
            //deletelogger.error("Error parsing file: " + this.file2parse.getName());

            for (StackTraceElement ste :  e.getStackTrace()) {
                deletelogger.error( "parser error", ste);
            }

        }
    }


    public ArrayList<String> getIds2Delete() {
        return ids2Delete;
    }

    public void setDebugIndexedDocs(boolean debugIndexedDocs) {
        this.debugIndexedDocs = debugIndexedDocs;
    }


}
