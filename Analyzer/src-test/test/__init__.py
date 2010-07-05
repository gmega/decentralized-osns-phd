from resources import ORIGINAL_ID

def assertEdgeList(instance, graph, edgeList, map=False):
    edgeSet = set()
    
    for edge in edgeList:
        edgeSet.add(edge)
       
    for edge in graph.es:
        source = graph.vs[edge.source][ORIGINAL_ID] if map else edge.source
        target = graph.vs[edge.target][ORIGINAL_ID] if map else edge.target
        tuple = (source, target)
        tuple_i = (target, source)          
                                                            
        if tuple in edgeSet:
            edgeSet.remove(tuple)
        elif tuple_i in edgeSet:
            edgeSet.remove(tuple_i)
        else:
            instance.fail("Found spurious edge: " + str(tuple))
                
    instance.assertEqual(0, len(edgeSet), "Edges not found: " + str(edgeSet))