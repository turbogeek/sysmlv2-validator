package StockTicker_Viewpoints {
    doc /*
    Stock Ticker Device - Architecture Framework

    This package defines the architecture framework for the Stock Ticker system:
    - Stakeholders: Part definitions representing people/organizations with interests
    - Concerns: Issues or questions that stakeholders care about
    - Viewpoints: Templates that address concerns and specify what views should contain

    The architecture follows the MagicGrid methodology with 5 pillars:
    1. Requirements - Stakeholder needs and system requirements
    2. Structure - System decomposition and component organization
    3. Behavior - System functions, actions, and state machines
    4. Parameters - Budgets, constraints, and analysis
    5. Safety & Reliability - Verification, validation, and error handling

    View definitions in other packages satisfy these viewpoints to demonstrate
    compliance with stakeholder concerns.
    */

    private import SysML::*;
    private import Views::*;

    // ================================================================
    // STAKEHOLDER DEFINITIONS
    // ================================================================

    part def 'Systems Engineer' {
        doc /*
        Responsible for overall system design, requirements allocation,
        and ensuring system meets stakeholder needs.

        Primary Concerns:
        - Requirements traceability
        - System decomposition
        - Verification coverage
        - Interface definitions
        */
    }

    part def 'Systems Architect' {
        doc /*
        Defines logical and physical architecture, component interactions,
        and system behavior.

        Primary Concerns:
        - System structure
        - Behavioral correctness
        - Interface compatibility
        - Design patterns
        */
    }

    part def 'Hardware Engineer' {
        doc /*
        Designs and implements physical hardware components and connections.

        Primary Concerns:
        - Physical implementation
        - Hardware interconnections
        - Power budgets
        - Thermal analysis
        - Component specifications
        */
    }

    part def 'Software Engineer' {
        doc /*
        Implements software components, algorithms, and state machines.

        Primary Concerns:
        - Software behavior
        - Action sequences
        - State machine correctness
        - Error handling
        - Memory utilization
        */
    }

    part def 'Project Manager' {
        doc /*
        Manages project execution, budgets, schedules, and risks.

        Primary Concerns:
        - Requirements traceability
        - Verification progress
        - Cost budgets
        - Risk mitigation
        */
    }

    part def 'Test Engineer' {
        doc /*
        Develops and executes verification test cases.

        Primary Concerns:
        - Verification coverage
        - Test case traceability
        - Analysis results
        - Requirement validation
        */
    }

    part def 'End User Representative' {
        doc /*
        Represents the end users (investors, traders) who will use the device.

        Primary Concerns:
        - Use case satisfaction
        - User interface
        - Performance requirements
        - Reliability
        */
    }

    // ================================================================
    // CONCERN DEFINITIONS
    // ================================================================

    concern 'requirements traceability' {
        doc /*
        Need to trace stakeholder needs through system requirements
        to design elements to verification cases.

        Ensures:
        - All stakeholder needs are captured as requirements
        - All requirements are satisfied by design elements
        - All requirements are verified by test cases
        - No orphaned requirements exist
        */
        subject;  // The system being modeled
        stakeholder se : 'Systems Engineer';
        stakeholder pm : 'Project Manager';
        stakeholder te : 'Test Engineer';
    }

    concern 'system decomposition' {
        doc /*
        Need to understand how the system is broken down into
        logical and physical components.

        Ensures:
        - Clear hierarchical structure
        - Proper component allocation
        - Interface definitions
        - Component responsibilities
        */
        subject;
        stakeholder se : 'Systems Engineer';
        stakeholder arch : 'Systems Architect';
    }

    concern 'behavioral correctness' {
        doc /*
        Need to ensure system behaviors, actions, and state machines
        correctly implement functional requirements.

        Ensures:
        - Action sequences are correct
        - State machines are complete
        - Transitions are properly guarded
        - Error handling is comprehensive
        */
        subject;
        stakeholder arch : 'Systems Architect';
        stakeholder swe : 'Software Engineer';
    }

    concern 'physical implementation' {
        doc /*
        Need to understand hardware components, connections,
        and physical constraints.

        Ensures:
        - Hardware components are specified
        - Physical connections are defined
        - Power distribution is correct
        - Thermal constraints are met
        */
        subject;
        stakeholder hwe : 'Hardware Engineer';
        stakeholder arch : 'Systems Architect';
    }

    concern 'parameter budgeting' {
        doc /*
        Need to track and allocate system budgets for power,
        memory, performance, and other parameters.

        Ensures:
        - Budgets are allocated to components
        - Constraints are met
        - Trade-offs are analyzed
        - Margins are maintained
        */
        subject;
        stakeholder se : 'Systems Engineer';
        stakeholder hwe : 'Hardware Engineer';
        stakeholder swe : 'Software Engineer';
    }

    concern 'verification coverage' {
        doc /*
        Need to ensure all requirements are verified by test cases
        and that verification is complete.

        Ensures:
        - All requirements have verification cases
        - Test coverage is complete
        - Analysis cases validate performance
        - Results demonstrate compliance
        */
        subject;
        stakeholder te : 'Test Engineer';
        stakeholder se : 'Systems Engineer';
    }

    concern 'use case satisfaction' {
        doc /*
        Need to ensure system supports all user scenarios
        and provides expected functionality.

        Ensures:
        - All use cases are captured
        - User workflows are supported
        - Interface meets user needs
        - Performance meets expectations
        */
        subject;
        stakeholder user : 'End User Representative';
        stakeholder se : 'Systems Engineer';
    }

    concern 'interface compatibility' {
        doc /*
        Need to ensure all component interfaces are properly
        defined and connected.

        Ensures:
        - Interface definitions are complete
        - Port connections are correct
        - Data flows are specified
        - Communication protocols are defined
        */
        subject;
        stakeholder arch : 'Systems Architect';
        stakeholder hwe : 'Hardware Engineer';
        stakeholder swe : 'Software Engineer';
    }

    // ================================================================
    // VIEWPOINT DEFINITIONS
    // ================================================================

    viewpoint 'Requirements Perspective' {
        doc /*
        Views conforming to this viewpoint show requirements
        with traceability to stakeholder needs and verification.

        Addressed Concerns:
        - Requirements traceability
        - Verification coverage
        - Use case satisfaction

        Expected Views:
        - Requirements tables
        - Requirements tree views
        - Verification cases tables
        - Use cases tables
        - Requirement-to-verification matrix
        */
        frame 'requirements traceability';
        frame 'verification coverage';
        frame 'use case satisfaction';

        require constraint {
            doc /*
            Shall show all requirements with:
            - Derivation to stakeholder needs or use cases
            - Satisfaction by design elements
            - Verification by test cases
            - Traceability relationships
            */
        }
    }

    viewpoint 'System Structure Perspective' {
        doc /*
        Views conforming to this viewpoint show system decomposition,
        component organization, and structural relationships.

        Addressed Concerns:
        - System decomposition
        - Physical implementation
        - Interface compatibility

        Expected Views:
        - Parts tables (logical and physical)
        - Parts tree views
        - Product configurations tree
        - Hardware interconnections view
        - Logical interconnections view
        */
        frame 'system decomposition';
        frame 'physical implementation';
        frame 'interface compatibility';

        require constraint {
            doc /*
            Shall show system structure with:
            - Part decomposition hierarchy
            - Component relationships
            - Interface definitions
            - Physical and logical views
            */
        }
    }

    viewpoint 'Behavior Perspective' {
        doc /*
        Views conforming to this viewpoint show system behaviors,
        actions, state machines, and functional flows.

        Addressed Concerns:
        - Behavioral correctness
        - Use case satisfaction

        Expected Views:
        - Actions tables
        - Actions tree view
        - Action flow diagrams
        - State machine diagrams
        - States tables
        - States nested view
        */
        frame 'behavioral correctness';
        frame 'use case satisfaction';

        require constraint {
            doc /*
            Shall show system behavior with:
            - Action decomposition
            - State machine states and transitions
            - Control flow and decision points
            - Event handling
            */
        }
    }

    viewpoint 'Parameters Perspective' {
        doc /*
        Views conforming to this viewpoint show parameter budgets,
        constraints, and analysis results.

        Addressed Concerns:
        - Parameter budgeting
        - Physical implementation

        Expected Views:
        - Analysis cases tables
        - Parameter budgets (power, memory, thermal)
        - Constraint networks
        - Trade study results
        */
        frame 'parameter budgeting';
        frame 'physical implementation';

        require constraint {
            doc /*
            Shall show parameters with:
            - Budget allocations
            - Constraint definitions
            - Analysis results
            - Margins and trade-offs
            */
        }
    }

    viewpoint 'Verification Perspective' {
        doc /*
        Views conforming to this viewpoint show verification approach,
        test cases, and validation results.

        Addressed Concerns:
        - Verification coverage
        - Requirements traceability
        - Behavioral correctness

        Expected Views:
        - Verification cases table
        - Analysis cases table
        - Requirement-to-verification matrix
        - Test coverage reports
        */
        frame 'verification coverage';
        frame 'requirements traceability';
        frame 'behavioral correctness';

        require constraint {
            doc /*
            Shall show verification with:
            - Test cases for each requirement
            - Verification methods
            - Analysis approaches
            - Expected results and criteria
            */
        }
    }

    viewpoint 'System Context Perspective' {
        doc /*
        Views conforming to this viewpoint show the system boundary,
        external actors, and use case interactions.

        Addressed Concerns:
        - Use case satisfaction
        - Interface compatibility

        Expected Views:
        - System context diagram
        - Use cases with actors
        - External interfaces
        - System boundary definition
        */
        frame 'use case satisfaction';
        frame 'interface compatibility';

        require constraint {
            doc /*
            Shall show system context with:
            - External actors
            - Use case relationships
            - System boundary
            - External interfaces
            */
        }
    }

    viewpoint 'MagicGrid Complete Perspective' {
        doc /*
        Views conforming to this viewpoint provide comprehensive
        system overview across all MagicGrid domains and pillars.

        Addressed Concerns:
        - System decomposition
        - Requirements traceability
        - Behavioral correctness
        - Parameter budgeting
        - Verification coverage

        Expected Views:
        - Complete system overview
        - Domain-specific comprehensive views
        - Cross-domain traceability
        */
        frame 'system decomposition';
        frame 'requirements traceability';
        frame 'behavioral correctness';
        frame 'parameter budgeting';
        frame 'verification coverage';

        require constraint {
            doc /*
            Shall show complete system with:
            - All major elements from all domains
            - Cross-domain relationships
            - Traceability across pillars
            - Comprehensive coverage
            */
        }
    }
}
