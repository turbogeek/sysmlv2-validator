/*
 * Test file for Import Refactoring Feature
 *
 * RUN WITH:
 *   sysml-validator --refactor-imports refactoring_example.sysml
 *
 * TO AUTO-APPLY:
 *   sysml-validator --refactor-imports -y refactoring_example.sysml
 *
 * TRAINING NOTES:
 * - The refactoring tool analyzes which symbols are actually used
 * - It generates specific imports for only those symbols
 * - Files with parse errors are not refactored (safety check)
 */
package RefactoringExample {

    /* BEFORE REFACTORING - Wildcard imports */

    /* This wildcard import brings in many ISQ types but only 3 are used */
    import ISQ::*;

    /* This wildcard import brings in many SI units but only 2 are used */
    import SI::*;

    /*
     * AFTER REFACTORING - What the tool will suggest:
     *
     *   import ISQ::MassValue;
     *   import ISQ::LengthValue;
     *   import SI::kg;
     *   import SI::m;
     */

    /* MODEL CONTENT - Uses only specific symbols from imports */

    part def Automobile {
        attribute curbWeight : Real;
        attribute grossWeight : Real;
        attribute overallLength : Real;
        attribute wheelbase : Real;
        attribute trackWidth : Real;
    }

    part def PerformanceTest {
        part testVehicle : Automobile;
        attribute testDuration : Real;
        attribute averageSpeed : Real;
    }

    part def ManufacturingSpec {
        attribute minWeight : Real;
        attribute maxWeight : Real;
        attribute minLength : Real;
        attribute maxLength : Real;
    }
}

/*
 * REFACTORING OUTPUT FORMATS:
 *
 * HUMAN FORMAT (default):
 * ------------------------
 * === Import Refactoring: refactoring_example.sysml ===
 * Wildcard imports found: 2
 * refactoring_example.sysml:15:5: [LINT017] Replace wildcard import
 *   Replace: import ISQ::*;
 *   With:    import ISQ::MassValue;
 *            import ISQ::LengthValue;
 *
 * JSON FORMAT (--refactor-format json):
 * { "file": "refactoring_example.sysml", "edits": [...] }
 *
 * MONACO FORMAT (--refactor-format monaco):
 * { "resource": "refactoring_example.sysml", "edits": [...] }
 *
 * LSP FORMAT (--refactor-format lsp):
 * { "documentChanges": [...] }
 */
