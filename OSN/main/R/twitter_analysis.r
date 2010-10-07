compute_ratio <- function(numerator, denominator, increment) {
   sequence <- seq(0.0, 1.0, increment)
   ratios <- quantile(numerator, probs=sequence) / quantile(denominator, probs=sequence);

   return (data.frame(percentiles=sequence, ratio=ratios))
}
