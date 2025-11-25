parser grammar SysMLv2Parser;

options {
    tokenVocab = SysMLv2Lexer;
}

// ============================================================================
// TOP-LEVEL RULES
// ============================================================================

compilationUnit
    : rootNamespace EOF
    ;

rootNamespace
    : namespaceBodyElement*
    ;

// ============================================================================
// NAMESPACE AND PACKAGE
// ============================================================================

namespaceBodyElement
    : visibility? (
        packageDeclaration
      | libraryPackageDeclaration
      | namespaceDeclaration
      | aliasDeclaration
      | importDeclaration
      | member
      )
    ;

packageDeclaration
    : prefixes? PACKAGE qualifiedName packageBody
    ;

libraryPackageDeclaration
    : prefixes? STANDARD? LIBRARY PACKAGE qualifiedName packageBody
    ;

namespaceDeclaration
    : prefixes? NAMESPACE qualifiedName namespaceBody
    ;

packageBody
    : LBRACE namespaceBodyElement* RBRACE
    | SEMICOLON
    ;

namespaceBody
    : LBRACE namespaceBodyElement* RBRACE
    | SEMICOLON
    ;

// ============================================================================
// ALIASES AND IMPORTS
// ============================================================================

aliasDeclaration
    : ALIAS aliasName FOR qualifiedName SEMICOLON
    ;

aliasName
    : name (shortName)?
    | shortName
    ;

importDeclaration
    : visibility? IMPORT (ALL | importFilter)? qualifiedNameWithWildcard SEMICOLON
    ;

importFilter
    : COLON COLON
    ;

qualifiedNameWithWildcard
    : qualifiedName (DOUBLE_COLON STAR DOUBLE_COLON DOUBLE_STAR | DOUBLE_COLON DOUBLE_STAR | DOUBLE_COLON STAR)?
    ;

// ============================================================================
// PREFIXES AND MODIFIERS
// ============================================================================

prefixes
    : (metadataAnnotation | hashAnnotation | featurePrefix)+
    ;

hashAnnotation
    : HASH qualifiedName
    ;

metadataAnnotation
    : AT_SIGN qualifiedName (LPAREN annotationArguments RPAREN)?
    | AT_SIGN qualifiedName metadataBody
    | AT_SIGN qualifiedName SEMICOLON?
    ;

annotationArguments
    : annotationArgument (COMMA annotationArgument)*
    ;

annotationArgument
    : name EQUALS expression
    | expression
    ;

featurePrefix
    : definitionPrefix
    | usagePrefix
    ;

definitionPrefix
    : ABSTRACT
    | VARIATION
    | INDIVIDUAL
    ;

usagePrefix
    : REF
    | READONLY
    | DERIVED
    | ORDERED
    | NONUNIQUE
    | COMPOSITE
    | PORTION
    | INDIVIDUAL
    | VARIANT
    | END
    | CONSTANT
    ;

directionPrefix
    : IN
    | OUT
    | INOUT
    ;

// ============================================================================
// VISIBILITY
// ============================================================================

visibility
    : PUBLIC
    | PRIVATE
    | PROTECTED
    ;

// ============================================================================
// MEMBERS (ALL DEFINITIONS AND USAGES)
// ============================================================================

member
    : prefixes? (
        // SysML Definitions
        partDefinition
      | actionDefinition
      | stateDefinition
      | requirementDefinition
      | viewDefinition
      | viewpointDefinition
      | constraintDefinition
      | attributeDefinition
      | portDefinition
      | connectionDefinition
      | interfaceDefinition
      | allocationDefinition
      | itemDefinition
      | enumDefinition
      | calcDefinition
      | analysisDefinition
      | caseDefinition
      | useCaseDefinition
      | verificationDefinition
      | concernDefinition
      | renderingDefinition
      | occurrenceDefinition
      | flowDefinition
      | metadataDefinition
        // KerML Definitions
      | datatypeDefinition
      | classDefinition
      | structDefinition
      | assocDefinition
      | behaviorDefinition
      | functionDefinition
      | predicateDefinition
      | interactionDefinition
      | metaclassDefinition
      | classifierDefinition
      | typeDefinition
      | featureDefinition
      | connectorDefinition
      | bindingConnectorDefinition
        // Usages
      | partUsage
      | actionUsage
      | stateUsage
      | requirementUsage
      | viewUsage
      | constraintUsage
      | attributeUsage
      | enumUsage
      | portUsage
      | itemUsage
      | refUsage
      | connectionUsage
      | flowConnectionUsage
      | successionUsage
      | transitionUsage
      | satisfyRequirement
      | allocateUsage
      | performUsage
      | exhibitUsage
      | includeUsage
      | calcUsage
      | analysisUsage
      | caseUsage
      | useCaseUsage
      | verificationUsage
      | renderingUsage
      | viewpointUsage
      | actorUsage
      | stakeholderUsage
      | concernUsage
      | occurrenceUsage
      | variantUsage
      | portionUsage
      | stepUsage
      | interfaceUsage
        // Control nodes
      | decideNode
      | mergeNode
      | forkNode
      | joinNode
        // Binding and messaging
      | bindingUsage
      | acceptUsage
      | sendUsage
      | assignUsage
      | messageUsage
        // End/connector ends
      | endUsage
        // Metadata
      | metadataUsage
        // Dependencies
      | dependencyDeclaration
        // Textual representations
      | textualRepresentation
      | languageStatement
        // Analysis/Verification/Use Case elements
      | subjectDeclaration
      | objectiveRequirement
        // Other elements
      | comment
      | documentation
      )
    ;

