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

residue_vec <- get_residue(a_table)
duplicate_vec <- get_dup_ratio(a_table)

latency_avg <- global_latency_average(a_table)
latency_var <- global_variance(a_table)
latency_max <- max(o_table$t_max)
latency_avg_tmax <- global_average_of_maximums(a_table)
percs <- quantile(a_table$t_avg, c(0.9, 0.95, 0.99))
respercs <- quantile(residue_vec, c(0.9, 0.95, 0.99))
drpercs <- quantile(duplicate_vec, c(0.9, 0.95, 0.99))
duplicate_vec <- duplicate_vec[!is.nan(duplicate_vec) & is.finite(duplicate_vec)]
max_dups <- max(duplicate_vec)
print(var(duplicate_vec))
std_dev_dups <- sqrt(var(duplicate_vec))
std_dev_res <- sqrt(var(residue_vec))

print(paste("Delivered:", delivered))
print(paste("Undelivered:", undelivered))
print(paste("Duplicates:", duplicates))
print(paste("Residue:", residue))
print(paste("Std. dev. Residue:", std_dev_res))
print(paste("Duplicate ratio:", ratio))
print(paste("Max dup. ratio:", max_dups))
print(paste("Std. dev dup. ratio:", std_dev_dups))
print(paste("Max. latency:", latency_max))
print(paste("Avg. max. latency:", latency_avg_tmax))
print(paste("Avg. latency:", latency_avg))
print(paste("Std. dev. latency:", sqrt(latency_var)))
print(paste("90th percentile latency:", as.numeric(percs[1])))
print(paste("95th percentile latency:", as.numeric(percs[2])))
print(paste("99th percentile latency:", as.numeric(percs[3])))
print(paste("90th percentile residue:", as.numeric(respercs[1])))
print(paste("95th percentile residue:", as.numeric(respercs[2])))
print(paste("99th percentile residue:", as.numeric(respercs[3])))
print(paste("90th percentile dup. ratio:", as.numeric(drpercs[1])))
print(paste("95th percentile dup. ratio:", as.numeric(drpercs[2])))
print(paste("99th percentile dup. ratio:", as.numeric(drpercs[3])))


