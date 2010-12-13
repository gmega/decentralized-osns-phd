/*
 This program parses multiple experiment files and extracts parameters and repetitions, printing out a single log stream.

 Parameters are guessed from file names. A log file containing parameters PSI and NZERO has to contain two substrings:

 PSI_[PSI_VALUE]
 NZERO_[NZERO_VALUE]

 which point to the values of PSI and NZERO, respectively. Note that names are all-caps, with a following underscore. Values are assumed to be numeric.

*/
package main

import (
	"os"
	"flag"
	"regexp"
)

var printRepetitions = flag.Bool("n", false, "don't include the repetition number (R_[0-9]+) in the final output.")

func main() {

}

// Regular expressions for template extraction.
const (
	IDENT = "[A-Z]+"
	FLOAT = "[0-9](\".\"[0-9]+)?"
	REPETITION = "R"
)

var parameter = regexp.MustCompile(IDENT + "_" + FLOAT)

type parameterExtractor {
	// Template.
	var template map[string]float	
}

func (self *parameterExtractor) NewExtractor(prototype string) {
	parameter.FindAllStringSu
}

func (self *parameterExtractor) Parameters(filename string, map[string]float) {

}