// ============================================================================
// SYSML DEFINITIONS
// ============================================================================

partDefinition
    : PART_DEF declarationName typeRelationships? definitionBody
    ;

actionDefinition
    : ACTION_DEF declarationName typeRelationships? definitionBody
    ;

stateDefinition
    : STATE_DEF declarationName typeRelationships? stateDefinitionBody
    ;

requirementDefinition
    : REQUIREMENT_DEF declarationName typeRelationships? requirementBody
    ;

viewDefinition
    : VIEW_DEF declarationName typeRelationships? viewBody
    ;

viewpointDefinition
    : VIEWPOINT_DEF declarationName typeRelationships? viewpointBody
    ;

viewpointBody
    : LBRACE viewpointBodyElement* RBRACE
    | SEMICOLON
    ;

viewpointBodyElement
    : namespaceBodyElement
    | framedConcern
    | concernUsage
    ;

constraintDefinition
    : CONSTRAINT_DEF declarationName typeRelationships? constraintDefBody
    ;

constraintDefBody
    : LBRACE constraintDefBodyElement* constraintExpression? RBRACE
    | SEMICOLON
    ;

constraintDefBodyElement
    : namespaceBodyElement
    | attributeUsage
    | inParameter
    | outParameter
    ;

attributeDefinition
    : ATTRIBUTE_DEF declarationName? typeRelationships? definitionBody
    | ATTRIBUTE_DEF declarationName? typeRelationships? SEMICOLON
    ;

portDefinition
    : PORT_DEF declarationName typeRelationships? definitionBody
    ;

connectionDefinition
    : CONNECTION_DEF declarationName typeRelationships? definitionBody
    ;

interfaceDefinition
    : INTERFACE_DEF declarationName typeRelationships? definitionBody
    ;

allocationDefinition
    : ALLOCATION_DEF declarationName typeRelationships? definitionBody
    ;

itemDefinition
    : ITEM_DEF declarationName typeRelationships? definitionBody
    ;

enumDefinition
    : ENUM_DEF declarationName typeRelationships? enumBody
    | ENUM_DEF declarationName typeRelationships? SEMICOLON
    ;

calcDefinition
    : CALC_DEF declarationName typeRelationships? definitionBody
    ;

analysisDefinition
    : ANALYSIS_DEF declarationName typeRelationships? definitionBody
    ;

caseDefinition
    : CASE_DEF declarationName typeRelationships? definitionBody
    ;

useCaseDefinition
    : USE_CASE_DEF declarationName typeRelationships? definitionBody
    ;

verificationDefinition
    : VERIFICATION_DEF declarationName typeRelationships? definitionBody
    ;

concernDefinition
    : CONCERN_DEF declarationName typeRelationships? definitionBody
    ;

renderingDefinition
    : RENDERING_DEF declarationName typeRelationships? definitionBody
    ;

occurrenceDefinition
    : OCCURRENCE_DEF declarationName typeRelationships? definitionBody
    ;

flowDefinition
    : FLOW_DEF declarationName typeRelationships? definitionBody
    ;

metadataDefinition
    : METADATA_DEF declarationName? shortName? typeRelationships? metadataDefBody?
    | METADATA_DEF declarationName? shortName? typeRelationships? SEMICOLON
    ;

metadataDefBody
    : LBRACE metadataDefBodyElement* RBRACE
    ;

metadataDefBodyElement
    : namespaceBodyElement
    | anonymousRedefines
    ;

// ============================================================================
// KERML DEFINITIONS
// ============================================================================

datatypeDefinition
    : DATATYPE declarationName typeRelationships? (definitionBody | SEMICOLON)
    ;

classDefinition
    : CLASS ALL? declarationName? multiplicityClause? typeRelationships? (definitionBody | SEMICOLON)?
    ;

structDefinition
    : STRUCT declarationName typeRelationships? (definitionBody | SEMICOLON)?
    ;

