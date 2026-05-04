import com.dassault_systemes.modeler.kerml.model.ISysMLTransientModelBuilder
import com.dassault_systemes.modeler.kerml.model.SysMLTransientModelBuilder
import com.nomagic.magicdraw.core.Application
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Namespace
import java.util.stream.Collectors

def project = Application.getInstance().getProject()
ISysMLTransientModelBuilder modelBuilder = new SysMLTransientModelBuilder(project)
List<Namespace> allRoots = com.dassault_systemes.modeler.kerml.model.RootNamespaces.getAllRoots(project)
    .stream().filter { it instanceof Namespace }.map { (Namespace) it }.collect(Collectors.toList())

def buildResult = modelBuilder.build("package Foo {}", allRoots)

def methods = buildResult.getClass().getMethods().collect { it.name }.unique().sort()
return methods.join(", ")
