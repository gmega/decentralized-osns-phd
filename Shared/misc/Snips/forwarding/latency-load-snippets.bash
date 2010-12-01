# Run snippet
for i in `seq 1 6`; do ( ./run.sh ../../master.template 2> ./outputs-err/err-${i}.text | gzip > ./outputs-out/log-${i}.text.gz &); done;

# Labeled runs. 
for i in `seq 1 15`; do echo "id degree t_max t_avg t_var avg_gain max_gain duplicates undelivered" > ./analysis/run-${i}.text; cat log-${i}.text | grep "DE:" | cut -d" " -f2-10 >> ./analysis/run-${i}.text; done;

# Extracts the schedule.
cat log-1.text | grep Scheduled | head -n 1200 | cut -d" " -f4.

# R Script.
source("/home/giuliano/workspace/Utilities/misc/R/common.R")
all_data <- load_tables(15, simple_name_generator("./run-", ".text"))

#######################################################
# Load extraction
#######################################################

# -----------------------------------------------------
# DEMERS
# -----------------------------------------------------

# For BaseLoad script.

for i in 0.05 0.1 0.2 0.3 0.4; do
	(nohup zcat output-GIVEUP_${i}-1.out.gz | egrep -e "N:|-- Unit experiment" | sed 's/.$//' | sed 's/^N://' | sed -r 's/ GIVEUP [0-9]+.[0-9]//' | analyzer -t python --verbose ex1.analysis.BaseLoad > ./tot-load/load-${i}.text 2> ./tot-load/load-${i}.err &) ; 
done;

# For LoadSim.
for i in 0.05 0.1 0.2 0.3 0.4; do
echo "root_id neighbor_id sent received duplicates" > ./loadsim-inputs/sim_input-${i}.text
(nohup cat ./output-GIVEUP_${i}-1.out.gz | pigz -d | analyzer -t python -V sep='-- Unit experiment 72302' misc.cli.PrintUntil | grep "N:" | sed 's/.$//' | sed 's/^N://' | sed -r 's/ GIVEUP [0-9]+.[0-9]//' >> ./loadsim-inputs/sim_input-${i}.text 2> ./loadsim-inputs/sim_input-${i}.err &); done;

# -----------------------------------------------------
# Parameterless heuristics.
# -----------------------------------------------------

# BaseLoad script.
nohup cat ./output.gz | pigz -d | grep "N:" | cut -d":" -f2 | analyzer -t python --verbose ex1.analysis.BaseLoad &> ./tot-loads.text &

# BaseLoad script with CHURN.
for i in 0.01 0.05 0.1; do (nohup zcat output-${i}-1.out.gz | grep "N:" | sed 's/.$//' | sed 's/^N://' | sed -r 's/ DOWN [0-9]+.[0-9]//' | analyzer -t python --verbose ex1.analysis.BaseLoad &> ./tot-loads/tot-load-${i}.text &); done;

# For LoadSim.
echo "root_id neighbor_id sent received duplicates" > ./loadsim-inputs/input.text
nohup cat ./output.gz | pigz -d | analyzer -t python -V sep='-- Unit experiment 72302' misc.cli.PrintUntil | grep "N:" | cut -d":" -f2 >> ./loadsim-inputs/input.text

# -----------------------------------------------------
# PSI-based heuristics
# -----------------------------------------------------
# Dettaching the PSI from the rest.
# From PSI to 0.1 to 0.9
cat $i | pigz -d | grep "N:" | sed 's/.$//' | cut -d":" -f2 | sed 's/PSI [0-9]\.[0-9]/& /' | cut -d" " -f2-6 

# The painful 0.99.
cat $i | pigz -d | grep "N:" | sed 's/.$//' | cut -d":" -f2 | sed 's/PSI 0\.99/& /' | cut -d" " -f2-6

#######################################################
# LoadSim command line.
#######################################################

# -----------------------------------------------------
# DEMERS  
# -----------------------------------------------------
ACTIVITY=0.01
for i in 0.4 0.3 0.2 0.1; do
	cat ${PWD}/loadsim-inputs/sim_input-${i}.text.gz | pigz -d |\
	analyzer-j -i ${GRAPHS_HOME}/Facebook.al:stdin\
 -p scheduler=it.unitn.disi.analysis.loadsim.UniformRateScheduler:\
