library(R.oo)
library(ggplot2)
library(multicore)

# -----------------------------------------------------------------------------
# Pareto experiments.
# -----------------------------------------------------------------------------

churnsetting <- function(alpha, beta, dist_alpha, 
		fb = function(x, beta) { return (x*beta) }) {
	
	return(function(m, n) {
		li <- pareto(alpha, beta, m)
		samps <- c()
		samps[1:(m*n)] <- 0
		for (i in 1:m) {
			samps[((i - 1)*n + 1):(i*n)] <- pareto(dist_alpha, fb(li[i], beta), n)
		}
		return(samps)
	});
}


# -----------------------------------------------------------------------------
# Plotting.
# -----------------------------------------------------------------------------

png_mass_plot <- function(all_tables, degs, prefix, suffixes, titles, w, h, yl) {
	for(i in 1:length(all_tables)) {
		table <- all_tables[[i]]

		# Variances.p
		filename <- paste(prefix,suffixes[i],"-var",".png",sep="")
		print(filename)
		png(filename, width=w, height=h)
		print(plot_fairness(compute_plot_fairness(table, degs, "var"), title=titles[i], yl=yl, ylb="Variance"))
		dev.off()

		# Coefficient of variation.
		filename <- paste(prefix,suffixes[i],"-cv",".png",sep="")
		print(filename)
		png(filename, width=w, height=h)
		print(plot_fairness(compute_plot_fairness(table, degs, "cv"), title=titles[i], yl=c(0,20), ylb="Coefficient of Variation"))
		dev.off()
	}
}

# Computes data for fairness plot.
compute_plot_fairness <- function(tbl, degs, stat="var") {
	sendstats <- fairness_stats(tbl, "sent", "root")
	recvstats <- fairness_stats(tbl, "received", "root")
	sendstats <- sendstats[order(sendstats$root),]
	recvstats <- recvstats[order(sendstats$root),]
	all <- data.frame(root=sendstats$root, degree=degs$degree, sent=sendstats[,c(stat)], recv=recvstats[,c(stat)])
	return(all)
}

# Fairness plots separating sends and receives.
plot_fairness <- function(plot_data, title=NULL, alph=0.4, xl=NULL, yl=NULL, ylb="Value") {
	d <- ggplot(plot_data) +
	geom_point(aes(x=degree, y=sent, colour="Sent"), size=1) + 
	geom_point(aes(x=degree, y=recv, colour="Received"), size=1) +
	coord_trans(xtrans="log1p", ytrans="log1p") +
	scale_colour_manual(name="Type", c("Sent"=alpha("Black", alph), "Received"=alpha("Red",alph))) + 
	xlab("Degree") + ylab(ylb)
	
	d <- add_title(add_lim(add_lim(d, ylim, yl), xlim, xl), title)

	return(d)
}

# Neighborhood plot of messages sent by participant within unit experiment.
plot_sent <- function(tbl, root) {
	sub <- tbl[tbl$root == root,]
	sub <- cbind(sub, idx=1:length(sub$id))
	root_row <- sub[sub$id == root,]
	mn <- data.frame(mn=c(mean(sub$sent)))
	d <- ggplot() + 
	geom_point(data=sub, aes(x=idx, y=sent, colour="Friends")) + 
	geom_point(data=root_row, aes(x=idx, y=sent, colour="Root"), size=6) + 
	geom_hline(data=mn, aes(yintercept=mn, colour="Average"), size=1.5) + 
	scale_colour_manual(name="Type", c("Friends"="Black", "Root"=alpha("Red", 0.6), "Average"=alpha("Orange", 0.6))) + 
	xlab("Node Index") + ylab("Messages Sent")
	return(d)
}

# Plots sent by root + direct mailing.
plot_savings <- function(worktbl, degs, experiments, stat="sent") {
	
	print(max(worktbl[[stat]]))
	
	# Direct mailing line (reference).
	dm <- curve_data(sort(unique(degs$degree)))
	
	pstat <- simple_cap(stat)
	colors <- c()
	colors[[pstat]] <- alpha("black", 0.6)
	colors[["Direct Mailing"]] <- alpha("red", 0.6)
	
	worktbl$pstat <- pstat
	
	geom_point(data=worktbl, aes(x=degree, y=stat, colour=pstat), size = 1) + 
	d <- ggplot() +
	geom_line(data=degs, aes(x=degree, y=degree, colour="Direct Mailing"), lwd=1, lty=2) + 
	scale_colour_manual(name="Type", colors) +
	xlab("Degree (log)") + ylab(paste("Messages ",pstat,sep="")) + ylim(c(0, max(rwork[[stat]]))) + coord_trans(xtrans="log1p")

	return(d)			
}

# Plots neighborhood using igraph.
plot_neighborhood <- function(graph, root) {
	components <- neighbors(graph, root)
	subg <- subgraph(graph, c(root, components))
	tkplot(subg)
}

# -----------------------------------------------------------------------------
# Aggregates plotting.
# -----------------------------------------------------------------------------