assocDefinition
    : (ASSOC_STRUCT | ASSOC) declarationName typeRelationships? (definitionBody | SEMICOLON)?
    ;

behaviorDefinition
    : BEHAVIOR declarationName typeRelationships? (definitionBody | SEMICOLON)?
    ;

functionDefinition
    : FUNCTION declarationName typeRelationships? (definitionBody | SEMICOLON)?
    ;

predicateDefinition
    : PREDICATE declarationName typeRelationships? (definitionBody | SEMICOLON)?
    ;

interactionDefinition
    : INTERACTION declarationName typeRelationships? (definitionBody | SEMICOLON)?
    ;

metaclassDefinition
    : METACLASS declarationName typeRelationships? (definitionBody | SEMICOLON)?
    ;

classifierDefinition
    : CLASSIFIER declarationName typeRelationships? (definitionBody | SEMICOLON)?
    ;

typeDefinition
    : TYPE declarationName typeRelationships? (definitionBody | SEMICOLON)
    ;

featureDefinition
    : FEATURE declarationName featureRelationships? (definitionBody | SEMICOLON)?
    ;

connectorDefinition
    : CONNECTOR declarationName typeRelationships? (definitionBody | SEMICOLON)?
    ;

bindingConnectorDefinition
    : BINDING declarationName typeRelationships? (definitionBody | SEMICOLON)?
    ;

// ============================================================================
// SYSML USAGES
// ============================================================================

partUsage
    : directionPrefix? PART usageName? featureRelationships? usageBody?
    | directionPrefix? PART featureRelationships usageBody?
    | directionPrefix? PART featureRelationships SEMICOLON
    ;

actionUsage
    : directionPrefix? ACTION usageName? actionKeyword? featureRelationships? actionSendClause? usageBody?
    | directionPrefix? ACTION usageName? actionKeyword? featureRelationships? actionSendClause? SEMICOLON
    | directionPrefix? ACTION usageName? SEND (VIA expression)? TO expression SEMICOLON?
    | directionPrefix? ACTION usageName? ACCEPT expression (VIA expression)? SEMICOLON?
    ;

actionKeyword
    : SEND
    | ACCEPT
    | DECIDE
    | MERGE
    | FORK
    | JOIN
    ;

actionSendClause
    : SEND (VIA expression)? TO expression
    ;

stateUsage
    : PARALLEL? STATE usageName? PARALLEL? featureRelationships? stateUsageBody?
    ;

requirementUsage
    : REQUIREMENT usageName? featureRelationships? requirementBody?
    ;

viewUsage
    : VIEW usageName? featureRelationships? viewBody?
    ;

constraintUsage
    : CONSTRAINT usageName? featureRelationships? constraintBody?
    | ASSERT CONSTRAINT usageName? featureRelationships? constraintBody?
    | ASSERT CONSTRAINT usageName? featureRelationships? SEMICOLON
    | ASSERT NOT? qualifiedName constraintBody?
    | ASSERT NOT? qualifiedName SEMICOLON
    ;

constraintBody
    : LBRACE constraintBodyElement* constraintExpression? RBRACE
    ;

constraintBodyElement
    : namespaceBodyElement
    | inParameter
    | outParameter
    | anonymousRedefines
    | attributeUsage
    ;

constraintExpression
    : expression
    ;

attributeUsage
    : directionPrefix? ATTRIBUTE usageName? featureRelationships? valueInit? (SEMICOLON | usageBody)
    | directionPrefix? ATTRIBUTE REDEFINES qualifiedName tupleOrValueInit? (SEMICOLON | usageBody)
    ;

enumUsage
    : ENUM usageName? featureRelationships? valueInit? SEMICOLON
    | ENUM usageName? featureRelationships? valueInit? usageBody
    ;

tupleOrValueInit
    : EQUALS LPAREN argumentList RPAREN
    | valueInit
    ;

portUsage
    : directionPrefix? PORT usageName? featureRelationships? usageBody?
    | directionPrefix? PORT featureRelationships usageBody?
    | directionPrefix? PORT featureRelationships SEMICOLON
    ;

itemUsage
    : directionPrefix? ITEM usageName? featureRelationships? usageBody?
    ;

refUsage
    : directionPrefix? REF hashAnnotation* refKind? usageName? featureRelationships? valueInit? (SEMICOLON | usageBody)
    ;

refKind
    : PART
    | ACTION
    | STATE
    | ATTRIBUTE
    | PORT
    | ITEM
    ;

connectionUsage
    : CONNECTION usageName? featureRelationships? connectionEndpoints? usageBody?
    | CONNECTION usageName? featureRelationships? connectionEndpoints? SEMICOLON
    | CONNECT connectionEndpoints SEMICOLON?
    ;

