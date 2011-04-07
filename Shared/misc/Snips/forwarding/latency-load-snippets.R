#!/usr/bin/env Rscript

library(igraph)
library(ggplot2)

lib_home <- Sys.getenv("RLIB_HOME")
source(paste(lib_home,"commonStats.R",sep="/"))

heatmap_colours <- function() {
	return(c("blue", "cyan", "green", "yellow", "orange", "red"))
}

order_by <- function(table, field) {
	return(table[order(field),])
}

add_degree <- function(table, graph) {
	degrees <- degree(graph)
	# igraph IDs are zero-based, but R arrays are 1-based.
	indexes <- table$id + 1 
	return(cbind(table, degree=degrees[indexes]))
}


###############################################################################
# Load Analysis
###############################################################################

#
# Adds "delivered" column to the table.
#
add_delivered <- function(a_table) {
	delivered <- a_table$degree - a_table$undelivered
	return(cbind(a_table, delivered=delivered))
}

add_message_statistics <- function(load_table) {
	total_received <- load_table$received + load_table$duplicates
	total_messages <- load_table$received + load_table$duplicates + load_table$sent
}

combine_load_data <- function(load, ideal, factors=c("id")) {
	# Order tables.
	ideal <- order_by(ideal, ideal$id)
	load <- order_by(load, load$id)

	# Iron out the lenghts.
	ideal <- ideal[which(ideal$id %in% load$id),]
	
	# Aggregate everything but IDs on the load tables.
	load <- aggregate(load[names_excluding(load, factors)], list(id=load$id), sum)

	if (length(ideal$id) != length(load$id)) {
		warning(paste("ID tables differ in length: ",
				length(ideal$id),"!=", length(load$id),". Leftover IDs will be discarded."))
		load <- load[which(load$id %in% ideal$id),]
	}
	
	total_receives <- load$duplicates + load$received
	total_messages <- total_receives + load$sent
	ideal_total_messages <- ideal$wreceived + ideal$wsent
	
	return(cbind(load, ideal, total_received=total_receives, 
			total_messages=total_messages, wtotal_messages=ideal_total_messages))
}

add_overwork_statistics <- function(cload_table) {
	send_overwork <- safe_divide(cload_table$sent, cload_table$wsent)
	receive_overwork <- safe_divide(cload_table$total_received, cload_table$wreceived)
	
	send_savings <- cload_table$wsent - cload_table$sent
	receive_savings <- cload_table$wreceived - cload_table$total_received
	
	balance <- send_savings + receive_savings

	return(cbind(cload_table, send_overwork=send_overwork, 
		receive_overwork=receive_overwork, balance=balance))
}

full_load_analysis <- function(load_table, ideal, graph, factors=c("id")) {
	transformed <- combine_load_data(load_table, ideal, factors)
	transformed <- add_overwork_statistics(transformed)
	transformed <- add_degree(transformed, graph)
	return(transformed)
}

sorted_fairness_plot <- function(table, work_col, root_col="root", ...) {
	# Sorts the points.
	alt_roots <- 1:length(table[work_col])
	table <- cbind(table, alt_roots=alt_roots)
	table <- table[order(table$alt_roots),]
	return(fairness_plot(table, work_col, root_col="alt_roots", ...))
}

fairness_plot <- function(table, work_col, root_col="root", bins=200, breaks=NULL, ylimit=NULL) {
	pl <- ggplot(table)
	pl <- pl + stat_binhex(bins=bins, aes_string(x=root_col, y=work_col))
	pl <- pl + scale_fill_gradientn(name="density", colours=heatmap_colours(), breaks=breaks)
	if(!is.null(ylimit)) {
		pl <- pl + ylim(ylimit)
	}
	return(pl)
}

mar <- function(vals) {
	sum(abs(vals - mean(vals)))
}

coeff_var <- function(vals) {
	return(sqrt(var(vals))/mean(vals))
}

norm_mar <- function(vals) {
	return(mar(vals)/mean(vals))
}

###############################################################################
# Latency Analysis
###############################################################################

#
# Gets the residue.
#

res_divide <- function(a, b) {
	return(safe_divide(a,b,0.0))
}

get_residue <- function(a_table) {
	undelivered <- a_table$undelivered
	delivered <- a_table$delivered
	tot <- undelivered + delivered
	return(mapply(res_divide, undelivered, tot))
}

