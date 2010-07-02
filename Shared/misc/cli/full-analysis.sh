#!/bin/bash
#
# This script performs a full analysis of an experiment with 
# a standard folder structure.

. $BASH_LIB/common.lib
checkenv

# Uncomment for debug mode.
# DEBUG=true

# Controls whether temp folder will be clened or not.
if [ ${DEBUG} ]
then
    err " WARNING: Debug mode is ON."
else
    CLEANUP=true
fi

# ==============================
# Local functions.
# ==============================
function print_usage() {
    err "Syntax: $0 [experiment root] [social graph (stem)] [protocol_file_stem:descriptive_name] [analysis-output-folder]"
}

function summary() {
    VNAME="${1^^?}"

    # TODO Unify name generation with the Python part
    # (i.e. replace this part with a call to analyzer).
    export $VNAME"_STEM"=$1

    if [ ${2} ]
    then
	export $VNAME"_SUMMARY"="${1}-summary.${2}"
    fi	

    if [ ${3} ]
    then 
	export $VNAME"_EXPERIMENT"="${1}-${3}.${2}"
    fi

    # ------------------------------------------------
}

function plot() {
    err "Plotting metric $2."

    std_stats.R\
 --input ${4}/${1}\
 --metric $2\
 --algorithm "$5"\
 --output ${4}/${2}-${3}-linlin.eps |\
 analyzer --psyco --verbose -t python misc.cli.NumbersOnly > ${4}/${2}-stats-${3}.text
    
    std_stats.R\
 --input ${4}/${1}\
 --metric $2\
 --algorithm "$5"\
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
ALGORITHM=`echo $3 | awk 'BEGIN {FS=":"} {print $1}'`
DESCRIPTIVE=`echo $3 | awk 'BEGIN {FS=":"} {print $2}'`

err "Descriptive name is $DESCRIPTIVE, result stem is $ALGORITHM."

# ==============================
# Pre-analysis.
# ==============================
# Verifies that the folder structure is sane.
analyzer -t python --psyco -V root=$ROOT_FOLDER experiment.util.ChkStructure

REPETITIONS=$?

if [ ${DEBUG} ]
then
    REPETITIONS=1
fi

err "Will analyse $REPETITIONS repetitions."

if [ $REPETITIONS -le 0 ]
then
    exit $REPETITIONS
fi

# Verifies that the social graphs do exist.
assert_exists $GRAPHS_HOME/$SOCIAL_GRAPH".al"
assert_exists $GRAPHS_HOME/$SOCIAL_GRAPH".bin"

# Gets the size of the graph.
GRAPH_SIZE=`analyzer -t python --psyco -V input=$GRAPHS_HOME/$SOCIAL_GRAPH.al graph.cli.GraphSize`

# Makes a shadow copy of the root folder.
NEWROOT="${ROOT_FOLDER}-${RANDOM}"
cp -Rl $ROOT_FOLDER $NEWROOT
ROOT_FOLDER=$NEWROOT

# Message folder.
M=$ROOT_FOLDER/`analyzer -t python -V attribute="experiment.MESSAGE_LOG_FOLDER" misc.cli.ReadAttribute`
# Output folder.
O=$ROOT_FOLDER/`analyzer -t python -V attribute="experiment.OUTPUT_LOG_FOLDER" misc.cli.ReadAttribute`
# Analysis result folder.
A=$4/$ALGORITHM

mkdir -p $A/$ALGORITHM

err Verification passed.

# ==============================
# Log parsing.
# ==============================

for i in `seq 1 $REPETITIONS`
do

    # Parses the binary log.
    summary "log" "bin.gz" $i
    summary "messages" "text" $i
    ucat $M/$LOG_EXPERIMENT | analyzer-j -i $GRAPHS_HOME/$SOCIAL_GRAPH".bin":stdin it.unitn.disi.analysis.LatencyComputer > $M/$MESSAGES_EXPERIMENT

    # Extracts the standard set of metrics. These are:
    summary "load" "text" $i 
    summary "latency" "text" $i 
    summary "minload" "text" $i
    summary "dups" "text" $i
    summary "total" "text" $i
    
    # 1 - load (per-node);
    # 2 - load (total);
    # 3 - delivered messages (total);
    # 4 - undelivered messages (total);
    # 5 - latency distribution;
    # 6 - zero-load versus time.
    parselog --file $M/$MESSAGES_EXPERIMENT --psyco -V network_size=$GRAPH_SIZE --nolabels --statistics\
 load:allpoints:$M/$LOAD_EXPERIMENT\
,load:total:append:$M/$TOTAL_EXPERIMENT\
,delivered:total:append:$M/$TOTAL_EXPERIMENT\
,duplicates:total:append:$M/$TOTAL_EXPERIMENT\
,undelivered:total:append:$M/$TOTAL_EXPERIMENT\
,latency:allpoints:$M/$LATENCY_EXPERIMENT\
,minload:allpoints:$M/$MINLOAD_EXPERIMENT
    
    # 8 - Duplicates-per-message (per-message).
    analyzer --verbose -t python -V log=$M/$MESSAGES_EXPERIMENT:social_graph=$GRAPHS_HOME/$SOCIAL_GRAPH".al" experiment.logparse.DuplicatesPerMessage > $M/$DUPS_EXPERIMENT
done

# Averages everything.
summary "load" "text"    
summary "latency" "text"
summary "total" "text"

extract_average $REPETITIONS $M/$LOAD_STEM "text" > $A/$LOAD_SUMMARY        # Per-node load.
extract_average $REPETITIONS $M/$LATENCY_STEM "text" > $A/$LATENCY_SUMMARY  # Per-node latencies.
extract_average $REPETITIONS $M/$MINLOAD_STEM "text" > $A/$MINLOAD_SUMMARY  # Zero-load versus time.
extract_average $REPETITIONS $M/$TOTAL_STEM "text" > $A/$TOTAL_SUMMARY      # Total load, delivered, undelivered.

# Now drift, and convergence figures.
summary "drift" "text"
summary "convergence" "text"
summary "output"

extract_average -t 'drift' $REPETITIONS "$O/$OUTPUT_STEM" "text.gz" > $A/$DRIFT_SUMMARY
extract_average -t 'STABLE' -p ':' $REPETITIONS "$O/$OUTPUT_STEM" "text.gz" > $A/$CONVERGENCE_SUMMARY

# ==============================
# Plots graphs and statistics.
# ==============================

# Plots the graphs and prints the statistics.
plot $LOAD_SUMMARY "load" $ALGORITHM $A "$DESCRIPTIVE"
plot $LATENCY_SUMMARY "latency" $ALGORITHM $A "$DESCRIPTIVE"

if [ ${CLEANUP} ]
then
    rm -rf $ROOT_FOLDER
fi

cd $A
minloadplot.g
mv minload.eps $A

