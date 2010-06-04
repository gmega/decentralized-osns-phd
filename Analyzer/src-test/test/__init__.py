
def assertEdgeList(instance, graph, edgeList):
    edgeSet = set()
    removed = set()
    for edge in edgeList:
        edgeSet.add(edge)
       
    for edge in graph.es:
        tuple = (edge.source, edge.target)
        tuple_i = (edge.target, edge.source)          
                                                            
        if edgeSet.__contains__(tuple):
            edgeSet.remove(tuple)
        elif edgeSet.__contains__(tuple_i):
            edgeSet.remove(tuple_i)
        else:
            instance.fail("Found spurious edge: " + str(tuple))
                
    instance.assertEqual(0, len(edgeSet), "Edges not found: " + str(edgeSet))