probability=${ACTIVITY}:\
repetitions=15:\
decoder=it.unitn.disi.graph.codecs.AdjListGraphDecoder:\
size_generator=it.unitn.disi.analysis.loadsim.TrivialSizeGenerator:\
min=100:\
max=80000:\
cores=8:\
print_mode=summary_only:\
sim_mode=root:\
seed="__NONE__"\
 it.unitn.disi.analysis.loadsim.LoadSimulator > ./loadsim-outputs/load_analysis-${i}-${ACTIVITY}.text 
done;

# -----------------------------------------------------
# Parameterless heuristics
# -----------------------------------------------------

ACTIVITY=0.01
nohup cat ${PWD}/loadsim-inputs/input.text.gz | pigz -d |\
analyzer-j -i ${GRAPHS_HOME}/Facebook.al:stdin\
 -p scheduler=it.unitn.disi.analysis.loadsim.UniformRateScheduler:\
probability=${ACTIVITY}:\
repetitions=15:\
decoder=it.unitn.disi.graph.codecs.AdjListGraphDecoder:\
size_generator=it.unitn.disi.analysis.loadsim.TrivialSizeGenerator:\
min=100:\
max=80000:\
cores=8:\
print_mode=summary_only:\
sim_mode=root:\
seed="__NONE__"\
 it.unitn.disi.analysis.loadsim.LoadSimulator > ./loadsim-outputs/load_analysis-${ACTIVITY}.text 2> ./loadsim-outputs/load_analysis-${ACTIVITY}.err

# -- Overhead analysis --
cat ${PWD}/loadsim-inputs/input.text.gz | pigz -d |\
analyzer-j -i ${GRAPHS_HOME}/Facebook.al:stdin\
 -p scheduler=it.unitn.disi.analysis.loadsim.UniformRateScheduler:\
probability=${ACTIVITY}:\
repetitions=15:\
decoder=it.unitn.disi.graph.codecs.AdjListGraphDecoder:\
size_generator=it.unitn.disi.analysis.loadsim.BloomFilterOverheadGenerator:\
min=100:\
max=80000:\
cores=8:\
print_mode=summary_only:\
sim_mode=root:\
seed="__NONE__"\
 it.unitn.disi.analysis.loadsim.LoadSimulator > ./loadsim-outputs/overhead_analysis-${ACTIVITY}.text 

#######################################################
# Latency extraction and aggregate computation.
#######################################################

# -----------------------------------------------------
# DEMERS  
# -----------------------------------------------------

for i in 0.05 0.1 0.2 0.3 0.4; do
	echo id degree t_max t_avg t_var avg_speedup min_speedup duplicates undelivered > ./latencies/latency-${i}.text
	(nohup zcat output-GIVEUP_${i}-1.out.gz | grep DE: | sed 's/DE://' | sed 's/.$//' | sed -r 's/ GIVEUP [0-9]+.[0-9]//' | sed 's/Infinity/0/g' | sed 's/NaN/0/g' | sed 's/-4.6566128730773926E-10/0/g' | sed 's/-2147483648/0/g' >> ./latencies/latency-${i}.text &) ; 
done;

for i in 0.05 0.1 0.2 0.3 0.4; do
	export INPUT_FILE=${PWD}/analysis/latency-${i}.text
	cat ${SNIPS_HOME}/forwarding/aggregates.R | R --no-save | tee ./analysis/aggregate-${i}.text
done;

# -----------------------------------------------------
# Parameterless heuristics.
# -----------------------------------------------------
echo id degree t_max t_avg t_var avg_speedup min_speedup duplicates undelivered > ./latencies/latencies.text
cat ./output.gz | pigz -d | grep DE: | cut -d":" -f2 | sed 's/Infinity/0/g' | sed 's/NaN/0/g' | sed 's/-4.6566128730773926E-10/0/g' | sed 's/-2147483648/0/g' >> ./latencies/latencies.text

# -----------------------------------------------------
# PSI heuristics.
# -----------------------------------------------------

# CHECK, cause the -f3-10 is kind of bizarre and works only if there's a space before the psi.
nohup cat `ls *.gz` | pigz -d |  grep DE: | cut -d":" -f2 | sed 's/Infinity/0/g' | sed 's/NaN/0/g' | sed 's/-4.6566128730773926E-10/0/g' | sed 's/-2147483648/0/g' | sed 's/.$//' | cut -d" " -f3-8,12 | analyzer -t python -V keys=psi ex1.analysis.BestPars > ./latencies/optimal-latencies.text 2> ./latencies/optimal-latencies.err
#######################################################
# Parameter Analysis snippets.
#######################################################

