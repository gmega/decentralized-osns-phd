rename_columns <- function(table, old_names, new_names) {
  names <- colnames(table)
  colnames(table)[which(names %in% old_names)] <- new_names
  return(table)
}

max_min <- function(indegs, perc_threshold) {
  # Selects the ids going over the specified percentile threshold
  # for the average in-degree distribution.
  avg_indegs <- aggregate(indegs, list(id=indegs$id), mean)
  threshold <- quantile(avg_indegs$indegree, perc_threshold)
  threshold <- as.numeric(threshold)
 
  # Selects the simulation data only from these nodes.
  avg_indegs <- avg_indegs[avg_indegs$indegree >= threshold,]
  ids <- avg_indegs$id
  filtered <- indegs[which(indegs$id %in% ids),]

  # Computes maximum and minimum.
  max_table <- aggregate(filtered, list(id=filtered$id), max)
  max_table <- rename_columns(max_table, c("indegree"), c("max_indegree"))
  min_table <- aggregate(filtered, list(id=filtered$id), min)
  min_table <- rename_columns(min_table, c("indegree"), c("min_indegree"))

  # Finally, adds the average.
  max_table <- max_table[order(max_table$id),]
  min_table <- min_table[order(min_table$id),]
  avg_indegs <- avg_indegs[order(avg_indegs$id),]
  avg_indegs <- avg_indegs$indegree

  return(cbind(max_table[c("id", "max_indegree")], min_table[c("min_indegree")], avg_indegree = avg_indegs))
}

# Produces a data frame from applying a function to a set of points. Default
# function is a line with slope 1.
curve_data <- function(x, f=linefun(0, 1)) {
	y = lapply(x, f)
	return(data.frame(x=x, y=y))
}

# Returns a line function with specified intercept and slope.
linefun <- function(intercept, slope) {
	return(function(x){
				return(intercept + slope*x)
			})
}

# -----------------------------------------------------------------------------
# Pareto tester.
# -----------------------------------------------------------------------------

betafun <- function(average) {
	linef <- function(alpha) {
		return(average*(alpha - 1.0))
	}
	return(linef)
}

pareto_frame <- function(n, alpha, beta) {
	sp <- ShiftedPareto(alpha, beta)
	dtf <- data.frame(x=sp$sample(n))
	dtf$alpha <- alpha
	dtf$beta <- beta
	return(dtf)
}

pareto_density <- function(framelist) {
	d <- ggplot()
	for(paretoframe in framelist) {
		parstr <- paste("(", paretoframe$alpha[1], ", ", paretoframe$beta[1], ")", sep="")
		paretoframe$k <- parstr
		d <- d + geom_density(data=paretoframe, aes(x=x, color=k))
	}
	return(d)
}

pareto_variances <- function(start, end, alphamin, alphamax, alphastep, ...) {
	print(end-start+1)
	lpl <- LinePlotter(colorscale=colorRampPalette(colors=c("red", "green", "blue"), space="rgb"), colorcycle=(end - start + 1), xlab="Alpha", ylab="Variance", lwd=2)
	for (average in start:end) {
		btf <- betafun(average)
		stepping <- seq(alphamin, alphamax, alphastep)
		variances <- sapply(stepping, function(alpha) {
			pd <- ShiftedPareto(alpha, btf(average))
			return(sqrt(pd$variance())/average)
		})
		print(max(variances))
		lpl$doPlot(variances ~ stepping, lwd=2, ...)
	}
}

# legend("topright", c("1h","2h","3h","4h","5h","6h","7h","8h","9h","10h"), lty=1:10, col=colorRampPalette(colors=c("red","green","blue"), space="rgb")(10), lwd=2)

