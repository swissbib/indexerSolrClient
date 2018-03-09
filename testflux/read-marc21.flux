// opens file 'fileName', interprets the content as marc21 and writes to stdout

default fileName = FLUX_DIR + "10.marc21";
//default file = FLUX_DIR + "/test.txt";
//default dir2read = FLUX_DIR + "/files";
default dir2read = "/swissbib_index/solrDocumentProcessing/MarcToSolr/data/outputfiles";

default appPropPath = FLUX_DIR + "../app.properties";



dir2read|
read-dir (recursive="true")|

open-exfile|
//as-lines|
//decode-marc21|
//encode-formeta(style="multiline") |
//write("stdout");
index-file-to-solr (appPropPath);