#!/bin/bash

PROJECTDIR_DOCPROCESSING=/swissbib_index/solrDocumentProcessing/MarcToSolr


POSTDIRBASE=${PROJECTDIR_DOCPROCESSING}/data/outputfiles

   for dir in `ls ${POSTDIRBASE}`
    do

        gunzip ${POSTDIRBASE}/$dir/*

    done


   echo "start indexer all job"
   java -jar -Dlog4j.configurationFile=log4j.xml \
            -Dlog.path.clientIndexer=indexerlogs \
            -Dapp.properties=app.properties  \
            indexerSolrClient-1.0-SNAPSHOT-plugin.jar


   for dir in `ls ${POSTDIRBASE}`
    do

        gzip ${POSTDIRBASE}/$dir/*

    done



