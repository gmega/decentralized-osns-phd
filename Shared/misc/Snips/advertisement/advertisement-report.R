# Scripts for plotting graphs which were used in Group Membership Report
# "Advertisement" 
#
# Author: giuliano
###############################################################################

compute_max <- function(envir, field) {
	mx <- -Inf
	for (key in ls(envir)) {
		object <- get(key, envir=envir)
		mx <- max(mx, object[,c(field)]) 
	}
	return(mx)
}


#
# Generates seen graphs.
###############################################################################

all_seen <- function(envir, samples, ...) {
	ymax <- compute_max(envir, "seen")
	xmax <- compute_max(envir, "time")

	lplotter <- LinePlotter(colorscale=colorRampPalette(colors=c("red", "green", "blue"), space="rgb"), 
			colorcycle=length(ls(envir)), lwd=2)
		
	for (sample_key in ls(envir)) {
		smple <- get(sample_key, envir)
		lplotter$doPlot(smple$seen ~ smple$time, xlab="Time", ylab="Seen", lwd=2,xlim=c(0,xmax), ylim=c(0,ymax), ...)
	}	
}

all_unseen <- function(envir) {
	par(mfrow=c(length(ls(envir))/2 + 1,2))

	xmax <- compute_max(envir, "time")
	
	for (sample_key in ls(envir)) {
		smple <- get(sample_key, envir)
		plot(smple$unseen ~ smple$time, xlab="Time", ylab="Unseen", xlim=c(0,xmax), cex=0.1, main=sample_key)	
	}
}

all_txbw <- function(envir) {
	par(mfrow=c(length(ls(envir))/2 + 1,2))
	
	for (sample_key in ls(envir)) {
		smple <- get(sample_key, envir)
		plot(smple$max_txbytes, xlab="Time", ylab="Tx Bdw (bytes/round)", cex=0.1, main=sample_key)	
	}
}
all_rbw <- function(envir) {
	par(mfrow=c(length(ls(envir))/2 + 1,2))
	
	for (sample_key in ls(envir)) {
		smple <- get(sample_key, envir)
		plot(smple$max_rxbytes, xlab="Time", ylab="Rx Bdw (bytes/round)", cex=0.1, main=sample_key)	
	}
}