send_receive_scatterplot <- function(srdata, degs, alph=0.4, title=NULL, xl=NULL, yl=NULL, log=TRUE) {

	d <- ggplot(data.frame(id=srdata$id, sent=srdata$sent, recv=srdata$recv, degree=degs$degree)) +
		geom_point(aes(x=degree, y=sent, colour="Sent"), size=1) + 
		geom_point(aes(x=degree, y=recv, colour="Received"), size=1) +
		scale_colour_manual(name="Type", c("Sent"=alpha("Black", alph), "Received"=alpha("Red",alph))) + 
		xlab("Degree") + ylab("Value")

	d <- add_log(add_title(add_lim(add_lim(d, ylim, yl), xlim, xl), title), log)

	return(d)		
}

add_log <- function(oplot, add_log) {
	if(add_log) {
		oplot <- oplot + coord_trans(xtrans="log1p", ytrans="log1p")
	}
	return(oplot)
}

add_title <- function(oplot, title=NULL) {
	if (!is.null(title)) {
		oplot <- oplot + opts(title=title)
	}
	return(oplot)
}

add_lim <- function(oplot, lim, value=NULL) {
	if (!is.null(value)) {
		oplot <- oplot + lim(value)
	}
	return(oplot)
}

# -----------------------------------------------------------------------------
# Computation.
# -----------------------------------------------------------------------------

total_work_stats <- function(ltable, experiments=1) {
	sent <- parallel(by_id(ltable, "sent", sum))
	recv <- parallel(by_id(ltable, "received", sum))
	results <- collect(list(sent, recv), wait=TRUE)

	# Pulls out the results.
	sent <- results[[1]]
	recv <- results[[2]]
	
	sent <- sent
	recv <- recv
	
	return(data.frame(id=sent$id, sent=sent$sent/experiments, recv=recv$received/experiments))
}

saved <- function(ltable, degs, experiments=1) {
	degree <- degs$degree
	sent <- ltable[which(ltable$id == ltable$root),]
	sent <- sent[order(sent$id),]
	sent$sent <- sent$sent/experiments
	return(data.frame(id=sent$id, saved=(degree - sent$sent)))
}

spent <- function(ltable, degs, experiments=1) {
	wstats <- total_work_stats(ltable, experiments)
	wstats <- wstats[order(wstats$id),]
	return(data.frame(id=wstats$id, spent=((wstats$recv + wstats$sent) - degs$degree)))
}

balance <- function(saved, spent) {
	return(data.frame(id=saved$id, balance=checked_divide_vector(spent$spent, saved$saved, 1)))
}

gpbalance <- function(ltable, degs, experiments) {
	root_sent <- ltable[which(ltable$id == ltable$root),]
	root_sent <- root_sent[order(root_sent$id),]
	ids <- root_sent$id
	root_sent <- root_sent$sent/experiments
	
	tot_work <- total_work_stats(ltable, experiments)
	tot_work <- tot_work[order(tot_work$id),]
	tot_work <- tot_work$sent + tot_work$recv
	
	return(data.frame(id=ids, balance=checked_divide_vector(tot_work,root_sent)))
}

avg_work_stats <- function(ltable, degs, reps) {
	ttw <- total_work_stats(ltable)
	ttw$sent <- ttw$sent/(reps*(degs$degree + 1))
	ttw$recv <- ttw$recv/(reps*(degs$degree + 1))
	return(ttw)
}

root_sent <- function(ltbl, experiments=1) {
	# Aggregates root work.
	rwork <- ltbl[which(ltbl$id == ltbl$root),]
	rwork <- cbind(rwork, degree=degs$degree)
	rwork$stat <- rwork$sent/experiments
}

fairness_stats <- function(lt, selector, grouping) {
	varsp <- parallel(aggregate(lt[,c(selector)], by=list(grouping=lt[,c(grouping)]), var))
	cvp <- parallel(aggregate(lt[,c(selector)], by=list(grouping=lt[,c(grouping)]), coeff_var))
	vars <- collect(varsp, wait=TRUE)[[1]]
	cv <- collect(cvp, wait=TRUE)[[1]]
	m <- data.frame(var=vars$x, cv=cv$x)
	m[c(grouping)] <- vars[c("grouping")]
	return(m)
}

by_id <- function(load, par, aggregator=sum) {
	agg <- aggregate(load[c(par)], by=list(id=load$id), aggregator)
	return(agg)
}

# Linear correlation without NAs.
cornonan <- function(v1, v2) {
	v1_nonan <- v1[!is.nan(v1) & !is.nan(v2)]
	v2_nonan <- v2[!is.nan(v1) & !is.nan(v2)]
	return(cor(v1_nonan,v2_nonan))
}

# Coefficient of variation (for use with sapply).
coeff_var <- function(vals) {
	return(sqrt(var(vals))/mean(vals))
}

# Total work aggregate (if idx refers to degree).
work_aggregate <- function(degs, stat="sent") {
	return(function(wtable, idx) {
		return(sum(wtable[idx, c(stat)])/sum(degs[idx]))
	});
}

