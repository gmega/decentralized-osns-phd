#!/bin/bash

export RELSIZES=./relsizes/relsizes-$1.text
export INTERSECTIONS=./intersections/intersections-$1.text
export TIMEVALUES=./times/times-$1.text

export OUTPUT_RELSIZES=$2/relsizes-$1.pdf
export OUTPUT_INTERSECTIONS=$2/intersections-$1.pdf

export i=$1

export SIZE=`cat $GRAPHS_HOME/2hop-samples/remapped/twohop-${1}.al | wc -l`

cat stragplot | analyzer -t python misc.cli.Subst | R --no-save