# Cut snippet for centrality.

# If needs to remove last char:
sed 's/.$//'

# Cleans the crap from IncrementalStats
sed 's/NaN/0/g' | sed 's/-4.6566128730773926E-10/0/g' | sed 's/-2147483648/0/g' 

# Centrality - modern, whole deal.
zcat output-PSI_0.1-R_0.out.gz | grep DE: | sed 's/NaN/0/g' | sed 's/-4.6566128730773926E-10/0/g' | sed 's/-2147483648/0/g' | sed 's/ R [0-9]//' | sed 's/.$//' | cut -d" " -f3-8,12


ALL_LOGS=log-{`seq -s, 1 15`}.text
cat `eval echo $ALL_LOGS` | grep "DE:" | cut -d" " -f3-8,12 | analyzer -t python --psyco ex1.scripts.BestPSI > ./best-psi.text

# Cut snippet for sequential heuristics.

cat `eval echo $ALL_LOGS` | grep DE: | cut -d" " -f3,5-10,14 | analyzer -t python -V keys=psi,nzero ex1.scripts.BestPars > ./analysis/optimal.text

cat `eval echo $ALL_LOGS` | grep DE: | sed 's/ R [0-9]//' | cut -d" " -f3,5-10,14 | sed 's/.$//' | analyzer -t python -V keys=psi,nzero ex1.scripts.BestPars > ./analysis/optimal.text

# Cut snippet for probabilistic heuristics.
cat `eval echo $ALL_LOGS` | grep DE: | cut -d" " -f3,5-11,15 | analyzer -t python -V keys=psi,p1,p2 ex1.scripts.BestPars > ./analysis/optimal.text

# Load analysis.
cat optimal.text | sed '1d' > optimal-noheader.text
cat optimal-noheader.text | awk '{print $3" "$4" "$1}' > keys.text
echo "psi nzero root_id neighbor_id sent received duplicates" > loads.text
cat `eval echo $ALL_LOGS` | grep "N:" | cut -d" " -f3,5-9| analyzer -t python -V optimals=./keys.text:keylength=3 ex1.scripts.Join >> loads.text

echo "id sent received duplicates" > loads.text
cat `eval echo $ALL_LOGS` | grep N: | cut -d" " -f2-5 >> loads.text

# Random disturbances.
disturbances <- runif(length(tavg), 0, 0.02)
disturbances <- disturbances - 0.01
tavg_d <- tavg + disturbances
plot (degrees ~ tavg_d, cex=0.3)

analyzer-j -i /home/giuliano/Graphs/Facebook.al:./unit-loads-1.text -p percentage=0.25:cores=2 it.unitn.disi.analysis.loadsim.LoadSimulator > load-analysis-1.text

for i in `seq 1 15`; do ucat log-${i}.text.gz | grep N: | cut -d" " -f1-4 | cut -d":" -f2 > ./load-analysis/load-${i}.text; done;
for i in `seq 1 15`; analyzer-j -i $GRAPHS_HOME/Facebook.al:../load-data/load-data-${i}.text -p percentage=0.25:cores=8 it.unitn.disi.analysis.loadsim.LoadSimulator > load-analysis-${i}.text; done;

######################################################
# CyclonSN debugging.
#####################################################
cat 500round-debug.text | grep --text INDG | cut -d" " -f2-5 | grep "^299 " | cut -d" " -f 3-4 > ./debug/indegree.text
cat 500round-debug.text | grep --text "F1" | cut -d" " -f4-5 > ./debug/f1.text

echo "root_id neighbor_id sent received duplicates" > loads.text
cat output.out | grep N: | cut -d":" -f2 >> loads.text

analyzer-j -i ${HOME}/Graphs/Facebook.al:./loads.text -p cores=6:probability=0.02:decoder=it.unitn.disi.graph.codecs.AdjListGraphDecoder:sim_mode=root:print_mode=summary_only:scheduler=it.unitn.disi.analysis.loadsim.UniformRateScheduler:repetitions=500 it.unitn.disi.analysis.loadsim.LoadSimulator