flowConnectionUsage
    : FLOW usageName? flowEndpoints? usageBody?
    | FLOW flowEndpoints SEMICOLON?
    | FLOW expression TO expression SEMICOLON
    ;

successionUsage
    : FIRST expression (THEN expression)? SEMICOLON?
    | THEN expression SEMICOLON?
    | SUCCESSION FLOW? usageName? featureRelationships? successionFlowOf? successionEndpoints? SEMICOLON?
    ;

successionFlowOf
    : OF qualifiedName
    ;

transitionUsage
    : TRANSITION usageName? transitionRelationships? transitionBody
    | TRANSITION usageName? FIRST expression transitionBody
    ;

satisfyRequirement
    : ASSERT? NOT? SATISFY expression (BY expression)? SEMICOLON?
    ;

allocateUsage
    : ALLOCATION usageName? featureRelationships? usageBody?
    | ALLOCATE expression TO expression SEMICOLON?
    ;

performUsage
    : PERFORM ACTION? expression (BY expression)? SEMICOLON?
    | PERFORM ACTION? usageName? featureRelationships? usageBody
    ;

exhibitUsage
    : EXHIBIT STATE? expression (BY expression)? SEMICOLON?
    | EXHIBIT STATE? usageName? featureRelationships? usageBody
    ;

includeUsage
    : INCLUDE expression SEMICOLON?
    | INCLUDE USE_CASE usageName? featureRelationships? usageBody?
    | INCLUDE USE_CASE usageName? featureRelationships? SEMICOLON
    ;

calcUsage
    : directionPrefix? CALC usageName? featureRelationships? usageBody?
    ;

analysisUsage
    : ANALYSIS usageName? featureRelationships? usageBody?
    ;

caseUsage
    : CASE usageName? featureRelationships? usageBody?
    ;

useCaseUsage
    : USE_CASE usageName? featureRelationships? usageBody?
    ;

verificationUsage
    : VERIFICATION usageName? featureRelationships? usageBody?
    ;

renderingUsage
    : RENDERING usageName? featureRelationships? usageBody?
    ;

viewpointUsage
    : VIEWPOINT usageName? featureRelationships? viewpointBody?
    ;

actorUsage
    : ACTOR usageName? featureRelationships? valueInit? usageBody?
    | ACTOR usageName? featureRelationships? valueInit? SEMICOLON
    ;

stakeholderUsage
    : STAKEHOLDER usageName? featureRelationships? usageBody?
    | STAKEHOLDER usageName? featureRelationships? SEMICOLON
    ;

concernUsage
    : CONCERN usageName? featureRelationships? concernBody?
    | CONCERN usageName? featureRelationships? SEMICOLON
    ;

concernBody
    : LBRACE concernBodyElement* RBRACE
    ;

concernBodyElement
    : namespaceBodyElement
    | subjectDeclaration
    | stakeholderUsage
    ;

occurrenceUsage
    : OCCURRENCE usageName? featureRelationships? usageBody?
    | INDIVIDUAL usageName? featureRelationships? usageBody?
    | SNAPSHOT usageName? featureRelationships? valueInit? SEMICOLON?
    | TIMESLICE usageName? featureRelationships? valueInit? SEMICOLON?
    | EVENT OCCURRENCE? usageName? featureRelationships? usageBody?
    | EVENT OCCURRENCE? usageName? featureRelationships? SEMICOLON
    ;

variantUsage
    : VARIANT usageName? featureRelationships? usageBody?
    | VARIANT usageName? featureRelationships? SEMICOLON
    ;

portionUsage
    : PORTION usageName? featureRelationships? usageBody?
    | PORTION usageName? featureRelationships? SEMICOLON
    ;

stepUsage
    : STEP usageName? featureRelationships? usageBody?
    | STEP usageName? featureRelationships? SEMICOLON
    ;

interfaceUsage
    : INTERFACE usageName? featureRelationships? connectionEndpoints? usageBody?
    | INTERFACE usageName? featureRelationships? connectionEndpoints? SEMICOLON
    ;

// ============================================================================
// CONTROL NODES
// ============================================================================

decideNode
    : DECIDE usageName? SEMICOLON? decideBody?
    ;

decideBody
    : ifBranch+ elseBranch?
    ;

ifBranch
    : IF expression THEN expression SEMICOLON?
    ;

elseBranch
    : ELSE expression SEMICOLON?
    ;

mergeNode
    : MERGE usageName? SEMICOLON?
    ;

forkNode
    : FORK usageName? SEMICOLON?
    ;

joinNode
    : JOIN usageName? SEMICOLON?
    ;

// ============================================================================
// BINDING AND MESSAGING
// ============================================================================

bindingUsage
    : BIND expression (EQUALS expression)? SEMICOLON?
    ;

