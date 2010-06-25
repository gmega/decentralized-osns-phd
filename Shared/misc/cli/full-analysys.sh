#!/bin/bash
#
# This script performs a full analysis of an experiment with 
# a standard folder structure.

. $BASH_LIB/common.lib
checkenv

# ==============================
# Local functions.
# ==============================
function print_usage() {
    err "Syntax: $0 [experiment root] [social graph] [protocol name] [analysis-output-folder]"
}

function summary() {
    VNAME="${1^^?}_SUMMARY"

    # TODO Unify name generation with the Python part
    # (i.e. replace this part with a call to analyzer).
    if [ ! ${2} ]
    then
	RESULT="$1"
    elif [ ! ${3} ]
    then 
	RESULT="${1}-summary.${2}"
    else
	RESULT="${1}-${3}.${2}"
    fi
    # ------------------------------------------------
    export $VNAME="$RESULT"
}

function plot() {
    err "Plotting metric $2."

    std_stats.R\
 --input ${4}/${1}\
 --metric $2\
 --algorithm $3\
 --output ${4}/${2}-${3}-linlin.eps > ${4}/${2}-stats-${3}.text
    
    std_stats.R\
 --input ${4}/${1}\
 --metric $2\
 --algorithm $3\
 --output ${4}/${2}-${3}-loglin.eps\
 --logplot "y" > /dev/null
} 

# ==============================
# Command-line argument handling.
# ==============================
if [ $# -le 3 ]
then
    err "Required parameter missing."
    print_usage
    exit 1
fi

ROOT_FOLDER=$1
SOCIAL_GRAPH=$2
ALGORITHM=$3

# ==============================
# Pre-analysis.
# ==============================
# Verifies that the folder structure is sane.
analyzer -t python --psyco -V root=$ROOT_FOLDER experiment.util.ChkStructure
REPETITIONS=1
#REPETITIONS=$?

if [ $REPETITIONS -le 0 ]
then
    exit $REPETITIONS
fi

NEWROOT="${ROOT_FOLDER}-${RANDOM}"

# Makes a shadow copy of the root folder.
cp -Rl $ROOT_FOLDER $NEWROOT
ROOT_FOLDER=$NEWROOT

# Message folder.
M=$ROOT_FOLDER/`analyzer -t python -V attribute="experiment.MESSAGE_LOG_FOLDER" misc.cli.ReadAttribute`
# Output folder.
O=$ROOT_FOLDER/`analyzer -t python -V attribute="experiment.OUTPUT_LOG_FOLDER" misc.cli.ReadAttribute`
# Analysis result folder.
A=$4/$ALGORITHM

mkdir -p $A/$ALGORITHM

# ==============================
# Log parsing.
# ==============================

# Parses the binary log.
for i in `seq 1 $REPETITIONS`
do
    summary "log" "bin.gz" $i
    summary "messages" "text" $i
    ucat $M/$LOG_SUMMARY | analyzer-j -i $GRAPHS_HOME/$SOCIAL_GRAPH:stdin it.unitn.disi.analysis.LatencyComputer > $M/$MESSAGES_SUMMARY
done

# Extracts the standard set of metrics. These are:
for i in `seq 1 $REPETITIONS`
do
    # Generates names.
    summary "load" "text" $i 
    summary "latency" "text" $i 
    summary "messages" "text" $i 

    # 1 - load distribution;
    # 2 - latency distribution;
    parselog $M/$MESSAGES_SUMMARY --psyco -s\
 load:pernode:$M/$LOAD_SUMMARY\
,latency:pernode:$M/$LATENCY_SUMMARY
    
    # 3 - duplicates-per-message distribution;
#    analyzer --psyco -t python 
    # 4 - zero-load nodes versus time.
done

# Averages load, latency, duplicates-per-message, and zero-load versus time.
summary "load"
summary "load_avg" "text"

summary "latency"
summary "latency_avg" "text"

extract_average $REPETITIONS $M/$LOAD_SUMMARY "text" > $A/$LOAD_AVG_SUMMARY
extract_average $REPETITIONS $M/$LATENCY_SUMMARY "text" > $A/$LATENCY_AVG_SUMMARY

# Now drift, and convergence figures.
summary "drift" "text"
summary "convergence" "text"
summary "output"

extract_average -t 'drift' $REPETITIONS "$O/$OUTPUT_SUMMARY" "text.gz"> $A/$DRIFT_SUMMARY
extract_average -t 'STABLE' -p ':' $REPETITIONS "$O/$OUTPUT_SUMMARY" "text.gz" > $A/$CONVERGENCE_SUMMARY

# ==============================
# Plots graphs and statistics.
# ==============================

# Plots the graphs and prints the statistics.

plot $LOAD_AVG_SUMMARY "load" $ALGORITHM $A
plot $LATENCY_AVG_SUMMARY "latency" $ALGORITHM $A



