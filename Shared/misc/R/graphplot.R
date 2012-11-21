library(igraph)
library(R.oo)


#' Plots a graph 1-hop neighborhood using igraph's tkplot.
#' 
#' @param g the graph in which the neighborhood sits.
#' @param root_id the id of the neighborhood root.
#' @author giuliano
plot_neighborhood <- function(g, root_id) {
	vertices <- c(root_id, neighbors(g, root_id))
	subg <- subgraph(g, vertices)
	tkplot(subg)
}

#
# Plots multiple CDF functions together with a legend.
#
multicdf <- function(datasets, freqsets = NULL, lwd, lty, cdf_fun=ecdf, col, 
		pos = NULL, names = NULL, xlim=NULL, quantiles, quantilep=0.95, 
		round_dec=2, legend.cex=1, marker.col="darkgray", complete_lines = FALSE, ...) {
	
	qs <- c()
	qs[1:length(datasets)] <- 0
	
	# Computes the maximum and minimum.
	if (is.null(xlim)) {
		xmin <- xmax <- NULL
		for (dataset in datasets) {
			if (is.null(xmax)) {
				xmin <- min(dataset)
				xmax <- max(dataset)
			} else {
				xmin <- min(dataset)
				xmax <- max(xmax, dataset)
			}
		}
		
		xlim <- c(xmin, xmax)
		print(paste(xmin," ",xmax))
	}
	
	first <- TRUE
	
	for (i in seq(1, length(datasets))) {
		x <- NULL
		if (is.null(freqsets)) {
			f <- cdf_fun(datasets[[i]])
			x <- knots(f)
			y <- f(x)
		} else {
			x <- datasets[[i]]
			y <- freqsets[[i]]
		}
		
    qs[i] <- round(quantile(datasets[[i]], c(quantilep))[[1]], round_dec)
    
    if (first) {
      plot(y ~ x, lwd=lwd, xlim=xlim, type="l", col=col[i], lty=lty[i], ...)
      if (any(quantiles)) {
				abline(h=c(quantilep), lty=2, lwd=lwd, col=marker.col)
			}
      first <- FALSE
      
    	} else {
      		lines(y ~ x, lwd=lwd, type="l", col=col[i], lty=lty[i])
    	}
		
      if (quantiles[i]) {
        print(qs[i])
        abline(v=c(qs[i]), lwd=lwd, col=marker.col, lty=2)
        axis(3, at=c(qs[i]))
      }
      
      if (complete_lines && (max(x) < xlim[2])) {
        lines(c(max(x), xlim[2]*2), c(1.0, 1.0), lwd=lwd, col=col[i], lty=lty[i])
      }
	}
	
	if (!is.null(names)) {
	  xpos <- pos[1]
	  ypos <- NULL
    if (length(pos) > 1) {
      ypos <- pos[2]
    }
	
		legend(x=xpos, y=ypos, names, lwd=lwd, lty=lty, col=col, bg="white", cex=legend.cex)
	}
	
	return(qs)
}

#' Creates a new multiline plotter.
#' 
#' @param colorscale a function providing a colorscale for the lines.
#' @param colorcycle the number of colors to be generated. These will be 
#' 	circularly reused.
#' @param ... parameters to be passed onto the first call to \code{plot}.
#' @author giuliano
setConstructorS3("LinePlotter", function(colorscale = NULL, colorcycle=20, ...) {
	
	pars <- match.call()
	pars$colorscale <- pars$colorcycle <- NULL
	
	print(pars)
	
	extend(Object(), "LinePlotter",
			.first = TRUE,
			.pars = pars,
			.lty = 1,
			.colorscale = colorscale(colorcycle)
	)
});

setMethodS3("doPlot", "LinePlotter", function(this, ylim, formula, ...) {
	color <- this$.color(this$.lty - 1)
	style <- this$.lty
	this$.lty <- this$.lty + 1
	
	if(this$.first){
		# Sets up the plot call.
		this$.pars$lty <- style
		this$.pars$col <- color
		this$.pars$type <- "l"
	    this$.pars$ylim <- ylim
		
		penvir <- parent.frame()
		
		if (is.formula(formula)) {
			this$.pars$formula <- formula	
			this$.pars[[1]] <- as.name("pfun__")
			penvir$pfun__ <- graphics:::plot.formula
		} else {
			this$.pars$x <- formula
			this$.pars[[1]] <- as.name("plot")
		}
		
		# And now performs it.
		eval(this$.pars, penvir)
		
		# Done, all subsequent calls will be to lines.
		this$.first=FALSE
	} else {
		lines(formula, lty = style, col = color, ...)
	}
});

setMethodS3("legends", "LinePlotter", function(this, x, y = NULL, names, ...) {
	colors <- sapply(0:(this$.lty - 1), this$.color)
	print(colors)
	legend(x, y, names, col=colors, lty=1:(this$.lty), bty="n", ...)
});

setMethodS3("clear", "LinePlotter", function(this) {
	this$.first = TRUE
	this$.lty = 1
})

setMethodS3(".color", "LinePlotter", function(this, index) {
	print(index)
	if (is.null(this$.colorscale)) {
		return("black")
	}
	return(this$.colorscale[(index %% length(this$.colorscale)) + 1])
})

is.formula <- function(x) {
	return(class(x) == "formula")
}
