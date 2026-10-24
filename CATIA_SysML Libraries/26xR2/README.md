# CATIA Magic 26xR2 (Refresh2) Dassault Systemes libraries

The Dassault Systemes SysML v2 libraries shipped with CATIA Magic / Cameo 2026x Refresh2:
DassaultSystemesCollaborator, Functions, Simulation, Styles, UIComponents, Validation and Views
(short names DS_Collaborator, DS_Functions, DS_Simulation, DS_Styles, DS_UIComponents,
DS_Validation, DS_Views).

Use them as the Dassault library path, with the OMG standard library as the SysML library path:

```
java -Dsysml.library.path=<sysml.library> -Dds.views.library.path="CATIA_SysML Libraries/26xR2" ...
```

These are the real libraries, not stand-ins. Keep one folder per release; when the release
changes, add a new folder rather than editing these files.

Not included: the DiagramInterchange (beta) library that 26xR2 models use for diagram views
(`DiagramInterchange::NodeView`, `EdgeView`, `asRegularNode`, `asEdge`). Models that contain
diagram views report `'DiagramInterchange' not found` until it is added here.
