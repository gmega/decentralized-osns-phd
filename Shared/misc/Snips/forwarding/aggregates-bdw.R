#!/usr/bin/env Rscript

lib_home <- Sys.getenv("RLIB_HOME")
source(paste(lib_home,"commonStats.R",sep="/"))

args <- commandArgs(TRUE)
latency_file <- args[1]

bdw <- read.table(latency_file, header=TRUE)
bdw <- add_squared_sum(bdw, "tx_avg", "tx_var", "duration", "tx_sqr_sum")
bdw <- add_squared_sum(bdw, "rx_avg", "rx_var", "duration", "rx_sqr_sum")

# Send
print(paste("Max. send bandwidth:", max(bdw$tx_avg)))
print(paste("Avg. max. send bandwidth:", mean(bdw$tx_max)))
print(paste("Avg. send bandwidth:", global_average(bdw, sum_row="tx_tot", n_row="duration")))
print(paste("Std. dev. send bandwidth:", sqrt(global_variance(bdw, sum_row="tx_tot", sqr_sum_row="tx_sqr_sum", n_row="duration"))))
print(paste("90th percentile send bandwidth:", as.numeric(quantile(bdw$tx_avg, 0.9))))
print(paste("95th percentile send bandwidth:", as.numeric(quantile(bdw$tx_avg, 0.95))))
print(paste("99th percentile send bandwidth:", as.numeric(quantile(bdw$tx_avg, 0.99))))

# Receive
print(paste("Max. receive bandwidth:", max(bdw$rx_avg)))
print(paste("Avg. max. receive  bandwidth:", mean(bdw$rx_max)))
print(paste("Avg. receive bandwidth:", global_average(bdw, sum_row="rx_tot", n_row="duration")))
print(paste("Std. dev. receive bandwidth:", sqrt(global_variance(bdw, sum_row="rx_tot", sqr_sum_row="rx_sqr_sum", n_row="duration"))))
print(paste("90th percentile receive bandwidth:", as.numeric(quantile(bdw$rx_avg, 0.9))))
print(paste("95th percentile receive bandwidth:", as.numeric(quantile(bdw$rx_avg, 0.95))))
print(paste("99th percentile receive bandwidth:", as.numeric(quantile(bdw$rx_avg, 0.99))))

