#!/bin/bash

# Init package dir.
rm -rf $WORKSPACE_HOME/Utilities/package
mkdir $WORKSPACE_HOME/Utilities/package
mkdir $WORKSPACE_HOME/Utilities/package/jars
mkdir $WORKSPACE_HOME/Utilities/package/graphs
mkdir $WORKSPACE_HOME/Utilities/package/bin
mkdir $WORKSPACE_HOME/Utilities/package/data
mkdir $WORKSPACE_HOME/Utilities/package/outputs

# Compile the stuff.
cd $WORKSPACE_HOME/Utilities
ant build-analyzer-jar

cd $WORKSPACE_HOME/OSN
ant build-osns-jar

cd $WORKSPACE_HOME/QuickAndDirty
ant build-experiments-jar

cp $WORKSPACE_HOME/Utilities/lib-gen/*.jar $WORKSPACE_HOME/Utilities/package/jars
cp $WORKSPACE_HOME/Utilities/misc/cli/portablebins/* $WORKSPACE_HOME/Utilities/package/bin
cp $HOME/Graphs/Orkut-cat* $WORKSPACE_HOME/Utilities/package/graphs