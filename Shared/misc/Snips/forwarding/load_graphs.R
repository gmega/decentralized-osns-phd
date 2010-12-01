#!/usr/bin/env Rscript

lib_home <- Sys.getenv("SNIPS_HOME")
graphs_home <- Sys.getenv("GRAPHS_HOME")
source(paste(lib_home, "forwarding/latency-load-snippets.R", sep="/"))

args <- commandArgs(TRUE)
input_file <- args[1]
filename <- args[2]
main <- args[3]
width <- as.numeric(args[4])
height <- as.numeric(args[5])

ldt <- read.table(input_file, header=TRUE)
sent_norm <- ldt$sent/ldt$points
recv_norm <- ldt$received/ldt$points
tot_norm <- sent_norm + recv_norm

if (length(args) == 1) {
	tot_avg <- (sum(ldt$sent) + sum(ldt$received))/sum(ldt$points)
	tot_max <- max(tot_norm)
	tot_var <- sqrt(var(tot_norm))
	print(paste("Max.", tot_max))
	print(paste("Avg.", tot_var))
	print(paste("Std. dev:", sqrt(tot_var)))
	percs <- quantile(tot_norm, c(0.9, 0.95, 0.99))
	print(paste("90th:", as.numeric(percs[1])))
	print(paste("95th:", as.numeric(percs[2])))
	print(paste("99th:", as.numeric(percs[3])))
	quit()
}


print(paste("PNG width is", width))
print(paste("PNG height is", height))

if (length(args) > 5) {
	ylimsent <- as.numeric(args[6])
	ylimrec <- as.numeric(args[7])
	ylimtot <- as.numeric(args[8])
} else {
	ylimsent <- max(sent_norm)
	ylimrec <- max(recv_norm)
	ylimtot <- max(tot_norm)
}



sent <- paste(filename,"-sent.png", sep="")
received <- paste(filename,"-received.png", sep="")
total <- paste(filename,"-total.png", sep="")

png(filename=sent, width=width, height=height)
par(cex.axis=1.8)
plot(sent_norm ~ ldt$degree, xlab="degree (log)", ylab="avg. overhead per friend (sent)", cex=0.4, log="x", ylim=c(0, ylimsent), cex.axis=1.8, cex.lab=1.8)
mx <- round(max(sent_norm))
mn <- round(min(sent_norm))
abline(h=mx, lwd=2, lty=2, col="gray")
abline(h=mn, lwd=2, lty=2, col="gray")
axis(side=4, at = c(mn, mx), cex=1.8)
legend("topleft", c(main), bty="n", cex=1.8)
dev.off()

png(filename=received, width=width, height=height)
par(cex.axis=1.8)
plot(recv_norm ~ ldt$degree, xlab="degree (log)", ylab="avg. cost per friend (received)", cex=0.4,  log="x", ylim=c(0, ylimrec), cex.axis=1.8, cex.lab=1.8)
mx <- round(max(recv_norm))
mn <- round(min(recv_norm))
abline(h=mx, lwd=2, lty=2, col="gray")
abline(h=mn, lwd=2, lty=2, col="gray")
axis(side=4, at = c(mn, mx), cex=1.8)
legend("topleft", c(main), bty="n", cex=1.8)
dev.off()

png(filename=total, width=width, height=height)
par(cex.axis=1.8)
plot(tot_norm ~ ldt$degree, xlab="degree (log)", ylab="avg. cost per friend", cex=0.4, log="x", ylim=c(0, ylimtot), cex.axis=1.8, cex.lab=1.8)
mx <- round(max(tot_norm))
mn <- round(min(tot_norm))
abline(h=mx, lwd=2, lty=2, col="gray")
abline(h=mn, lwd=2, lty=2, col="gray")
axis(side=4, at = c(mn, mx), cex=1.8)
legend("topleft", c(main), bty="n", cex=1.8)
dev.off()

