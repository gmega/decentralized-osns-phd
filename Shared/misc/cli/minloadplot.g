#!/usr/bin/gnuplot
set terminal postscript eps enhanced monochrome
set output 'minload.eps'

set xlabel 'Round'
set ylabel 'Percentage of nodes with zero load'
set title 'Percentage of zero-load nodes per round'
plot 'minload-summary.text' using 1:2 w l title ""
