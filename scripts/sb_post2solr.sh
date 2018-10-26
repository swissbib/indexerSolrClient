#!/usr/bin/env bash

cwd=`pwd`

LOGDIR=$cwd/log
LOGFILE=$LOGDIR/post2solr.log


export METAFACTURE_HOME=$cwd



function usage()
{
 printf "usage: $0 [-i <content input directory>  -c <config properties>\n]\n"
}


function preChecks()
{

   [ ! -d "$LOGDIR" ] && mkdir -p $LOGDIR && printf "Logdir created ...\n"
   [ ! -d "$1" ] &&  printf "inputdir $1 not available ...\n"  &&  usage && exit 9
   [ -z "$2" ] &&  printf "config $2 not available ...\n"  &&  usage && exit 9

   printf "prechecks finished ...\n"
   printf "inputdir: $1 / config: $2\n"

}


function runIndexing()
{

   echo "given inputdir: $1"

   #[ ! -d "$LOGDIR" ] && mkdir -p $LOGDIR && printf "Logdir created ...\n" >> $LOGFILE
   [ ! -d "$1" ] &&  echo "inputdir $1 not available ...\n"    &&  exit 9

   printf "start indexing ...\n\n"

    ./flux.sh fluxscripts/indexsolr.flux \
            dir2read=$1 config=$2

}






#it seems getopts doesn't run in within function scope...
while getopts hi:c: OPTION
do
  case $OPTION in
    h) usage
	exit 9
	;;
    i) INPUTDIR=$OPTARG;;
    c) CONFIG=$OPTARG;;

    *) printf "unknown option -%c\n" $OPTION; usage; exit;;
  esac
done

preChecks $INPUTDIR $CONFIG
runIndexing $INPUTDIR $CONFIG

