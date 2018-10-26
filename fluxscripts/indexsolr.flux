
//this is the default value for initial indexing;
//you should always overwrite it when starting the flux-script with a key/value pair for dir2read
default dir2read = "/swissbib_index/solrDocumentProcessing/MarcToSolr/data/outputfiles";
default config = "";

default appPropPath = FLUX_DIR + config;


dir2read|
read-dir (recursive="true")|
open-exfile|
index-file-to-solr (appPropPath);