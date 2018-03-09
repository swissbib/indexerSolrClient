package org.swissbib.solr;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class ExtendedInputStreamReader extends InputStreamReader {

    private File file = null;

    public ExtendedInputStreamReader(InputStream in) {
        super(in);
    }

    public ExtendedInputStreamReader(InputStream in, String charsetName) throws UnsupportedEncodingException {
        super(in, charsetName);
    }

    public ExtendedInputStreamReader(InputStream in, Charset cs) {
        super(in, cs);
    }

    public ExtendedInputStreamReader(InputStream in, CharsetDecoder dec) {
        super(in, dec);
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