acceptUsage
    : ACCEPT usageName? featureRelationships? acceptTiming? SEMICOLON?
    ;

acceptTiming
    : AFTER expression
    | AT expression
    | WHEN expression
    ;

sendUsage
    : SEND expression (VIA expression)? TO expression SEMICOLON?
    ;

assignUsage
    : ASSIGN expression COLON_EQUALS expression SEMICOLON?
    ;

messageUsage
    : MESSAGE usageName? featureRelationships? flowEndpoints? usageBody?
    | MESSAGE usageName? featureRelationships? flowEndpoints? SEMICOLON
    ;

dependencyDeclaration
    : DEPENDENCY usageName? (FROM expression)? TO expression (COMMA expression)* SEMICOLON
    ;

metadataUsage
    : METADATA usageName? featureRelationships? metadataBody?
    | METADATA usageName? featureRelationships? SEMICOLON
    ;

metadataBody
    : LBRACE metadataBodyElement* RBRACE
    ;

metadataBodyElement
    : usageName EQUALS expression SEMICOLON
    | metadataAnnotation
    ;

// ============================================================================
// END USAGES (CONNECTOR ENDPOINTS)
// ============================================================================

endUsage
    : END multiplicityClause? endModifiers? endMemberKind? usageName? featureRelationships? valueInit? usageBody?
    | END multiplicityClause? endModifiers? endMemberKind? usageName? featureRelationships? valueInit? SEMICOLON
    | END usageName? multiplicityClause? endModifiers? endMemberKind? usageName? featureRelationships? valueInit? usageBody?
    | END usageName? multiplicityClause? endModifiers? endMemberKind? usageName? featureRelationships? valueInit? SEMICOLON
    | END featureRelationships SEMICOLON?
    ;

endModifiers
    : (NONUNIQUE | ORDERED | REF)+
    ;

endMemberKind
    : PART
    | ITEM
    | PORT
    | REF
    | ACTION
    | STATE
    | CONNECTION
    | INTERFACE
    | FLOW
    | ATTRIBUTE
    ;

// ============================================================================
// NAMES AND DECLARATIONS
// ============================================================================

declarationName
    : name (shortName)?
    | shortName (name)?
    | shortName
    ;

usageName
    : name (shortName)?
    | shortName
    ;

name
    : ID
    | QUOTED_ID
    ;

shortName
    : UNRESTRICTED_NAME
    ;

qualifiedName
    : name (DOUBLE_COLON name)*
    ;

featureChain
    : name (DOT name | DOUBLE_COLON name)*
    ;

// ============================================================================
// TYPE RELATIONSHIPS (AFTER DEFINITION/USAGE NAME)
// ============================================================================

typeRelationships
    : typeRelationship+
    ;

typeRelationship
    : specializesClause
    | redefinesClause
    | subsetsClause
    | referencesClause
    | conjugatesClause
    | typingClause
    | unionsClause
    | intersectsClause
    | differencesClause
    | disjointClause
    ;

featureRelationships
    : featureRelationship+
    ;

featureRelationship
    : typeRelationship
    | multiplicityClause
    | valueInit
    ;

specializesClause
    : (SPECIALIZES_OP | SPECIALIZES) qualifiedName (COMMA qualifiedName)*
    ;

redefinesClause
    : (REDEFINES_OP | REDEFINES) (featureChain | qualifiedName) (COMMA (featureChain | qualifiedName))*
    ;

subsetsClause
    : SUBSETS qualifiedName (COMMA qualifiedName)*
    ;

referencesClause
    : (REFERENCE_SUBSETTING | REFERENCES) (featureChain | qualifiedName) (COMMA (featureChain | qualifiedName))*
    ;

conjugatesClause
    : (TILDE | CONJUGATES) qualifiedName
    ;

typingClause
    : COLON TILDE? qualifiedName (COMMA TILDE? qualifiedName)*
    ;

unionsClause
    : UNIONS qualifiedName (COMMA qualifiedName)*
    ;

intersectsClause
    : INTERSECTS qualifiedName (COMMA qualifiedName)*
    ;

differencesClause
    : DIFFERENCES qualifiedName (COMMA qualifiedName)*
    ;

disjointClause
    : DISJOINT FROM? qualifiedName (COMMA qualifiedName)*
    ;

multiplicityClause
    : LBRACK multiplicityRange RBRACK
    ;

multiplicityRange
    : multiplicityBound (DOTDOT multiplicityBound)?
    | STAR
    ;

multiplicityBound
    : STAR
    | INTEGER
    | qualifiedName
    ;

// ============================================================================
// CONNECTION AND FLOW ENDPOINTS
// ============================================================================

