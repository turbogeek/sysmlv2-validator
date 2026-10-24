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
            ModelIndex.Resolution r = site.getKind() == ReferenceSite.Kind.IMPORT
                ? index.resolveImportTarget(site.getSegments(), site.getScope())
                : index.resolve(site.getSegments(), site.getScope());
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