#
# Gets the duplicate ratio.
#
get_dup_ratio <- function(a_table) {
	dups <- a_table$duplicates
	delivered <- a_table$delivered
	return(mapply(safe_divide, dups, delivered))
}

#
# Adds "experiments" column to table.
#
add_experiment_counts <- function(a_table) {
	ones <- c()
	ones[1:length(a_table$id)] <- 1
	return(cbind(a_table, experiments=ones))
}

#
# Computes and adds "t_avg" from latency_sum and delivered.
#
add_latencies <- function(a_table) {
	t_avg <- mapply(safe_divide, a_table$latency_sum, a_table$delivered)
	return(cbind(a_table, t_avg=t_avg))
}

#
# Computes and adds "t_var" from squared_sum, t_avg, and delivered.
#
add_variances <- function(a_table) {
	sq_average <- a_table$t_avg*a_table$t_avg
	n <- a_table$delivered
	t_var <- a_table$squared_sum/n - sq_average
	return(cbind(a_table, t_var=t_var))
}

add_transitivities <- function(a_table, a_graph) {
	transitivities <- transitivity(a_graph, type='localundirected', a_table$id)
	replace <- which(is.nan(transitivities))
	warning(paste("there were",length(replace),"NaN values.",sep=" "))
	transitivities[replace] <- 1.0
	return (cbind(a_table, transitivity=transitivities))
}

#
# Aggregates raw experiment table and creates sum-of-latencies row, 
# as well as aggregate variance.
#
combine_experiments <- function(a_table) {
	transformed <- a_table
	# Adds the latency sums first, if missing.
	if(is.null(a_table$latency_sum)) {
		transformed <- add_simple_sum(a_table, "t_avg", "delivered", "latency_sum")
	}
	# Adds average, if missing.
	if(is.null(a_table$t_avg)) {
		transformed$t_avg <- checked_divide_vector(transformed$latency_sum, transformed$delivered, 0.0)
	}
	# Then the squared sums.
	transformed <- add_squared_sum(transformed, "t_avg", "t_var", "delivered")
	# Then the experiment counts.
	transformed <- add_experiment_counts(transformed)
	# Then the t_max sums.
	transformed <- cbind(transformed, max_latency_sum=transformed$t_max)
	# Finally, aggregates all experiments.
	transformed <- aggregate(transformed[c("latency_sum", "max_latency_sum", "squared_sum", "delivered", "undelivered", "duplicates", "experiments")],
	list(id=transformed$id, degree=transformed$degree), sum)
	# Now computes the variances and averages again, as well as average tmax.
	transformed <- add_latencies(transformed)
	transformed <- add_variances(transformed)

	t_max <- transformed$max_latency_sum / transformed$experiments
	transformed <- cbind(transformed, t_max=t_max)

	return(transformed)
}

global_variance <- function(a_table) {
	average <- global_latency_average(a_table)
	sq_sums <- sum(a_table$squared_sum)
	n <- sum(a_table$delivered)
	sq_average <- average*average
	variance <- sq_sums/n - sq_average
	return(variance)
}

global_latency_average <- function(a_table) {
	summation <- sum(a_table$latency_sum)
	n <- sum(a_table$delivered)
	return(summation/n)
}

global_average_of_maximums <- function(a_table) {
	summation <- sum(a_table$max_latency_sum)
	n <- sum(a_table$experiments)
	return(summation/n)	
}

# Not very reusable.
full_processing <- function(start, end, name_generator, graph) {
	a_table <- loadtables(start, end, name_generator)
	return(table_processing(a_table, graph))
}

table_processing <- function(a_table, graph) {
	a_table <- add_delivered(a_table)
	a_table <- combine_experiments(a_table)
	if (!is.null(graph)) {
		a_table <- add_transitivities(a_table, graph)
	}
	a_table <- a_table[order(a_table$degree),]

	return(a_table)
}

aggregates <- function(a_table, graph) {
	a_table <- table_processing(a_table, graph)
	
}

###############################################################################
# Printing metrics.
###############################################################################
print_percentiles <- function(name, vec, percentiles) {
	values <- quantile(vec, percentiles)
	for(i in 1:length(percentiles)) {
		percentil <- percentiles[i]*100
		cat(paste(percentil, "th percentile", name, values[i], "\n"))
	}
}



