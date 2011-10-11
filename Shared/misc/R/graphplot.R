library(igraph)
library(R.oo)

plot_neighborhood <- function(g, root_id) {
	vertices <- c(root_id, neighbors(g, root_id))
	subg <- subgraph(g, vertices)
	tkplot(subg)
}

# Line plotting util which calls plot, then lines.
#
###############################################################################

setConstructorS3("LinePlotter", function(colorscale = NULL, colorcycle=20, ...) {
	
	pars <- match.call()
	pars$colorscale <- pars$colorcycle <- NULL
	
	extend(Object(), "LinePlotter",
			.first = TRUE,
			.pars = pars,
			.lty = 1,
			.colorscale = colorscale(colorcycle)
	)
});

setMethodS3("doPlot", "LinePlotter", function(this, formula, ...) {
	color <- this$.color()
	style <- this$.lty
	this$.lty <- this$.lty + 1
	
	if(this$.first){
		# Sets up the plot call.
		this$.pars$lty <- style
		this$.pars$col <- color
		this$.pars$type <- "l"
		this$.pars$x <- formula		
		this$.pars[[1]] <- as.name("plot")
		
		# And now performs it.
		eval(this$.pars, sys.frame(sys.parent()))
		
		# Done, all subsequent calls will be to lines.
		this$.first=FALSE
	} else {
		lines(formula, lty = style, col=color, ...)
	}
});

setMethodS3("clear", "LinePlotter", function(this) {
	this$.first = TRUE
	this$.lty = 1
})

setMethodS3(".color", "LinePlotter", function(this) {
	if (is.null(this$.colorscale)) {
		return("black")
	}
	return(this$.colorscale[((this$.lty - 1 ) %% length(this$.colorscale)) + 1])
})