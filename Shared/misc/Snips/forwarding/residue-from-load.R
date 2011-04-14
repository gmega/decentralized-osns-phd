#!/usr/bin/env Rscript

lib_home <- Sys.getenv("RLIB_HOME")
source(paste(lib_home,"common.R",sep="/"))
source("bulkprocess.R")

# ---------------------------------------------------------------------
# Residue from load.
# ---------------------------------------------------------------------

residue <- function(tbl) {
	print(tbl)
	destinations <- tbl[which(tbl$id != tbl$root),]
	undelivered <- which(destinations$received == 0)
	return(length(undelivered) / length(destinations$id))
}

cresidue <- function(tbl) {
	destinations <- tbl[which((tbl$id != tbl$root) & tbl$uptime > 0),]
	undelivered <- which(destinations$received == 0)
	return(length(undelivered) / length(destinations$id))
}

residue_from_load <- function(pars, input) {
	tbl <- read.table(input, header=TRUE)
	res <- residue(tbl)
	cres <- cresidue(tbl)
	cat(paste(" -", pars, res, cres, "\n"))
}

residue_aggregator <- function(tbl, indexes) {
	tbl <- tbl[indexes,]
	undelivered <- tbl$degree - tbl$delivered
	return(sum(undelivered)/sum(tbl$degree))	
}

corr_residue_aggregator <- function(tbl, indexes) {
	tbl <- tbl[indexes,]
	receivers <- tbl$degree - tbl$zero_uptime
	undelivered <- receivers - tbl$delivered
	return(checked_divide(sum(undelivered), sum(receivers), 0.0))
}

dup_ratio_aggregator <- function(tbl, indexes) {
	tbl <- tbl[indexes,]
	return(sum(tbl$dups_received)/sum(tbl$updates))
}

# ---------------------------------------------------------------------

deg_filter <- function(tbl) {
	idx <- tbl$root + 1
	return(degs[idx])
}

residue_plot <- function(envir, pat, resfun, tit, ylim=NULL, bty="n") {
	plot_comparison_by_feature(all_in(envir, pat), featurebreaks=sort(unique(degs)),
	featurevector_extractor=degfilter, colors=colorRampPalette(c("blue", "green", "yellow", "red"),
	bias = 1, space="rgb")(length(all_in(envir))), aggregator=resfun, xlab="Degree",
	ylab=expression(paste("Value nodes with degree ">=" x")), lnames=ls(envir, pattern=pat), 
	lpos=lpos, ylim=ylim, bty=bty, main=tit)
}

# ---------------------------------------------------------------------
# Load plots.
# ---------------------------------------------------------------------

average_load <- function(tbl, degs) {
	tbl <- tbl[order(tbl$id),]
	indexes <- tbl$id + 1
	degs <- degs[indexes]
	avg <- data.frame(id=tbl$id, degree=degs, avg_load=(tbl$total / tbl$experiments))
}

average_load_aggreagator <- function(tbl, indexes) {
	tbl <- tbl[indexes,]
	return((sum(tbl$total)/sum(tbl$experiments)))
}

average_load_plot <- function(loads, breaks=c(200, 400, 800, 1500)) {
	lims <- c(1, max(loads$degree))
	ggplot(loads) + geom_point(aes(x=degree, y=avg_load), size=1, colour=alpha("black", 0.4)) + 
			coord_trans(xtrans="log2") + scale_x_continuous(limits = lims, breaks=breaks) + 
			xlab("Degree (log)") + ylab("Average Load")
}