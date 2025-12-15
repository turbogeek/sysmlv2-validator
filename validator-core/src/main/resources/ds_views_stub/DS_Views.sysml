/**
 * DS_Views Stub Library
 *
 * This is a minimal stub of the Cameo Systems Modeler proprietary DS_Views library.
 * It provides type definitions to allow validation of models that import DS_Views
 * without requiring a full Cameo installation.
 *
 * NOTE: This stub only defines the structure, not the implementation.
 * For full DS_Views functionality, use Cameo Systems Modeler.
 *
 * Source: Dassault Systemes / No Magic Inc.
 * Version: Stub for validation purposes
 */
package DS_Views {
	doc /*
	 * Cameo Systems Modeler view library extensions.
	 * Provides specialized view types for tabular and symbolic representations.
	 */

	// ========================================================================
	// TABULAR VIEWS
	// ========================================================================

	package TabularViews {
		doc /* Tabular view types for tables and matrices */

		view def rt {
			doc /* Requirements Table view */
		}

		view def gt {
			doc /* Generic Table view */
		}

		view def dt {
			doc /* Dependency Table view */
		}

		view def pt {
			doc /* Properties Table view */
		}
	}

	// ========================================================================
	// SYMBOLIC VIEWS (Graphical Diagrams)
	// ========================================================================

	package SymbolicViews {
		doc /* Symbolic (graphical) view types for diagrams */

		view def gv {
			doc /* General View - generic graphical diagram */
		}

		view def iv {
			doc /* Interconnection View - connection diagram */
		}

		view def afv {
			doc /* Action Flow View - action/activity diagram */
		}

		view def stv {
			doc /* State Transition View - state machine diagram */
		}

		view def sv {
			doc /* Sequence View - sequence diagram */
		}

		view def bv {
			doc /* Browser View - containment tree browser */
		}
	}

	// ========================================================================
	// SYMBOLIC VIEWS BY EXPRESSION (Filtered Views)
	// ========================================================================

	package SymbolicViewsByExpression {
		doc /* Specialized views with built-in filtering and structure */

		view def ActionsTreeView {
			doc /* Hierarchical tree view of actions */
		}

		view def ActionsNestedView {
			doc /* Nested view of action decomposition */
		}

		view def StatesTreeView {
			doc /* Hierarchical tree view of states */
		}

		view def StatesNestedView {
			doc /* Nested view of state hierarchy */
		}

		view def PartsTreeView {
			doc /* Hierarchical tree view of parts (component breakdown) */
		}

		view def PartsNestedView {
			doc /* Nested view of part decomposition */
		}

		view def RequirementsTreeView {
			doc /* Hierarchical tree view of requirements */
		}

		view def RequirementsNestedView {
			doc /* Nested view of requirement hierarchy */
		}

		view def ConstraintsTreeView {
			doc /* Hierarchical tree view of constraints */
		}

		view def UseCasesTreeView {
			doc /* Hierarchical tree view of use cases */
		}

		view def NonStandardLibraryElementFilter {
			doc /* Filter to exclude standard library elements from views */
		}
	}

	// ========================================================================
	// DEFAULT RENDERING
	// ========================================================================

	rendering def DefaultRendering {
		doc /* Default rendering for Cameo views */
	}

	rendering asDefault : DefaultRendering {
		doc /* Default rendering instance */
	}
}
