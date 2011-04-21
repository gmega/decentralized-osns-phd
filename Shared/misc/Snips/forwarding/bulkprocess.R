# -----------------------------------------------------------------------------
# Prefixes all entries in an environment with the provided string prefix.
# -----------------------------------------------------------------------------
prefixenv <- function(envir, prefix) {
	for (key in ls(envir)) {
		nkey <- paste(prefix,key,sep="")
		oldie <- get(key, envir)
		assign(nkey, oldie, envir=envir)
		remove(list=c(key), envir=envir)
	}
}

# -----------------------------------------------------------------------------
# Merges a list of environments in a new environment.
# -----------------------------------------------------------------------------
mergenv <- function(envs) {
	merged <- new.env()
	for (envir in envs) {
		for (key in ls(envir)) {
			assign(key, get(key, envir=envir), envir=merged)
		}
	}
	return(merged)
}

# -----------------------------------------------------------------------------
# Returns all objects in an environment whose keys satisfy a given pattern
# (as opposed to ls() which returns only the keys). 
# -----------------------------------------------------------------------------
all_in <- function(envir, pat=NULL) {
	tbl_list = list()
	for (key in ls(envir)) {
		do_append <- TRUE
		if (!is.null(pat)) {
			do_append <- length(grep(key, pattern=pat))
		}
		if (do_append) {
			tbl_list <- append(tbl_list, list(get(key, envir)))
		}
	}
	return(tbl_list)
}

# -----------------------------------------------------------------------------
# Applies a function to all objects in an environment, and replaces the 
# originals with the processed version. Careful as, if it fails midway through,
# the contents of the environment might be corrupted.
# -----------------------------------------------------------------------------
envapply <- function(envir, fun) {
	for (key in ls(envir)) {
		assign(key, fun(get(key, envir)), env=envir)
	}
}

# -----------------------------------------------------------------------------
# Loads all files from a given folder into an environment.
# -----------------------------------------------------------------------------
load_all <- function(path="./", pattern=NULL, naming=function(x) { return(x); }, envir=environment()) {
	for(i in dir(path, pattern=pattern)) {
		assign(naming(i), read.table(paste(path,i,sep=""), header=TRUE), env=envir)
		cat(paste("Read ",i,".\n", sep=""))
	}
	return(envir)
}

parstring <- function(fname) {
	p1 <- gsub(".*?-(.*)(\\..*)", replacement="\\1", fname, perl=TRUE);
	p2 <- gsub("-", replacement="\\.", p1)
	return(p2)
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