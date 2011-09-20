parstring <- function(fname) {
	p1 <- gsub(".*?-(.*)(\\..*)", replacement="\\1", fname, perl=TRUE);
	p2 <- gsub("-", replacement="\\.", p1)
	return(p2)
}

as_table <- function(envir) {
	large_table <- NULL
	for(key in ls(envir)) {
		print("Append")
		tbl <- get(key, envir)
		len <- dim(tbl)[1]
		new_col <- c()
		new_col[1:len] <- key
		tbl <- cbind(tbl, cat=new_col)
		if (is.null(large_table)) {
			large_table <- tbl	
		} else {
			large_table <- rbind(large_table, tbl)	
		}
	}
	return(large_table)
}

# -----------------------------------------------------------------------------
# Generic driver for batch processing.
# -----------------------------------------------------------------------------
run <- function(processor) {
	inputs <- Sys.getenv("INPUT_FILE")
	pat <- Sys.getenv("FILE_PAT")
	if (inputs == "") {
		inputs <- dir(pattern=pat)
	} else {
		inputs <- list(inputs)
	}

	print(inputs)

	for (input in inputs) {
		processor(parstring(input), input)
	}
}