import os

#===============================================================================
# Constant definitions
#===============================================================================

#===============================================================================
# Vertex attributes
#===============================================================================
VERTEX_COLOR = "vertex color"
VERTEX_COLOR_GDL = "vertex color GDL"
VERTEX_COLOR_SVG = "vertex color SVG"
VERTEX_SHAPE = "shape"
VERTEX_ID = "vertex id"
PARENT_GRAPH = "parent graph"

ORIGINAL_ID = "original id"
IGRAPH_ID = "igraph_id"
ROUTING_TABLE = "routing table"
COMMUNITY_OBJECT = "community_object"

ROUTING_REGION_COLOR = "routing region color"

#===============================================================================
# Edge attributes.
#===============================================================================
EDGE_COLOR = "edge_color"
WEIGHT = "weight"

BLACK = "black"
WHITE = "white"

#===============================================================================
# Internal constants.
#===============================================================================
_RESOURCE_PATH = os.path.dirname(__file__)
_RESOURCES = "resources"
_OUTPUTS = "outputs"
_BASE_DEPTH = 2

_METADATA = ".analyzer"

#===============================================================================
# Resource accessors
#===============================================================================
def resource(file_name):
    the_path = path([_RESOURCES] + [file_name])
    if os.path.exists(the_path):
        return the_path
    
    return None


def output(file_elements):
    return path([_OUTPUTS] + file_elements)


def home():
    base_path = _RESOURCE_PATH
    for i in range(0, _BASE_DEPTH):
        base_path, tail = os.path.split(base_path)

    return base_path


def path(elements, base_path=home()):
    
    file_name = elements.pop()
    
    for element in elements:
        base_path = os.path.join(base_path, element)
        if not os.path.exists(base_path):
            os.mkdir(base_path)
            
    return os.path.join(base_path, file_name)
    
    return base_path


def storage_area(filename):
    return path([filename, _METADATA], os.getenv("HOME"))


RESOURCE_HOME = path([_RESOURCES])
OUTPUT_HOME = path([_OUTPUTS]) 
