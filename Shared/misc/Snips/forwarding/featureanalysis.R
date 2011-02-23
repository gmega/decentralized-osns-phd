dem_lat0.1 <- add_simple_sum(dem_lat0.1, avg_row="t_avg", n_row="delivered", sum_row="latency_sum")
dem_lat0.2 <- add_simple_sum(dem_lat0.2, avg_row="t_avg", n_row="delivered", sum_row="latency_sum")
dem_lat0.3 <- add_simple_sum(dem_lat0.3, avg_row="t_avg", n_row="delivered", sum_row="latency_sum")
dem_lat0.4 <- add_simple_sum(dem_lat0.4, avg_row="t_avg", n_row="delivered", sum_row="latency_sum")
 
lat_tables <- list(dem_lat0.1, dem_lat0.2, dem_lat0.3, dem_lat0.4, fwd_c_lat, fwd_rnd_lat, fwd_ac_lat, fwd_bal_lat)
fwd_bal_lat <- add_simple_sum(fwd_bal_lat, avg_row="t_avg", n_row="delivered", sum_row="latency_sum")

plot(ecdf(dem_lat0.1$t_avg)[1:len]

# -----------------------------------------------------------------------------
# Plots correlations with average latency.
# -----------------------------------------------------------------------------
plot_cor <- function(table, x=800, y=0.6, pos=NULL, lwd=2) {
	max_deg <- max(table$degree)
	table <- table[order(table$id),]
	corr <- sapply(1:1500, function(x) { return(cor(table$t_avg[table$degree > x], fbego$communities[fbego$degree > x])) })
	corr2 <- sapply(1:1500, function(x) { return(cor(table$t_avg[table$degree > x], conn$components[conn$degree > x])) })
	corr3 <- sapply(1:1500, function(x) { return(cor(table$t_avg[table$degree > x], trans[which(table$degree > x)])) })
	corr4 <- sapply(1:1500, function(x) { return(cor(table$t_avg[table$degree > x], table$degree[(table$degree > x)])) })

	plot(corr, type="l", xlab="neighborhoods larger than", ylab="correlation", xlim=c(0,max_deg), ylim=c(-1,1), lwd=lwd)
	abline(h=0.5, lty=2, col="gray")
	abline(h=-0.5, lty=2, col="gray")
	lines(corr2, col="red", lwd=lwd, lty=2)
	lines(corr3*-1, col="green", lwd=lwd, lty=3)
	lines(corr4, col="magenta", lwd=lwd, lty=4)

	leg_names <- c("communities (greedy modularity)", "fragmentation", "clustering", "degree")
	leg_colors <- c("black", "red", "green", "magenta")
	leg_shapes <- c(1:4)

	if(!is.null(pos)) {
		legend(pos, leg_names, col=leg_colors, lty=leg_shapes, lwd=lwd, bty="y")
	} else {
		legend(x=x, y=y, leg_names, col=leg_colors, lty=leg_shapes, lwd=lwd, bty="y")
	}
}

# -----------------------------------------------------------------------------
# Computes a set of aggregates by successively filtering out points taking by 
# reference a feature vector and a set of feature vector breaks.
# -----------------------------------------------------------------------------

aggregate_by_featurevalue <- function(table, avg_function, featurebreaks, featurevector) {
	return(sapply(featurebreaks, function(x) { return(avg_function(table[which(featurevector >= x),])) }));
}

# -----------------------------------------------------------------------------
#
#
# -----------------------------------------------------------------------------
plot_comparison_by_feature <- function(tables, featurebreaks, featurevector, colors, aggregator=global_latency_average, xlab=NULL, ylab=NULL, lwd=2, lnames=NULL, lpos="bottomright", ylim=NULL,...) {
	datasets <- sapply(tables, function(x) { return (aggregate_by_featurevalue(x, aggregator, featurebreaks, featurevector)) })
	# Finds the maximum
	if (is.null(ylim)) {
		coalesced <- c(datasets)
		coalesced <- coalesced[!is.nan(coalesced)]
		maximum <- max(coalesced)
		minimum <- min(coalesced)
		ylim=c(minimum, maximum)	
	}
	rm(coalesced)
	# First calls plot.
	first_set <- datasets[,1]
	plot(first_set ~ featurebreaks, type="l", xlab=xlab, ylab=ylab, col=colors[1], lwd=lwd, lty=1, ylim=ylim,...)
	# Then lines.
	for(i in 2:length(tables)) {
		lines(datasets[,i] ~ featurebreaks, col=colors[i], lwd=lwd, lty=i)
	}

	if(!is.null(lnames)) {
		if(!is.character(lpos)) {
			x <- lpos[1]
			y <- lpos[2]
		} else {
			x <- lpos
			y <- NULL
		}
		legend(x, y, lnames, col=colors, lwd=lwd, lty=1:length(tables), bty="n")
	}
}

m <- c(seg_avg_con, seg_avg_con2, seg_avg_con3)
m <- m[!is.nan(m)]
plot(seg_avg_con, type="l", lty=2, lwd=2, col="red", xlab=expression(paste("neighborhoods with ",kappa[comm],"(A) > x")), ylab="average latency", ylim=c(0, max(m)))
# plot(seg_avg_con, type="l", lty=2, lwd=2, col="red", xlab=expression(paste("neighborhoods with ",kappa,"(A) > x")), ylab="average latency", ylim=c(0, max(m)))
rm(m)
lines(seg_avg_con2, type="l", lty=3, lwd=2, col="purple")
lines(seg_avg_con3, type="l", lty=3, lwd=2, col="blue")
legend("bottomright", c("anticentrality", "components/clustering/random", "components/size/random"), col=c("red", "blue", "purple"), lty=c(1:3), lwd=2, bty="n")

