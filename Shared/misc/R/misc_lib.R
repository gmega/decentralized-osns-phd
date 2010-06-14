##############################################################
#
# Tries to fit a data set to a power law and then plot the two
# together.
#
##############################################################
plot_integer_powerlaw <- function(dataset, xmin=NULL) {
   # Begins by fitting the power law. 
   alpha <- as.double(attributes(power.law.fit(dataset, xmin))$fullcoef)
   pow_lay <- function(x) { x^(-alpha) }
   
   # Now plots the frequency.
   frequencies <- table(dataset)
   values <- lapply(attributes(frequencies)$dimnames, FUN=as.integer)$dataset
   counts <- as.integer(frequencies)
   
   plot(counts ~ values, ylabel="Frequency", xlabel="Value")
   
   curve(pow_lay, from=xmin, add=TRUE)
}

##############################################################
#
# Plots a simple, bin-1-width histogram. Based on the plot
# function, allows log-log and log-lin.
#
# Works by rounding the input vector and feeding it to
# tabulate.
#
##############################################################
plot_simple_histogram <- function(x, file_name=NULL, log="", real_zero=FALSE,...) {

  # Sums one into the data set, as tabulate doesn't count the
  # zeroes.
  rounded_x <- round(x)
  rounded_x <- rounded_x + 1
  tabulated <- tabulate(rounded_x)

  print(tabulated[1])

  # Creates a corrected index list to account for the sum we
  # made.
  indexes <- seq(min(rounded_x) - 1, max(rounded_x) - 1)

  # Pre-processes the data.
  if (length(log) && log != ""){
    logspec <- strsplit(log, NULL)[[1L]]

    if ("y" %in% logspec)
      tabulated = tabulated + 1.0
    
    if ("x" %in% logspec)
      indexes <- seq(min(rounded_x), max(rounded_x))
  }
 
  if (!is.null(file_name)) {
    postscript(file_name)
  }
  
  plot(tabulated ~ indexes,log=log,...)

  if (real_zero){
    abline(a=0, b=0, lty=2,lwd=2)
  }

  if(!is.null(file_name)){
    dev.off()
  }
}

##############################################################
#
# Plots a log-lin density histogram. Allows the number of
# breaks to be specified.
#
##############################################################

custom_log_histogram <- function (x, breaks = "Sturges", include.lowest = TRUE, right = TRUE, 
    main = paste("Log-Histogram of", xName), xlim = range(breaks), 
    ylim = NULL, xlab = xName, ylab = "Log-density", nclass = NULL, 
    htype = "b", xbreaks = NULL, ...) 
{
    xName <- paste(deparse(substitute(x), 500), collapse = "\n")
		if (!missing(xbreaks))
	    histInfor <- hist.default(x, plot = FALSE, breaks = xbreaks)
		else 
			histInfor <- hist.default(x, plot = FALSE)
    logDensity <- log(histInfor$density)
    breaks <- histInfor$breaks
    mids <- histInfor$mids
    nB <- length(breaks)
    counts <- histInfor$counts
    height <- range(logDensity, finite = TRUE)[2] - range(logDensity, 
        finite = TRUE)[1]
    base <- min(logDensity[is.finite(logDensity)]) - 0.25 * height
    yMax <- 0.25 * abs(max(logDensity)) + max(logDensity)
    if (is.null(ylim)) 
        ylim <- range(base, yMax)
    plot(mids, logDensity, xlim = xlim, ylim = ylim, type = "n", 
        xlab = xlab, ylab = ylab, main = main, ...)
    if (htype == "b" || htype == "p") {
        points(mids, logDensity, ...)
    }
    heights <- rep(0, nB)
    for (j in 2:(nB - 1)) {
        if (is.finite(max(logDensity[j - 1], logDensity[j]))) {
            heights[j] <- max(logDensity[j - 1], logDensity[j])
        }
        else {
            heights[j] <- NA
        }
    }
    heights[1] <- ifelse(is.finite(logDensity[1]), logDensity[1], 
        NA)
    heights[nB] <- ifelse(is.finite(logDensity[nB - 1]), logDensity[nB - 
        1], NA)
    if (htype == "b" || htype == "h") {
        i <- 1:(nB)
        segments(breaks[i], logDensity[i], breaks[i + 1], logDensity[i])
        segments(breaks[i], heights[i], breaks[i], base, lty = 2)
        segments(breaks[nB], heights[nB], breaks[nB], base, lty = 2)
    }
    r <- list(breaks = breaks, counts = counts, logDensity = logDensity, 
        mids = mids, xName = xName, heights = heights, ylim = ylim)
    invisible(r)
}

##############################################################
#
# Computes our standard set of statistics.
#
##############################################################
std_stats <- function (vector) {
   dt <- mean(vector)
   mn <- min(vector)
   mx <- max(vector)
   std <- sqrt(var(vector))
   pr <- quantile(vector, c(0.9))

   return (data.frame(c(mn, mx, dt, std, pr), row.names=c("minimum", "maximum", "average", "stdandard deviation", "90 percentile")))
}

##############################################################
#
# Plots pre-formatted metric graphs.
#
##############################################################
metric_hist <- function(x, measure, algorithm, file_name=NULL, type="h", log="", real_zero=FALSE,...) {
  logspec <- strsplit(log, NULL)[[1L]]
  
  hist_type <- ""
  xaxis <- measure
  yaxis <- "frequency"
    
  if ("y" %in% logspec) {
    hist_type <- "Log"
    yaxis <- paste(yaxis," ", "(log)", sep="")
  } else {
    hist_type <- "Lin"
  }

  if ("x" %in% logspec) {
    hist_type <- paste(hist_type,"log", sep="-")
    xaxis <- paste(xaxis," ", "(log)", sep="")
  } else {
    hist_type <- paste(hist_type,"lin", sep="-")
  }

  hist_type <- paste(hist_type," ",measure," histogram (",algorithm,")", sep="")
  plot_simple_histogram(x, file_name=file_name, type=type, log=log, real_zero=real_zero, ylab=yaxis, xlab=xaxis, main=hist_type,...)
}
                
                
