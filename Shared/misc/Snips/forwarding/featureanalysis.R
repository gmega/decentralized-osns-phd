library(ggplot2)
library(multicore)
library(R.oo)

# -----------------------------------------------------------------------------
# Plots correlations with average latency.
# -----------------------------------------------------------------------------

plot_cor <- function(table, x=800, y=0.6, pos=NULL, lwd=2, bty="n") {
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




