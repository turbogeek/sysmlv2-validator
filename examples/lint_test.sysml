/* Test file for lint warnings */
package LintTest {
    /* Unused definition - should trigger warning */
    part def UnusedPartDef {}

    /* Part usage without type - should trigger warning */
    part untypedPart;

    /* Part with type reference to definition */
    part def Vehicle {}

    part car : Vehicle;

    /* Attribute usage without type - should trigger warning */
    attribute untypedAttr;
}
