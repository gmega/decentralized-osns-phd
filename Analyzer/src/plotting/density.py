'''
Created on Dec 17, 2010

@author: giuliano
'''
import numpy
import math
import rpy2

from enthought.mayavi import mlab
from rpy2.robjects.lib import ggplot2
from rpy2 import robjects
from rpy2.robjects.lib.ggplot2 import ggplot
from rpy2.robjects.packages import importr
import sys


class ThreeDHistogram(object):
    
    def __init__(self, input, bins, zlog=False, zscale=1.0,
                 sum=False, separator=" ", average=False, type="mlab",
                 output=None):
        self._bins = int(bins)
        self._average = bool(average)
        self._separator = separator
        self._file = input
        self._zlog = bool(zlog)
        self._zscale = float(zscale)
        self._sum = sum
        self._type = type
        self._output = output
    
    def execute(self):
        # Note: I don't need necessarily to load the points. I can do
        # two passes over the file, if extreme scalability is required.
        points = []
        with open(self._file, "r") as input:
            for line in input:
                x, y, z = [float(i) for i in line.split(self._separator)]
                points.append((x, y, z))

        xmax = max(points, key=lambda x : x[0])[0] 
        ymax = max(points, key=lambda x : x[1])[1]
        
        xwidth = xmax / float(self._bins)
        ywidth = ymax / float(self._bins)
        
        if self._type == "mlab":
            plotter = MLABPlotter(self._bins, self._bins, self._zscale)
            
        elif self._type == "R":
            def hexbinplotter(dataframe):
                plot = ggplot(dataframe)
                plot = plot + ggplot2.aes_string(x='x', y='y') + ggplot2.stat_binhex()
                return plot
            plotter = RPlotter(hexbinplotter, True)

        for x, y, z in points:
            bin_x = max(0, int(math.ceil(x / xwidth)) - 1)
            bin_y = max(0, int(math.ceil(y / ywidth)) - 1)
            plotter.point(bin_x, bin_y, z)
        
        if self._zlog:
            plotter.log_z()
            
        plotter.plot()

class MLABPlotter(object):
    
    def __init__(self, xbins, ybins, zscale=1.0):
        self._mesh = numpy.zeros((xbins, ybins))
        self._zscale = zscale
    
    def point(self, x, y, z):
        self._mesh[x][y] += z
        
    def log_z(self):
        for i in range(0, len(self._mesh)):
            for j in range(0, len(self._mesh[i])):
                if (self._mesh[i][j] != 0):
                    self._mesh[i][j] = math.log(self._mesh[i][j]) 
        
    def plot(self):
        mlab.surf(self._mesh, warp_scale=self._zscale)
        mlab.axes(xlabel="Degree", ylabel="Components", nb_labels=0)
        mlab.show()

                
class RPlotter(object):
    
    def __init__(self, plotter, expand=False, output=None, \
                  width=1024, length=1024):
        self._xpoints = []
        self._ypoints = []
        self._zpoints = {}
        self._plotter = plotter
        self._expand = expand
        self._output = output
    
    def point(self, x, y, z):
        self._xpoints.append(x)
        self._ypoints.append(y)
        tuple = (x, y)
        self._zpoints[tuple] = self._zpoints.setdefault(tuple, 0.0) + z
        
    def log_z(self):
        for key, value in self._zpoints.items():
            self._zpoints[key] = math.log(value)
        
    def plot(self):
        df = self.__point_dataframe__()
        plotter = self._plotter(df)
        if not self._output is None:
            grdevices = importr('grDevices')
            grdevices.png(file=self._output,
                          width=self._width,
                          height=self._height)         
        plotter.plot()
        if not self._output is None:
            grdevices.dev_off()
        else:
            print "Press ENTER to exit."
            sys.stdin.readline()
            
    def __point_dataframe__(self):
        if self._expand:
            xpoints, ypoints = self.__expand__()
            zpoints = None
        else:
            xpoints = self._xpoints
            ypoints = self._ypoints
            zpoints = [self._zpoints[xy] for xy in self.__all_xy__()]
        
        df = {'x': robjects.FloatVector(xpoints),
              'y': robjects.FloatVector(ypoints)}
        
        if not zpoints is None:
            df['z'] = robjects.FloatVector(zpoints)
            
        return robjects.DataFrame(df)
    
    def __expand__(self):
        xpoints = []
        ypoints = []
        expansion = 0
        print "Expand"
        for x, y in self.__all_xy__():
            expand_factor = int(math.ceil(self._zpoints[(x, y)]))
            for j in range(0, expand_factor):
                xpoints.append(x)
                ypoints.append(y)
                
            expansion += expand_factor
            if (expansion % 1000 == 0):
                print expansion
        
        print "Before expansion:", len(self._xpoints), "After expansion:", expansion
        return (xpoints, ypoints)
    
    def __all_xy__(self):
        for i in range(0, len(self._xpoints)):
            yield (self._xpoints[i], self._ypoints[i])
