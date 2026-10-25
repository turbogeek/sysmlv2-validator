// SysML v2 import bodies and metadata usage declarations (SysML v2 8.2.2.2, 8.2.2.5.1, 8.2.2.27).
// Exercises: an import whose RelationshipBody owns a documentation comment (relationshipBody#2), a metadata annotation
// that names its usage ('@review : Reviewed;', MetadataUsageDeclaration, metadataDeclaredName) and a metadata usage
// typed with 'typed by' (TYPED_BY).
package SysmlImportsMetadata {
    private import ScalarValues::Integer {
        doc /* The unqualified Integer is signed. */
    }

    metadata def Reviewed;
    part def Engine {
        @review : Reviewed;
        metadata approval typed by Reviewed;
        attribute cylinders : Integer;
    }
}
