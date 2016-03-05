# magicdraw-plugin-sparql

MagicDraw SPARQl plugin.

Tested with MagicDraw v18.0 SP1

##User mode
Copy the "edu.gatech.mbse.magicdraw.sparql" folder
located in the "MagicDraw Plugin - Folder to copy into MD plugins directory" folder
into your MagicDraw plugins directory.

Example models will be added later.

Open a MagicDraw model containing constraints describing SPARQL queries. Right-click on the constraint and select "Execute SPARQL Design Rule"

You should then see details about the query and the query result in the MagicDraw notification window.

##Developer mode

The classpath is suited for MagicDraw v18.0sp1. For a different version, the Java classpath will probably have to be updated. The PrintPathsToMagicDrawJars.java application can be used to print out the path to therequired MagicDraw jars of your MagicDraw version.

create an Eclipse classpath variable called magicdrawinstalldir pointing to the MagicDraw installation directory (Windows -> Preferences -> Java -> Build Path -> Classpath Variables)
