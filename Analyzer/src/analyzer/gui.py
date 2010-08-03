'''
Created on Jul 23, 2010

@author: giuliano
'''
import wx
import wx.aui

from wx import Frame
from wxPython._windows import wxFrame
from wx._core import DefaultPosition, DefaultValidator

class AnalyzerGUI(wx.Frame):
    
    def __init__(self, parent):
        wxFrame.__init__(self, parent, title="Analyzer", size=(800,600));
        self.__create_menus__() 
        self.__create_main_panel__()
        self.Show(True)
        
    
    def __create_menus__(self):
        filemenu = wx.Menu()
        
        item = filemenu.Append(wx.ID_NEW, "&New Workspace ", "Opens a workspace")
        item = filemenu.Append(wx.ID_OPEN, "&Open Workspace ", "Opens a workspace")
        filemenu.AppendSeparator()
        
        item = filemenu.Append(wx.ID_SAVE, "&Save Workspace", "Saves the current workspace")
        item = filemenu.Append(wx.ID_SAVEAS, "Save Workspace &As", "Saves the current workspace under another name")
        filemenu.AppendSeparator()
        
        item = filemenu.Append(wx.ID_ANY, "&Import", "Imports an experiment set")
        filemenu.AppendSeparator()
        item = filemenu.Append(wx.ID_EXIT, "E&xit", "Exits this program")
        
        helpmenu = wx.Menu()
        item = helpmenu.Append(wx.ID_ABOUT, "About", "Shows about dialog")
        
        menubar = wx.MenuBar()
        menubar.Append(filemenu, "&File")
        menubar.Append(helpmenu, "&Help")
        
        self.SetMenuBar(menubar)
        
    def __create_main_panel__(self):
        self._mgr = wx.aui.AuiManager(self)
        
        experiments = wx.TreeCtrl(self, -1, DefaultPosition, wx.Size(200,150), 
                                  wx.NO_BORDER, DefaultValidator)
        
        pipelines = wx.ListCtrl(self, -1, DefaultPosition, wx.Size(200,150),
                                  wx.NO_BORDER, DefaultValidator)
        
        filelist = wx.ListCtrl(self, -1, DefaultPosition, wx.Size(200,150),
                                  wx.NO_BORDER, DefaultValidator)
                        
        console = wx.TextCtrl(self, -1, '', DefaultPosition, wx.Size(200, 150), wx.NO_BORDER, DefaultValidator)         
        
        # add the panes to the manager
        self._mgr.AddPane(experiments, wx.LEFT, 'Experiment Sets')
        self._mgr.AddPane(console, wx.BOTTOM, 'Console')
        self._mgr.AddPane(filelist, wx.RIGHT, 'Analysis Modules')
        self._mgr.AddPane(pipelines, wx.RIGHT, 'Analysis Pipelines')

        # tell the manager to 'commit' all the changes just made
        self._mgr.Update()
        self.Bind(wx.EVT_CLOSE, self.OnClose)


    def OnClose(self, event):
        # deinitialize the frame manager
        self._mgr.UnInit()
        # delete the frame
        self.Destroy()

    
class ExperimentManager(wx.Window):
    pass

app = wx.App(False)
frame = AnalyzerGUI(None)
app.MainLoop() 
