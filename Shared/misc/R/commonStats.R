lib_home <- Sys.getenv("RLIB_HOME")
source(paste(lib_home,"common.R",sep="/"))

#
# Adds "latency_sum" column to table.
#
add_simple_sum <- function(a_table, avg_row, n_row, sum_row="sum") {
	lsum <- a_table[c(avg_row)]*a_table[c(n_row)]
	return(add_row_with_name(a_table, lsum, sum_row))
}

#
# Adds "squared_sum" column to the table.
#
# @param avg_row: row containing averages.
# @param var_row: row containing variances.
# @param n_row: row containing number of observations.
#
add_squared_sum <- function(a_table, avg_row, var_row, n_row, sq_sum_row="squared_sum") {
	avg <- a_table[c(avg_row)]
	squared_average <- avg * avg
	sigma_square <- a_table[c(var_row)]
	n <- a_table[c(n_row)]
	squared_sum <- sigma_square + squared_average
	squared_sum <- squared_sum*n
	return(add_row_with_name(a_table, squared_sum, sq_sum_row))
}

#
# Computes the global variance.
#
# @param sum_row: row containing the sample sums.
# @param sqr_sum_row: row containing squared sums of the samples.
# @param n_row: row containing number of observations.
#
global_variance <- function(a_table, sum_row, sqr_sum_row, n_row) {
	average <- global_average(a_table, sum_row, n_row)
	sq_sums <- sum(sapply(a_table[c(sqr_sum_row)], as.numeric))
	n <- sum(a_table[c(n_row)])
	sq_average <- average*average
	variance <- sq_sums/n - sq_average
	return(variance)
}

#
# Computes the global average.
#
# @param sum_row: row containing the sample sums.
# @param n_row: row containing number of observations.
#
global_average <- function(a_table, sum_row, n_row) {
	summation <- sum(sapply(a_table[c(sum_row)], as.numeric))
	n <- sum(a_table[c(n_row)])
	return(summation/n)
}