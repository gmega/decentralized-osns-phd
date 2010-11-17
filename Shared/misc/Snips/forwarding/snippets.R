rename_columns <- function(table, old_names, new_names) {
  names <- colnames(table)
  colnames(table)[which(names %in% old_names)] <- new_names
  return(table)
}

max_min <- function(indegs, perc_threshold) {
  # Selects the ids going over the specified percentile threshold
  # for the average in-degree distribution.
  avg_indegs <- aggregate(indegs, list(id=indegs$id), mean)
  threshold <- quantile(avg_indegs$indegree, perc_threshold)
  threshold <- as.numeric(threshold)
 
  # Selects the simulation data only from these nodes.
  avg_indegs <- avg_indegs[avg_indegs$indegree >= threshold,]
  ids <- avg_indegs$id
  filtered <- indegs[which(indegs$id %in% ids),]

  # Computes maximum and minimum.
  max_table <- aggregate(filtered, list(id=filtered$id), max)
  max_table <- rename_columns(max_table, c("indegree"), c("max_indegree"))
  min_table <- aggregate(filtered, list(id=filtered$id), min)
  min_table <- rename_columns(min_table, c("indegree"), c("min_indegree"))

  # Finally, adds the average.
  max_table <- max_table[order(max_table$id),]
  min_table <- min_table[order(min_table$id),]
  avg_indegs <- avg_indegs[order(avg_indegs$id),]
  avg_indegs <- avg_indegs$indegree

  return(cbind(max_table[c("id", "max_indegree")], min_table[c("min_indegree")], avg_indegree = avg_indegs))
}

