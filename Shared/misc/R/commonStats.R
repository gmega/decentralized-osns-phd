lib_home <- Sys.getenv("RLIB_HOME")
source(paste(lib_home,"common.R",sep="/"))

#
# Computes the average ranking of each element in a vector, 
# and returns the rankings in the original order.
#
avg_rank <- function(vec, sortf=order) {
	ordering <- sortf(vec)
	reverse <- order(ordering)

	ordered <- vec[ordering]
	rank <- 1:length(vec)
	
	avg_ranks <- aggregate(rank, by=list(data=ordered), mean)
	ordered <- data.frame(data=ordered)
	avg_ranks <- merge(ordered, avg_ranks, all.x=TRUE)
	return(avg_ranks$x[reverse])
}

spearman <- function(a, b) {
	rxi <- avg_rank(a)
	ryi <- avg_rank(b)
	return(cor(rxi, ryi))
}

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

#
# Computes the mean absolute error of a vector.
#
# @param x: a numeric vector.
#
mar <- function(x) {
	total <- sum(abs(x - mean(x)))
	return(total/length(x))
}

#
# Compute Newman's assortativity coefficient.
#
assortativity <- function(graph)
{
	degs <- degree(graph)
	edges <- get.edgelist(graph, names=FALSE) + 1
	return(cor(degs[edges[,1]],degs[edges[,2]]))
}

# -----------------------------------------------------------------------------
# Shifted Pareto number generator.
# -----------------------------------------------------------------------------
setConstructorS3("ShiftedPareto", function(alpha, beta) {
	extend(Object(), "ShiftedPareto",
			.alpha = alpha,
			.beta = beta)		
})

setMethodS3("sample", "ShiftedPareto", function(this, n) {
	u <- runif(n)
	boundf <- function(x) {
		return(.shiftedParetoTransform(this, x))
	}
	return(sapply(u, boundf, simplify=TRUE))
})

setMethodS3(".shiftedParetoTransform", "ShiftedPareto", function(this, u) {
	return(this$.beta * ((u^(-1.0 / this$.alpha)) - 1));
});

setMethodS3("variance", "ShiftedPareto", function(this) {
	xsi <- 1.0/this$.alpha
	sigma <- this$.beta/this$.alpha
	a <- (sigma/(1.0 - xsi))^2
	b <- (1.0 - 2*xsi)
	return (a/b)
});
