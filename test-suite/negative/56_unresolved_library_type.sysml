/**
 * NEGATIVE TEST: Type from a library that is not configured
 * EXPECTED ERROR: SEMANTIC_UNRESOLVED_REFERENCE - 'DiagramInterchange' not found (is a library missing?)
 * EXPECTED ERROR: IMPORT_RESOLUTION_ERROR - 'MissingLibrary' not found
 * CATEGORY: Type/Resolution Errors
 * NOTE: needs the standard library configured; without libraries semantic checks are off.
 */
package NegativeTests56 {
    private import ScalarValues::*;
    private import MissingLibrary::*;
    part def Panel {
        attribute width : Real;
        view layout : DiagramInterchange::NodeView;
    }
}
