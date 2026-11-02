package com.validator.semantic.index;

import com.validator.ErrorCodes;
import com.validator.ValidationError;

import java.util.ArrayList;
import java.util.List;

/**
 * Reports names that do not resolve: import targets, typings, specializations of definitions and
 * metadata annotations. Names whose resolution depends on something the index cannot see are not
 * reported (see {@link ModelIndex}).
 */
public final class ReferenceChecker {

    private ReferenceChecker() {
    }

    /**
     * Check the reference sites of one file.
     *
     * @param sites the file's reference sites
     * @param index the index of the libraries and validated files
     * @param filePath the file path for the errors
     * @return one error per name that does not resolve
     */
    public static List<ValidationError> check(List<ReferenceSite> sites, ModelIndex index, String filePath) {
        List<ValidationError> errors = new ArrayList<>();
        for (ReferenceSite site : sites) {
            if (site.getKind() == ReferenceSite.Kind.REFERENCE && site.getSegments().size() == 1
                    && ("self".equals(site.getSegments().get(0)) || "that".equals(site.getSegments().get(0)))) {
                // 'send x to self': the context feature, not a declared name
                continue;
            }
            ModelIndex.Resolution r = site.getKind() == ReferenceSite.Kind.IMPORT
                ? index.resolveImportTarget(site.getSegments(), site.getScope())
                : index.resolve(site.getSegments(), site.getScope());
            if (r.getStatus() == ModelIndex.Status.FOUND && !site.getChain().isEmpty()) {
                ValidationError chainError = checkChain(site, r, index, filePath);
                if (chainError != null) {
                    errors.add(chainError);
                }
                continue;
            }
            if (r.getStatus() != ModelIndex.Status.NOT_FOUND) {
                continue;
            }
            errors.add(new ValidationError.Builder()
                .filePath(filePath)
                .line(site.getLine())
                .column(site.getColumn())
                .message(message(site, r.getFailedSegment()))
                .errorCode(errorCode(site, r.getFailedSegment()))
                .severity(ValidationError.Severity.ERROR)
                .build());
        }
        return errors;
    }

    /** Each name after a '.' must be a feature of the element before it; stops where resolution is not certain. */
    private static ValidationError checkChain(ReferenceSite site, ModelIndex.Resolution start, ModelIndex index, String filePath) {
        ModelIndex.Resolution r = start;
        String path = String.join("::", site.getSegments());
        boolean first = true;
        for (String name : site.getChain()) {
            if (r.getTargets().size() != 1) {
                return null;
            }
            String owner = r.getTargets().iterator().next();
            if (!certainType(owner, index)) {
                return null;
            }
            if (first && site.getSegments().size() == 1 && !lexicallyVisible(owner, site.getScope(), index)) {
                // An inherited feature may be redefined here by an unnamed ':>> x : T', which narrows its type
                return null;
            }
            first = false;
            r = index.resolveMember(owner, name);
            if (r.getStatus() == ModelIndex.Status.NOT_FOUND) {
                return new ValidationError.Builder()
                    .filePath(filePath)
                    .line(site.getLine())
                    .column(site.getColumn())
                    .message(String.format("Cannot resolve '%s': '%s' is not a feature of '%s'", site.getText(), name, path))
                    .errorCode(ErrorCodes.SEMANTIC_UNRESOLVED_REFERENCE)
                    .severity(ValidationError.Severity.ERROR)
                    .build();
            }
            if (r.getStatus() != ModelIndex.Status.FOUND) {
                return null;
            }
            path = path + "." + name;
        }
        return null;
    }

    /** The element's features are known: it is typed or specialized by plain names (an untyped usage, or one whose
     *  type a user-defined keyword supplies, is not checked). */
    private static boolean certainType(String qualifiedName, ModelIndex index) {
        List<IndexedElement> records = index.recordsOf(qualifiedName);
        if (records.isEmpty()) {
            return false;
        }
        for (IndexedElement e : records) {
            if (e.opaqueSupertypes || e.supertypes.isEmpty() && e.kind != null && !e.kind.endsWith("Definition")) {
                return false;
            }
        }
        return true;
    }

    /** The element is owned by the site's scope or a namespace enclosing it (not inherited from a supertype). */
    private static boolean lexicallyVisible(String qualifiedName, String scope, ModelIndex index) {
        List<IndexedElement> records = index.recordsOf(qualifiedName);
        if (records.isEmpty()) {
            return false;
        }
        String parent = records.get(0).parentName == null ? "" : records.get(0).parentName;
        String s = scope == null ? "" : scope;
        while (true) {
            if (s.equals(parent)) {
                return true;
            }
            int cut = s.lastIndexOf("::");
            if (s.isEmpty()) {
                return false;
            }
            s = cut < 0 ? "" : s.substring(0, cut);
        }
    }

    private static String errorCode(ReferenceSite site, int failed) {
        if (site.getKind() != ReferenceSite.Kind.IMPORT) {
            return ErrorCodes.SEMANTIC_UNRESOLVED_REFERENCE;
        }
        boolean elementImport = !site.getText().endsWith("*");
        return elementImport && failed == site.getSegments().size() - 1 && failed > 0
            ? ErrorCodes.IMPORT_ELEMENT_NOT_FOUND
            : ErrorCodes.IMPORT_PACKAGE_NOT_FOUND;
    }

    private static String message(ReferenceSite site, int failed) {
        List<String> segs = site.getSegments();
        String what = switch (site.getKind()) {
            case IMPORT -> "import";
            case TYPE -> "type";
            case SPECIALIZATION -> "supertype";
            case METADATA -> "metadata definition";
            case REFERENCE -> "referenced feature";
        };
        String missing = failed >= 0 && failed < segs.size() ? segs.get(failed) : site.getText();
        if (segs.size() == 1 || failed <= 0) {
            String hint = segs.size() == 1 && site.getKind() != ReferenceSite.Kind.IMPORT
                ? " (not declared in this or an enclosing namespace, and not imported)"
                : " (no such top-level package or visible member; is a library missing?)";
            return String.format("Cannot resolve %s '%s': '%s' not found%s", what, site.getText(), missing, hint);
        }
        String owner = String.join("::", segs.subList(0, failed));
        return String.format("Cannot resolve %s '%s': '%s' is not a member of '%s'", what, site.getText(), missing, owner);
    }
}