connectionEndpoints
    : CONNECT? multiplicityClause? expression TO multiplicityClause? expression
    | CONNECT LPAREN expressionList RPAREN
    ;

expressionList
    : expression (COMMA expression)*
    ;

flowEndpoints
    : FROM expression TO expression
    | OF expression FROM expression TO expression
    ;

successionEndpoints
    : FIRST expression THEN expression
    | FROM expression TO expression
    | expression THEN expression
    ;

transitionRelationships
    : FIRST expression
    ;

// ============================================================================
// BODIES
// ============================================================================

definitionBody
    : LBRACE definitionBodyElement* RBRACE
    | SEMICOLON
    ;

definitionBodyElement
    : namespaceBodyElement
    | returnFeature
    | inParameter
    | outParameter
    | inoutParameter
    | simpleFeature
    | statement
    ;

simpleFeature
    : usageName? featureRelationships valueInit? SEMICOLON
    | FEATURE REDEFINES qualifiedName featureRelationships? valueInit? usageBody?
    | FEATURE REDEFINES qualifiedName featureRelationships? valueInit? SEMICOLON
    | FEATURE usageName? featureRelationships valueInit? usageBody?
    | FEATURE usageName? featureRelationships valueInit? SEMICOLON
    ;

returnFeature
    : RETURN usageName? featureRelationships? (SEMICOLON | usageBody)
    ;

inParameter
    : IN usageName? featureRelationships? parameterValueInit? usageBody
    | IN usageName? featureRelationships? parameterValueInit? SEMICOLON
    ;

parameterValueInit
    : EQUALS expression
    | EQUALS LPAREN argumentList RPAREN
    | COLON_EQUALS expression
    ;

outParameter
    : OUT usageName? featureRelationships? valueInit? usageBody
    | OUT usageName? featureRelationships? valueInit? SEMICOLON
    ;

inoutParameter
    : INOUT usageName? featureRelationships? valueInit? usageBody
    | INOUT usageName? featureRelationships? valueInit? SEMICOLON
    ;

usageBody
    : LBRACE usageBodyElement* RBRACE
    | SEMICOLON
    ;

usageBodyElement
    : namespaceBodyElement
    | statement
    | anonymousRedefines
    | returnFeature
    | inParameter
    | outParameter
    | inoutParameter
    | subjectDeclaration
    | objectiveRequirement
    ;

anonymousRedefines
    : REDEFINES_OP qualifiedName DEFAULT? valueInit? metaExpression? SEMICOLON
    | REDEFINES_OP qualifiedName featureRelationships? valueInit? metaExpression? SEMICOLON
    ;

metaExpression
    : META qualifiedName
    ;

stateDefinitionBody
    : LBRACE stateBodyElement* RBRACE
    ;

stateUsageBody
    : LBRACE stateBodyElement* RBRACE
    | SEMICOLON
    ;

stateBodyElement
    : namespaceBodyElement
    | entryAction
    | doAction
    | exitAction
    | transitionUsage
    | acceptTransition
    | thenStatement
    ;

acceptTransition
    : ACCEPT usageName? (COLON qualifiedName)? (VIA expression)? doActionClause? THEN expression SEMICOLON?
    ;

doActionClause
    : DO (ACTION usageName? featureRelationships? | SEND expression TO expression | expression)
    ;

requirementBody
    : LBRACE requirementBodyElement* RBRACE
    | SEMICOLON
    ;

requirementBodyElement
    : namespaceBodyElement
    | subjectDeclaration
    | framedConcern
    | assumeConstraint
    | requireConstraint
    | objectiveRequirement
    ;

viewBody
    : LBRACE viewBodyElement* RBRACE
    | SEMICOLON
    ;

viewBodyElement
    : namespaceBodyElement
    | exposeStatement
    | renderStatement
    | filterStatement
    ;

enumBody
    : LBRACE enumBodyElement* RBRACE
    ;

enumBodyElement
    : enumMember
    | documentation
    ;

enumMember
    : visibility? ENUM name (EQUALS expression)? SEMICOLON?
    | visibility? ENUM name enumMemberBody
    | visibility? name SEMICOLON?
    | EQUALS expression SEMICOLON?
    ;

enumMemberBody
    : LBRACE enumMemberBodyElement* RBRACE
    ;

enumMemberBodyElement
    : redefinesClause EQUALS expression SEMICOLON?
    | namespaceBodyElement
    ;

transitionBody
    : acceptClause? guardClause? effectClause? THEN expression SEMICOLON?
    ;

// ============================================================================
// STATE ELEMENTS
// ============================================================================

entryAction
    : ENTRY assignStatement
    | ENTRY assignUsage
    | ENTRY expression? SEMICOLON
    ;

