package org.swissbib.solr;

import java.io.IOException;
import java.io.Reader;

public class IndexerMFClient {

    public IndexerMFClient() {
        Reader r = new Reader() {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                return 0;
            }

            @Override
            public void close() throws IOException {

            }
        };



    }


}
