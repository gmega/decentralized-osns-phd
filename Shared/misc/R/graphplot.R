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
	extend(Object(), "LinePlotter",
			.first = TRUE,
			.pars = ...,
			.lty = 1,
			.colorscale = colorscale(colorcycle)
	)
});

setMethodS3("doPlot", "LinePlotter", function(this, formula, ...) {
	color <- this$.color()
	style <- this$.lty
	this$.lty <- this$.lty + 1
	
	if(this$.first){
		plot(formula, lty = style, type = "l", col=color, this$.pars, ...)
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