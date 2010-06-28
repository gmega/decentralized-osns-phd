#!/usr/bin/Rscript --vanilla 
                                     
# Imports.
library(getopt)

# Sources in our common files.
rlibhome <- Sys.getenv("RLIB_HOME", unset=NA)
if (is.na(rlibhome)) {
  stop("Environment variable RLIB_HOME was not set.")
} else {
  source(file.path(rlibhome, "common.R"))
}

opt_spec <- matrix(c(
                     'verbose', 'v', 0, "logical", 'verbose mode',
                     'help', 'h', 0, "logical", 'prints this help message',
                     'input', 'i', 1, "character", 'input file (mandatory)',
                     'output', 'o', 1, "character", 'output file',
                     'metric', 'm', 1, "character", 'plotted metric (mandatory)',
                     'algorithm', 'a', 1, "character", 'algorithm name (mandatory)',
                     'logplot', 'l', 1, "character", 'logplot axis (x, y, or xy)'
                     ), ncol=5, byrow=TRUE)

# Parses the command line.
opt = getopt(opt_spec)
if (!is.null(opt$help)) {
  # Help request.
  message(getopt(opt_spec, usage=TRUE))
  q(save="no", status=0)
}

# Checks that the "mandatory options" are not null.
chkmandatory(opt, c("input", "metric", "algorithm"))

# Set the defaults for stuff that wasn't set.
if (is.null(opt$output))  { opt$output = "./output.eps"  }
if (is.null(opt$logplot)) { opt$logplot = "" }
if (is.null(opt$verbose)) { opt$verbose = FALSE }

# Reads the file.
the_data <- read.table(file=opt$input, header=FALSE, sep=" ")

# Plots the data.
metric_hist(the_data$V2, algorithm=opt$algorithm, measure=opt$metric, file_name=opt$output, log=opt$logplot, real_zero=TRUE)

# Prints minimum, maximum, avg, std. dev and 90th percentile.
s <- std_stats(the_data$V2)
s <- paste(s["minimum"], s["maximum"], s["average"], s["standard deviation"], s["90th percentile"])
cat(s)
