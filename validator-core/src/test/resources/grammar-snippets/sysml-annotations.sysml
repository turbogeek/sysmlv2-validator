// SysML v2 comments, textual representations, metadata, library packages and names (SysML v2 8.2.2.1.2, 8.2.2.4,
// 8.2.2.5, 8.2.2.27).
// Exercises: a comment with only 'locale', 'rep' without and with a name, 'language' alone, 'standard library
// package', a metadata usage whose body has an '@' annotation (MetadataBody admits a DefinitionMember, and so an
// AnnotatingElement) and a value, and words that the lexer has keyword tokens for but SysML v2 does not reserve, used
// as names.
standard library package SysmlAnnotations {
    locale "en_US" /* A comment with only a locale. */
    rep language "OCL" /* self.mass > 0 */
    rep Positive language "OCL" /* self.mass > 0 */
    language "ALF" /* x = 1; */

    metadata def Reviewed { }
    metadata def Rationale {
        attribute text : ScalarValues::String;
    }

    part def Holder {
        metadata Rationale {
            @Reviewed;
            text = "Names that SysML v2 does not reserve";
        }
        attribute any;
        attribute chains;
        attribute featuring;
        attribute inverses;
        attribute sequence;
        attribute typing;
        attribute value;
        attribute multiplicity;
    }
}