doAction
    : DO ACTION? usageName? featureRelationships? SEMICOLON
    | DO ACTION? expression SEMICOLON
    | DO SEND expression TO expression SEMICOLON
    | DO assignStatement
    | DO assignUsage
    ;

exitAction
    : EXIT assignStatement
    | EXIT expression? SEMICOLON
    ;

assignStatement
    : ASSIGN qualifiedName COLON_EQUALS expression SEMICOLON
    ;

acceptClause
    : ACCEPT name? (COLON qualifiedName)? (VIA expression)?
    ;

guardClause
    : IF expression
    ;

effectClause
    : DO ACTION usageName? featureRelationships?
    | DO (SEND expression TO expression | expression)
    ;

// ============================================================================
// REQUIREMENT ELEMENTS
// ============================================================================

subjectDeclaration
    : SUBJECT SEMICOLON
    | SUBJECT usageName? featureRelationships? valueInit? SEMICOLON?
    | SUBJECT usageName? featureRelationships? valueInit? usageBody
    ;

framedConcern
    : FRAME CONCERN? qualifiedName SEMICOLON
    | FRAME expression SEMICOLON
    ;

assumeConstraint
    : ASSUME CONSTRAINT usageName? featureRelationships? (expression | usageBody)? SEMICOLON?
    ;

requireConstraint
    : REQUIRE CONSTRAINT? usageName? featureRelationships? expression? SEMICOLON?
    | REQUIRE expression SEMICOLON?
    ;

objectiveRequirement
    : OBJECTIVE objectiveBody
    | OBJECTIVE usageName? featureRelationships? objectiveBody?
    | OBJECTIVE verifyBody
    ;

objectiveBody
    : LBRACE objectiveBodyElement* RBRACE
    ;

objectiveBodyElement
    : namespaceBodyElement
    | subjectDeclaration
    | usageName EQUALS expression SEMICOLON
    ;

verifyBody
    : LBRACE verifyBodyElement* RBRACE
    ;

verifyBodyElement
    : namespaceBodyElement
    | verifyRequirement
    ;

verifyRequirement
    : VERIFY REQUIREMENT? usageName? featureRelationships? SEMICOLON?
    | VERIFY expression SEMICOLON?
    ;

// ============================================================================
// VIEW ELEMENTS
// ============================================================================

exposeStatement
    : EXPOSE qualifiedNameWithWildcard (filterClause)* SEMICOLON
    ;

renderStatement
    : RENDER (AS qualifiedName | ID)? SEMICOLON
    | RENDER RENDERING usageName? featureRelationships? SEMICOLON
    | RENDER expression SEMICOLON
    ;

filterStatement
    : FILTER expression SEMICOLON
    ;

filterClause
    : LBRACK expression RBRACK
    ;

// ============================================================================
// VALUES AND INITIALIZATION
// ============================================================================

valueInit
    : EQUALS expression
    | COLON_EQUALS expression
    | DEFAULT (EQUALS | COLON_EQUALS)? expression
    ;

// ============================================================================
// STATEMENTS
// ============================================================================

statement
    : expressionStatement
    | ifStatement
    | whileStatement
    | loopStatement
    | forStatement
    | assignmentStatement
    | sendStatement
    | acceptStatement
    | flowStatement
    | terminateStatement
    | invariantStatement
    | thenStatement
    | assignStatement
    | doStatement
    ;

thenStatement
    : THEN MERGE usageName? SEMICOLON
    | THEN ACCEPT usageName? featureRelationships? acceptTiming? SEMICOLON?
    | THEN SEND expression (VIA expression)? TO expression SEMICOLON
    | THEN DECIDE SEMICOLON? decideBody?
    | THEN FORK usageName? usageBody?
    | THEN FORK usageName? SEMICOLON
    | THEN JOIN usageName? SEMICOLON
    | THEN TERMINATE expression? SEMICOLON
    | THEN ACTION usageName? featureRelationships? usageBody
    | THEN ACTION usageName? WHILE expression LBRACE statement* RBRACE (UNTIL expression)? SEMICOLON?
    | THEN STATE usageName? featureRelationships? stateUsageBody
    | THEN STATE usageName? featureRelationships? SEMICOLON
    | THEN WHILE expression LBRACE statement* RBRACE (UNTIL expression)? SEMICOLON?
    | THEN expression SEMICOLON?
    ;

invariantStatement
    : INV usageBody
    ;

flowStatement
    : FIRST (START | DONE | expression) SEMICOLON
    | THEN (START | DONE | expression) SEMICOLON
    ;

terminateStatement
    : TERMINATE expression? SEMICOLON
    | TERMINATE usageName SEMICOLON
    ;

doStatement
    : DO ACTION usageName? featureRelationships? usageBody?
    | DO ACTION usageName? featureRelationships? SEMICOLON
    ;

