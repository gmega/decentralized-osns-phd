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

multicdf <- function(datasets, lwd, pos = NULL, names = NULL, xmin = NULL,
		xmax = NULL, ...) {
	
	# Computes the maximum and minimum.
	for (dataset in datasets) {
		if (is.null(xmax)) {
			xmin <- min(dataset)
			xmax <- max(dataset)
		} else {
			xmin <- min(dataset)
			xmax <- max(xmax, dataset)
		}
	}
	
	xmax <- 10e6
	
	lp <- LinePlotter(colorscale=colorRampPalette(colors=c("red", "green", 
						"blue"), space="rgb"), colorcycle=length(datasets), 
						xlim=c(xmin, xmax), log="x", lwd=lwd, ...)

	for (dataset in datasets) {
		f <- ecdf(dataset)
		x <- knots(f)
		y <- f(x)
		doPlot(lp, y ~ x, lwd=lwd)
	}
	
	if (!is.null(names)) {
		legends(lp, x=pos, names=names, lwd=lwd)
	}
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

setMethodS3("doPlot", "LinePlotter", function(this, formula, ...) {
	color <- this$.color(this$.lty - 1)
	style <- this$.lty
	this$.lty <- this$.lty + 1
	
	if(this$.first){
		# Sets up the plot call.
		this$.pars$lty <- style
		this$.pars$col <- color
		this$.pars$type <- "l"
		
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
		lines(formula, lty = style, col=color, ...)
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