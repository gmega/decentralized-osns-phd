direct_mailing_table <- function(degs, experiments = NULL) {
	max_idx <- length(degs)
	
	if (is.null(experiments)) {
		experiments <- c()
		experiments[1:max_idx] <- 1.0
	}

	# ID vector
	ids <- seq(0, max_idx - 1)
		
	# Direct mailing delivers everything.
	undelivered <- c()
	undelivered[1:max_idx] <- 0
	delivered <- degs * experiments
	
	# Latency.
	t_max <- degs - 1
	t_avg <- sapply(degs, direct_mailing_latency)
	t_var <- sapply(degs, direct_mailing_variance)
	latency_sum <- sapply(degs, direct_mailing_latency_sum)
	
	# -- Note that average and variance are insensitive to
	#    repetitions for direct mailing.
	latency_sum <- latency_sum * experiments
	t_max_sum <- t_max * experiments
	
	# Direct mailing generates zero duplicates.
	duplicates <- undelivered
	
	return(data.frame(id=ids, 
					degree=degs, 
					undelivered=undelivered,
					delivered=delivered,
					duplicates=duplicates,
					t_var=t_var,
					t_max_sum=t_max_sum,
					t_avg=t_avg,
					t_max=t_max,
					latency_sum=latency_sum,
					experiments=experiments))
}

direct_mailing_latency_sum <- function(degree) {
	return ((degree)*(degree - 1)/2.0)
}

direct_mailing_latency <- function(degree) {
	return((degree - 1)/2.0)
}

direct_mailing_variance <- function(d) {
	if (d == 1) {
		return(0.0)
	}
	mu <- direct_mailing_latency(d)
	a <- d*(2*d - 1)/6.0
	b <- (d*mu^2)/(d - 1.0)
	c <- mu*d
	return (a + b - c)
}

direct_mailing_load <- function(degs) {
	id <- 1:length(degs) - 1
	sent <- degs
	received <- degs
	total <- sent + received
	disseminated <- degs
	updates <- degs
	
	dups_generated <- c()
	dups_generated[1:length(degs)] <- 0
	dups_received <- c()
	dups_received[1:length(degs)] <- 0
	
	experiments <- degs + 1
	
	return(data.frame(id=id, sent=sent, received=received, 
					total=total, disseminated=disseminated, 
					updates=updates, dups_generated = dups_generated, 
					dups_received=dups_received, experiments=experiments))
}

direct_mailing_fairness <- function(d) {
	mu <- d/(d + 1)
	dvar <- (d - mu)^2 + d*((1 - mu)^2)
	stddev <- sqrt(dvar)
	return(stddev/mu)
}