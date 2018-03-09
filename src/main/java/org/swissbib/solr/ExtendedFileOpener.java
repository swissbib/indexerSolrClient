package org.swissbib.solr;

import org.apache.commons.io.input.BOMInputStream;
import org.metafacture.framework.FluxCommand;
import org.metafacture.framework.MetafactureException;
import org.metafacture.framework.ObjectReceiver;
import org.metafacture.framework.annotations.Description;
import org.metafacture.framework.annotations.In;
import org.metafacture.framework.annotations.Out;
import org.metafacture.framework.helpers.DefaultObjectPipe;
import org.metafacture.io.FileCompression;

import java.io.*;

/**
 * Opens a file and passes an extended reader for it to the receiver.
 *
 * @author Christoph Böhme
 * @author Günter Hipler
 *
 */
@Description("Opens a file as Extension of MF core open file. The name of the open file is retained for the " +
        "receiver because it is necessary in case of different events (deletes or updates in Solr docproc workflow)")
@In(String.class)
@Out(java.io.Reader.class)
@FluxCommand("open-exfile")
public class ExtendedFileOpener
        extends DefaultObjectPipe<String, ObjectReceiver<Reader>> {

    private String encoding = "UTF-8";
    private FileCompression compression = FileCompression.AUTO;

    /**
     * Returns the encoding used to open the resource.
     *
     * @return current default setting
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Sets the encoding used to open the resource.
     *
     * @param encoding
     *            new encoding
     */
    public void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    public FileCompression getCompression() {
        return compression;
    }

    public void setCompression(final FileCompression compression) {
        this.compression = compression;
    }

    public void setCompression(final String compression) {
        setCompression(FileCompression.valueOf(compression.toUpperCase()));
    }

    @Override
    public void process(final String file) {
        try {
            final InputStream fileStream = new FileInputStream(file);
            try {
                final InputStream decompressor = compression.createDecompressor(fileStream);
                try {

                    ExtendedInputStreamReader eiR = new ExtendedInputStreamReader(new BOMInputStream(
                            decompressor), encoding);
                    eiR.setFile(new File(file));

                    getReceiver().process(eiR);
                } catch (final IOException | MetafactureException e) {
                    decompressor.close();
                    throw e;
                }
            } catch (final IOException | MetafactureException e) {
                fileStream.close();
                throw e;
            }
        } catch (final IOException e) {
            throw new MetafactureException(e);
        }
    }

}
