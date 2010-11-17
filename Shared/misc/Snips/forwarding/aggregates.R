#!/usr/bin/env Rscript

lib_home <- Sys.getenv("SNIPS_HOME")
graphs_home <- Sys.getenv("GRAPHS_HOME")

source(paste(lib_home, "forwarding/latency-load-snippets.R", sep="/"))

args <- commandArgs(TRUE)
latency_file <- args[1]

o_table <- read.table(latency_file, header=TRUE)
a_table <- table_processing(o_table, NULL)

delivered <- sum(a_table$delivered)
undelivered <- sum(a_table$undelivered)
duplicates <- sum(a_table$duplicates)
residue <- safe_divide(undelivered, (delivered + undelivered))
ratio <- safe_divide((duplicates + delivered), delivered)

latency_avg <- global_latency_average(a_table)
latency_var <- global_variance(a_table)
latency_max <- max(o_table$t_max)
latency_avg_tmax <- global_average_of_maximums(a_table)
percs <- quantile(a_table$t_avg, c(0.8, 0.9, 0.95, 0.99))

print(paste("Delivered:", delivered))
print(paste("Undelivered:", undelivered))
print(paste("Duplicates:", duplicates))
print(paste("Residue:", residue))
print(paste("Duplicate ratio:", ratio))
print(paste("Max. latency:", latency_max))
print(paste("Avg. max. latency:", latency_avg_tmax))
print(paste("Avg. latency:", latency_avg))
print(paste("Std. dev. latency:", sqrt(latency_var)))
print(paste("80th percentile latency:", as.numeric(percs[1])))
print(paste("90th percentile latency:", as.numeric(percs[2])))
print(paste("95th percentile latency:", as.numeric(percs[3])))
print(paste("99th percentile latency:", as.numeric(percs[4])))


