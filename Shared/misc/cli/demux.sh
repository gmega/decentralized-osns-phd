#!/bin/bash

# Parameter list:
#
# 1 - file list ("all" to use all in the current folder.);
# 2 - prefix.

. ${BASH_LIB}/common.lib

check_set "file list" $1
check_set "demux prefix" $2

if [ "$1" == "all" ] 
	then
	FILE_LIST=`ls | xargs`
else
	FILE_LIST=$1
fi

err "-- Prefix is $1."
err "-- File list is $FILE_LIST."

analyzer-j -s , -p \
file_list="$1",\
line_prefix=$2,\
matching_only=true,\
allow_partial=true,\
single_header=true \
it.unitn.disi.logparse.PeerSimLogDemux