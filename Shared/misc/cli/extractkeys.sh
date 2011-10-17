#!/bin/bash

################################################################################
#
# Given a collection of tabular files, efficiently computes the intersection of
# rows.
#
################################################################################

. ${BASH_LIB}/common.lib

function print_help {
    err Syntax: $0 [options] FILE1 FILE2 ...
    err
    err Options:
    err -e "   -c [column] : uses this column for intersection"
    err -e "   -d          : deletes the first line in each file"
    exit 1
}

COLUMN=1

while getopts "c:d" o ; do  
    case $o in  
        c ) COLUMN=$OPTARG;;
        d ) NOFIRSTLINE=1;;
		h ) 
	    print_help
	    ;;
		\?)
	    err "Type $0 -h for help." >&2
	    exit 1
	    ;;
		:)
	    err "Option -$OPTARG requires an argument. Type $0 -h for help." >&2
	    exit 1
	    ;;
    esac  
done

shift $((OPTIND-1))
TMPFILE=`tempfile`
INTERSECTS=$#

err "1. Creating initial key set file."
err "   Tempfile is $TMPFILE."
for i in $@;
do 
	err " -- processing: $i"
    cat $i | cut -d" " -f1 | sed '1d' | uniq | sort -n >> $TMPFILE 
done
err "   Done."
err "2. Outputting intersections. "

cat $TMPFILE | sort -n | uniq --count | sed -e 's/^[ \t]*//' |\
 grep "^${INTERSECTS}" | cut -d" " -f2
err "Cleanup..."
#rm -rf $TMPFILE