expressionStatement
    : expression SEMICOLON?
    ;

ifStatement
    : IF expression THEN statement (ELSE statement)?
    | IF expression LBRACE statement* RBRACE elseIfClause* elseClause?
    ;

elseIfClause
    : ELSE IF expression LBRACE statement* RBRACE
    ;

elseClause
    : ELSE LBRACE statement* RBRACE
    ;

whileStatement
    : WHILE expression LBRACE statement* RBRACE
    ;

loopStatement
    : LOOP expression? LBRACE statement* RBRACE (UNTIL expression)?
    ;

forStatement
    : FOR usageName featureRelationships? IN expression LBRACE statement* RBRACE
    ;

assignmentStatement
    : (qualifiedName | featureChain) (EQUALS | COLON_EQUALS | PLUS_EQUALS) expression SEMICOLON
    ;

sendStatement
    : SEND expression (VIA expression)? TO expression SEMICOLON
    ;

acceptStatement
    : ACCEPT expression (VIA expression)? SEMICOLON
    ;

// ============================================================================
// EXPRESSIONS
// ============================================================================

expression
    : conditionalExpression
    ;

conditionalExpression
    : nullCoalescingExpression (QUESTION expression COLON expression)?
    ;

nullCoalescingExpression
    : impliesExpression (NULL_COALESCING impliesExpression)*
    ;

impliesExpression
    : orExpression (IMPLIES orExpression)*
    ;

orExpression
    : xorExpression (OR xorExpression)*
    ;

xorExpression
    : andExpression (XOR andExpression)*
    ;

andExpression
    : equalityExpression (AND equalityExpression)*
    ;

equalityExpression
    : classificationExpression ((EQ | NE | SAME | NOT_SAME) classificationExpression)*
    ;

classificationExpression
    : relationalExpression ((HASTYPE | ISTYPE | AT_SIGN | META | AS) qualifiedName)?
    ;

relationalExpression
    : additiveExpression ((LT | GT | LE | GE) additiveExpression)*
    ;

additiveExpression
    : multiplicativeExpression ((PLUS | MINUS) multiplicativeExpression)*
    ;

multiplicativeExpression
    : exponentiationExpression ((STAR | SLASH | PERCENT) exponentiationExpression)*
    ;

exponentiationExpression
    : unaryExpression (DOUBLE_STAR unaryExpression)*
    ;

unaryExpression
    : MINUS unaryExpression
    | BANG unaryExpression
    | NOT unaryExpression
    | TILDE unaryExpression
    | primaryExpression
    ;

primaryExpression
    : baseExpression (sequenceSuffix)*
    ;

sequenceSuffix
    : DOT name                                            // Member access
    | ARROW name LPAREN argumentList? RPAREN              // Arrow operation
    | LBRACK expression RBRACK                            // Index
    | LPAREN argumentList? RPAREN                         // Invocation
    ;

baseExpression
    : literalExpression
    | nameExpression
    | invocationExpression
    | bodyExpression
    | metadataAccessExpression
    | newExpression
    | thisExpression
    | START
    | DONE
    | LPAREN expression RPAREN                            // Parenthesized
    ;

thisExpression
    : THIS
    ;

newExpression
    : NEW qualifiedName LPAREN argumentList? RPAREN
    ;

literalExpression
    : INTEGER
    | REAL
    | DECIMAL
    | HEX_INTEGER
    | BINARY_INTEGER
    | STRING
    | TRUE
    | FALSE
    | NULL
    ;

nameExpression
    : qualifiedName
    ;

invocationExpression
    : qualifiedName LPAREN argumentList? RPAREN
    ;

bodyExpression
    : LBRACE expression RBRACE
    ;

metadataAccessExpression
    : qualifiedName DOT AT_SIGN qualifiedName
    ;

argumentList
    : namedArgument (COMMA namedArgument)*
    | positionalArgument (COMMA positionalArgument)*
    ;

namedArgument
    : name EQUALS expression
    ;

positionalArgument
    : expression
    ;

// ============================================================================
// DOCUMENTATION
// ============================================================================

comment
    : COMMENT_KW usageName? (ABOUT qualifiedName)? (LOCALE STRING)? documentationBody? SEMICOLON?
    ;

documentation
    : DOC (LOCALE STRING)? documentationBody? SEMICOLON?
    | REP usageName? (LANGUAGE STRING)? documentationBody? SEMICOLON?
    | LOCALE STRING documentationBody? SEMICOLON?
    ;

documentationBody
    : STRING
    | REGULAR_EXPRESSION
    ;

textualRepresentation
    : REP usageName (LANGUAGE STRING)? documentationBody?
    ;

languageStatement
    : LANGUAGE STRING documentationBody? SEMICOLON?
    ;
