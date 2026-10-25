import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.validation.Issue;
import org.omg.sysml.interactive.SysMLInteractive;
import org.omg.sysml.interactive.SysMLInteractiveResult;
import org.omg.sysml.lang.sysml.Element;
import org.omg.sysml.lang.sysml.Feature;
import org.omg.sysml.lang.sysml.FeatureReferenceExpression;
import org.omg.sysml.lang.sysml.FeatureTyping;
import org.omg.sysml.lang.sysml.Membership;
import org.omg.sysml.lang.sysml.Namespace;
import org.omg.sysml.lang.sysml.Redefinition;
import org.omg.sysml.lang.sysml.Subsetting;

/**
 * Runs SysML files through the pilot implementation (SysMLInteractive, as the Jupyter kernel does) and prints
 * every diagnostic plus what redefinitions, subsettings, typings by a feature and feature references resolved to,
 * and the inherited memberships of each type (an alias membership is shown as 'element as aliasName').
 * Usage: java -cp <kernel-all.jar> PilotProbe.java <sysml.library dir> <file.sysml>...
 */
public class PilotProbe {
    public static void main(String[] args) throws Exception {
        SysMLInteractive si = SysMLInteractive.getInstance();
        si.loadLibrary(args[0].endsWith("/") ? args[0] : args[0] + "/");
        for (int i = 1; i < args.length; i++) {
            Path file = Path.of(args[i]);
            SysMLInteractiveResult r = si.process(Files.readString(file));
            System.out.println("### " + file.getFileName());
            if (r.getException() != null) {
                System.out.println("  EXCEPTION " + r.getException());
                continue;
            }
            if (r.getIssues().isEmpty()) {
                System.out.println("  (no issues)");
            }
            for (Issue issue : r.getIssues()) {
                System.out.printf("  %-7s L%d:%d [%s] %s%n", issue.getSeverity(), issue.getLineNumber(), issue.getColumn(),
                    issue.getCode(), issue.getMessage());
            }
            Element root = r.getRootElement();
            if (root == null) {
                continue;
            }
            for (Iterator<EObject> it = root.eAllContents(); it.hasNext();) {
                EObject o = it.next();
                if (o instanceof Feature f && !(o instanceof org.omg.sysml.lang.sysml.Expression)
                        && f.getName() != null && !f.getOwnedRelationship().isEmpty()) {
                    List<String> rel = new ArrayList<>();
                    for (Redefinition red : f.getOwnedRedefinition()) {
                        rel.add("redefines " + qn(red.getRedefinedFeature()) + (red.isImplied() ? " (implied)" : ""));
                    }
                    for (Subsetting sub : f.getOwnedSubsetting()) {
                        if (!(sub instanceof Redefinition)) {
                            rel.add("subsets " + qn(sub.getSubsettedFeature()) + (sub.isImplied() ? " (implied)" : ""));
                        }
                    }
                    rel.removeIf(s -> s.contains("(implied)") && !s.startsWith("redefines"));
                    for (FeatureTyping typing : f.getOwnedTyping()) {
                        if (typing.getType() instanceof Feature) {
                            rel.add("typed by feature " + qn(typing.getType()));
                        }
                    }
                    if (!rel.isEmpty()) {
                        System.out.println("  FEATURE " + qn(f) + " : " + String.join(", ", rel));
                    }
                }
                if (o instanceof FeatureReferenceExpression ref && ref.getReferent() != null) {
                    System.out.println("  REF " + ref.getReferent().getName() + " -> " + qn(ref.getReferent()));
                }
            }
            // computed (implicit) redefinitions and inherited memberships, as the pilot's semantics see them
            for (Iterator<EObject> it = root.eAllContents(); it.hasNext();) {
                EObject o = it.next();
                if (o instanceof Feature f && !(o instanceof org.omg.sysml.lang.sysml.Expression) && f.getName() != null
                        && f.getOwningType() != null) {
                    List<String> computed = new ArrayList<>();
                    for (Feature red : org.omg.sysml.util.FeatureUtil.getRedefinedFeaturesWithComputedOf(f)) {
                        computed.add(qn(red));
                    }
                    if (!computed.isEmpty()) {
                        System.out.println("  COMPUTED " + qn(f) + " redefines " + computed);
                    }
                }
                if (o instanceof org.omg.sysml.lang.sysml.Type t && !(o instanceof Feature && ((Feature) o).getOwningType() != null)
                        && t.getName() != null && !t.getOwnedSpecialization().isEmpty()) {
                    List<String> inherited = new ArrayList<>();
                    for (Membership m : org.omg.sysml.util.TypeUtil.getInheritedMembershipOf(t)) {
                        Element member = m.getMemberElement();
                        if (member != null && member.getName() != null && qn(member).startsWith(qn(t).split("::")[0] + "::")) {
                            String alias = m.getMemberName() != null && !m.getMemberName().equals(member.getName())
                                ? " as " + m.getMemberName() : "";
                            inherited.add(qn(member) + alias);
                        }
                    }
                    if (!inherited.isEmpty()) {
                        System.out.println("  INHERITS " + qn(t) + " " + inherited);
                    }
                }
            }
            // resolveLocal probe for the resolver-disagreement case
            for (Iterator<EObject> it = root.eAllContents(); it.hasNext();) {
                if (it.next() instanceof Namespace ns && "V6d".equals(ns.getName())) {
                    Membership m = ns.resolveLocal("mass");
                    System.out.println("  resolveLocal(V6d, 'mass') -> " + (m == null ? "null" : qn(m.getMemberElement())));
                }
            }
        }
    }

    private static String qn(Element e) {
        if (e == null) {
            return "null";
        }
        String q = e.getQualifiedName();
        if (q != null) {
            return q;
        }
        Namespace owner = e.getOwningNamespace();
        return (owner == null ? "" : qn(owner) + "::") + (e.getName() == null ? "<anon>" : e.getName());
    }
}
