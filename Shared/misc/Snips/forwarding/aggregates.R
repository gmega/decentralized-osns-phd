#!/usr/bin/env Rscript

lib_home <- Sys.getenv("SNIPS_HOME")
graphs_home <- Sys.getenv("GRAPHS_HOME")
source(paste(lib_home, "forwarding/latency-load-snippets.R", sep="/"))

# Command line parsing.
args <- commandArgs(TRUE)
latency_file <- args[1]

# Reads the data.
o_table <- read.table(latency_file, header=TRUE)
a_table <- table_processing(o_table, NULL)
delivered <- sum(a_table$delivered)
undelivered <- sum(a_table$undelivered)
residue <- safe_divide(undelivered, (delivered + undelivered))

duplicates <- sum(a_table$duplicates)
ratio <- safe_divide((duplicates + delivered), delivered)

residue_vec <- get_residue(a_table)
duplicate_vec <- get_dup_ratio(a_table)
duplicate_vec <- duplicate_vec[!is.nan(duplicate_vec) & is.finite(duplicate_vec)]
max_dups <- max(duplicate_vec)

latency_avg <- global_latency_average(a_table)
latency_var <- global_variance(a_table)
latency_max <- max(o_table$t_max)
latency_avg_tmax <- global_average_of_maximums(a_table)
std_dev_dups <- sqrt(var(duplicate_vec))
std_dev_res <- sqrt(var(residue_vec))

cat(paste("delivered", delivered, "\n"))
cat(paste("undelivered", undelivered, "\n"))
cat(paste("duplicates", duplicates, "\n"))
cat(paste("residue", residue, "\n"))

cat(paste("std dev residue", std_dev_res, "\n"))

cat(paste("duplicate ratio", ratio, "\n"))
cat(paste("max duplicate ratio", max_dups, "\n"))
cat(paste("std dev duplicate ratio", std_dev_dups, "\n"))

cat(paste("max latency", latency_max, "\n"))
cat(paste("avg latency", latency_avg, "\n"))
cat(paste("std dev latency", sqrt(latency_var), "\n"))
cat(paste("avg max latency", latency_avg_tmax, "\n"))

percentiles <- c(0.9, 0.95, 0.99)
print_percentiles("latency", a_table$t_avg, percentiles)
print_percentiles("residue", residue_vec, percentiles)
print_percentiles("duplicate ratio", duplicate_vec, percentiles)


