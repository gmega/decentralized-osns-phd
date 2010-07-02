''' Module with functionality related to managing and processing 
the results of experiments. '''
from os import getenv
import os

#==========================================================================
# General rules.
#==========================================================================

# Constituents of file names.
REPETITION = "REPETITION"
BASEFORMAT = "BASEFORMAT"
COMPRESSEDFORMAT = "COMPRESSEDFORMAT"
NAME_CONSTITUENTS = {REPETITION:"[0-9]+", BASEFORMAT:"\.[A-Za-z0-9_]+", COMPRESSEDFORMAT:"(?:\.[A-Za-z0-9_]+)?"}

# Make them name-addressable in the regular expression.
for key, value in NAME_CONSTITUENTS.items():
    NAME_CONSTITUENTS[key] = "(?P<" + key +">" + value +")"

#==========================================================================
# Default values.
#==========================================================================

# Default folder structure convention.
OUTPUT_LOG_FOLDER="output"
MESSAGE_LOG_FOLDER="messages"

# Default file naming conventions. 
TEXT_OUTPUT_LOG="output-${REPETITION}${BASEFORMAT}${COMPRESSEDFORMAT}"
BIN_MESSAGE_LOG="log-${REPETITION}${BASEFORMAT}${COMPRESSEDFORMAT}"

#==========================================================================
# Environment variables.
#==========================================================================
DESCRIPTORS_ROOT = os.getenv("DESCRIPTORS_ROOT")
if (DESCRIPTORS_ROOT is None):
    DESCRIPTORS_ROOT = os.getenv("HOME")

SNIPPETS_ROOT = os.path.join(DESCRIPTORS_ROOT, "snippets")
TEMPLATES_ROOT = os.path.join(DESCRIPTORS_ROOT, "templates")

#==========================================================================
# Other constants.
#==========================================================================
EXPERIMENT_TEMPLATE="experiment.template"
TPLPART_SEPARATOR = "-"

