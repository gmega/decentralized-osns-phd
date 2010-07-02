#!/bin/bash

. $BASH_LIB/common.lib

checkpar 2 $#

TEMPLATE_NAME=$1
PROTOCOL_NAMES=$2
OP_DIRECTORY=$PWD

shift 2

echo Finding template files. 

for i in `find -iname $TEMPLATE_NAME`
do
    NAME=`sed "${i}p" $PROTOCOL_NAMES`
    CMD="full-analysis.sh $i $SOCIAL_GRAPH_STEM $PROTOCOL_NAME $@"
    echo Running $CMD.
    $CMD
done