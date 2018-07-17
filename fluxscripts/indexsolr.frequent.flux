
default dir2read = "/swissbib_index/solrDocumentProcessing/MarcToSolr/data/outputfilesFrequent";
default appPropPath = FLUX_DIR + "../app.properties";


dir2read|
read-dir (recursive="true")|
open-exfile|
index-file-to-solr (appPropPath);