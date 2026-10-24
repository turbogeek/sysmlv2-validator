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
      | implicitFeatureDeclaration
      | member
      | metadataAnnotation
      | filterStatement
      )
    ;

packageDeclaration
    : prefixes? PACKAGE shortName? qualifiedName? packageBody
    ;

libraryPackageDeclaration
    : prefixes? STANDARD? LIBRARY PACKAGE shortName? qualifiedName? packageBody
    ;

namespaceDeclaration
    : prefixes? NAMESPACE shortName? qualifiedName? namespaceBody
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
    : ALIAS aliasName FOR qualifiedName (SEMICOLON | LBRACE (documentation | comment | metadataAnnotation)* RBRACE)
    ;

implicitFeatureDeclaration
    : declarationName featureRelationships? valueInit? (SEMICOLON | usageBody)
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
    : AT_SIGN qualifiedName (ABOUT qualifiedName (COMMA qualifiedName)*)? (LPAREN annotationArguments RPAREN)?
    | AT_SIGN qualifiedName (ABOUT qualifiedName (COMMA qualifiedName)*)? metadataBody
    | AT_SIGN qualifiedName (ABOUT qualifiedName (COMMA qualifiedName)*)? SEMICOLON?
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
    | directionPrefix
    | VAR                                                 // KerML 'var'
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
      | individualDefinition
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
      | multiplicityMember
      | kermlRelationshipMember
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
      | invariantStatement
      )
    ;

// ============================================================================
// SYSML DEFINITIONS
// ============================================================================

partDefinition
    : PART_DEF declarationName? typeRelationships? definitionBody
    ;

actionDefinition
    : ACTION_DEF declarationName? typeRelationships? definitionBody
    ;

stateDefinition
    : STATE_DEF declarationName? typeRelationships? PARALLEL? stateDefinitionBody
    ;

requirementDefinition
    : REQUIREMENT_DEF declarationName? typeRelationships? requirementBody
    ;

viewDefinition
    : VIEW_DEF declarationName? typeRelationships? viewBody
    ;

viewpointDefinition
    : VIEWPOINT_DEF declarationName? typeRelationships? viewpointBody
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
    : CONSTRAINT_DEF declarationName? typeRelationships? constraintDefBody
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
    | inoutParameter
    | returnFeature
    | anonymousRedefines
    ;

attributeDefinition
    : ATTRIBUTE_DEF declarationName? typeRelationships? definitionBody
    | ATTRIBUTE_DEF declarationName? typeRelationships? SEMICOLON
    ;

portDefinition
    : PORT_DEF declarationName? typeRelationships? definitionBody
    ;

connectionDefinition
    : CONNECTION_DEF declarationName? typeRelationships? definitionBody
    ;

interfaceDefinition
    : INTERFACE_DEF declarationName? typeRelationships? definitionBody
    ;

allocationDefinition
    : ALLOCATION_DEF declarationName? typeRelationships? definitionBody
    ;

itemDefinition
    : INDIVIDUAL? ITEM_DEF declarationName? typeRelationships? definitionBody
    ;

enumDefinition
    : ENUM_DEF declarationName? typeRelationships? enumBody
    | ENUM_DEF declarationName? typeRelationships? SEMICOLON
    ;

calcDefinition
    : CALC_DEF declarationName? typeRelationships? definitionBody
    ;

analysisDefinition
    : ANALYSIS_DEF declarationName? typeRelationships? definitionBody
    ;

caseDefinition
    : CASE_DEF declarationName? typeRelationships? definitionBody
    ;

useCaseDefinition
    : USE_CASE_DEF declarationName? typeRelationships? definitionBody
    ;

verificationDefinition
    : VERIFICATION_DEF declarationName? typeRelationships? definitionBody
    ;

concernDefinition
    : CONCERN_DEF declarationName? typeRelationships? definitionBody
    ;

renderingDefinition
    : RENDERING_DEF declarationName? typeRelationships? definitionBody
    ;

occurrenceDefinition
    : INDIVIDUAL? OCCURRENCE_DEF declarationName? typeRelationships? definitionBody
    ;

flowDefinition
    : FLOW_DEF declarationName? typeRelationships? definitionBody
    ;

metadataDefinition
    : METADATA_DEF declarationName? shortName? typeRelationships? metadataDefBody?
    | METADATA_DEF declarationName? shortName? typeRelationships? SEMICOLON
    ;

individualDefinition
    : INDIVIDUAL_DEF declarationName? typeRelationships? definitionBody
    | INDIVIDUAL_DEF declarationName? typeRelationships? SEMICOLON
    ;

metadataDefBody
    : LBRACE metadataDefBodyElement* RBRACE
    ;

metadataDefBodyElement
    : namespaceBodyElement
    | anonymousRedefines
    | simpleFeature
    ;

// ============================================================================
// KERML DEFINITIONS
// ============================================================================

datatypeDefinition
    : DATATYPE ALL? declarationName? multiplicityClause? typeRelationships? (definitionBody | SEMICOLON)?
    ;

classDefinition
    : CLASS ALL? declarationName? multiplicityClause? typeRelationships? (definitionBody | SEMICOLON)?
    ;

structDefinition
    : STRUCT ALL? declarationName? multiplicityClause? typeRelationships? (definitionBody | SEMICOLON)?
    ;

assocDefinition
    : (ASSOC_STRUCT | ASSOC) ALL? declarationName? multiplicityClause? typeRelationships? (definitionBody | SEMICOLON)?
    ;

behaviorDefinition
    : BEHAVIOR ALL? declarationName? multiplicityClause? typeRelationships? (definitionBody | SEMICOLON)?
    ;

functionDefinition
    : FUNCTION ALL? declarationName? multiplicityClause? typeRelationships? (definitionBody | SEMICOLON)?
    ;

predicateDefinition
    : PREDICATE ALL? declarationName? multiplicityClause? typeRelationships? (definitionBody | SEMICOLON)?
    ;

interactionDefinition
    : INTERACTION ALL? declarationName? multiplicityClause? typeRelationships? (definitionBody | SEMICOLON)?
    ;

metaclassDefinition
    : METACLASS ALL? declarationName? multiplicityClause? typeRelationships? (definitionBody | SEMICOLON)?
    ;

classifierDefinition
    : CLASSIFIER ALL? declarationName? multiplicityClause? typeRelationships? (definitionBody | SEMICOLON)?
    ;

typeDefinition
    : TYPE ALL? declarationName? multiplicityClause? typeRelationships? (definitionBody | SEMICOLON)?
    ;

featureDefinition
    : (FEATURE | kermlFeatureKeyword) ALL? usageName? featureRelationships? usageBody?
    ;

// KerML 'bool' and 'expr' features
kermlFeatureKeyword
    : BOOL
    | EXPR
    ;

multiplicityMember
    : MULTIPLICITY usageName? multiplicityClause? featureRelationships? usageBody
    ;

connectorDefinition
    : CONNECTOR ALL? connectionEnd TO connectionEnd usageBody
    | CONNECTOR ALL? usageName? featureRelationships?
      (FROM connectionEnd TO connectionEnd | LPAREN connectionEnd (COMMA connectionEnd)* RPAREN)? usageBody
    ;

// KerML 'binding [decl of] a = b' and SysML 'binding [decl] bind a = b'
bindingConnectorDefinition
    : BINDING ALL? (usageName? featureRelationships? (OF | BIND))? connectionEnd EQUALS connectionEnd usageBody
    | BINDING ALL? usageName? featureRelationships? usageBody
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
    : directionPrefix? ACTION usageName? featureRelationships? ASSIGN expression COLON_EQUALS expression SEMICOLON?
    | directionPrefix? ACTION usageName? featureRelationships? WHILE expression LBRACE usageBodyElement* RBRACE (UNTIL expression)? SEMICOLON?
    | directionPrefix? ACTION usageName? featureRelationships? LOOP LBRACE usageBodyElement* RBRACE (UNTIL expression)? SEMICOLON?
    | directionPrefix? ACTION usageName? featureRelationships? (actionKeyword | SEND actionPayload? (VIA expression)? (TO expression)? | ACCEPT actionPayload? (VIA expression)?)? usageBody?
    | directionPrefix? ACTION usageName? featureRelationships? (actionKeyword | SEND actionPayload? (VIA expression)? (TO expression)? | ACCEPT actionPayload? (VIA expression)?)? SEMICOLON
    ;

actionPayload
    : expression featureRelationships?
    ;

actionKeyword
    : DECIDE
    | MERGE
    | FORK
    | JOIN
    ;

stateUsage
    : PARALLEL? STATE usageName? PARALLEL? featureRelationships? PARALLEL? stateUsageBody?
    ;

requirementUsage
    : REQUIREMENT usageName? featureRelationships? requirementBody?
    ;

viewUsage
    : VIEW usageName? featureRelationships? viewBody?
    ;

constraintUsage
    : CONSTRAINT usageName? featureRelationships? constraintBody?
    | CONSTRAINT usageName? featureRelationships? SEMICOLON
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
    | constraintParameter
    ;

constraintParameter
    : name COLON qualifiedName SEMICOLON
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
    : directionPrefix? INDIVIDUAL? ITEM usageName? featureRelationships? usageBody?
    ;

refUsage
    : directionPrefix? REF hashAnnotation* refKind? usageName? featureRelationships? valueInit? (SEMICOLON | usageBody)
    | hashAnnotation+ usageName featureRelationships? valueInit? SEMICOLON
    ;

refKind
    : PART
    | ACTION
    | STATE
    | ATTRIBUTE
    | PORT
    | ITEM
    | OCCURRENCE
    | EVENT OCCURRENCE?
    | CALC
    | CONSTRAINT
    | REQUIREMENT
    | CONNECTION
    | INTERFACE
    | ALLOCATION
    | FLOW
    | VIEW
    | RENDERING
    | CONCERN
    | ANALYSIS
    | CASE
    | VERIFICATION
    | METADATA
    ;

connectionUsage // Reference: SysMLv2.md, Section 7.13.2 / Line 2416
    : CONNECTION usageName? featureRelationships? connectionEndpoints? usageBody?
    | CONNECTION usageName? featureRelationships? connectionEndpoints? SEMICOLON
    | CONNECT connectionEndpoints SEMICOLON?
    ;

flowConnectionUsage
    : FLOW usageName? featureRelationships? flowEndpoints? usageBody?
    | FLOW usageName? featureRelationships? flowEndpoints? SEMICOLON?
    | FLOW expression TO expression SEMICOLON
    ;

successionUsage // Reference: SysMLv2.md, Section 7.17.4 / Line 3317
    : FIRST expression (THEN expression)? SEMICOLON?
    | THEN expression SEMICOLON?
    | SUCCESSION FLOW? ALL? usageName? featureRelationships? successionFlowOf? successionEndpoints? (SEMICOLON | usageBody)?
    ;

successionFlowOf
    : OF qualifiedName
    ;

transitionUsage
    : TRANSITION usageName? transitionRelationships? transitionBody
    | TRANSITION usageName? FIRST expression transitionBody
    | expression TRANSITION expression SEMICOLON? // Reference: SysMLv2.md, Section 7.18.3 / Line 4213
    ;

satisfyRequirement
      : ASSERT? NOT? SATISFY REQUIREMENT? usageName? featureRelationships? (BY expression)? requirementBody?
      | ASSERT? NOT? SATISFY expression (BY expression)? featureRelationships? requirementBody?
      ;

allocateUsage
    : ALLOCATION usageName? featureRelationships? usageBody?
    | ALLOCATION usageName? featureRelationships? ALLOCATE LPAREN namedAllocationList RPAREN SEMICOLON?
    | ALLOCATE expression TO expression (SEMICOLON | usageBody)
    ;

namedAllocationList
    : namedAllocation (COMMA namedAllocation)*
    ;

namedAllocation
    : name REFERENCE_SUBSETTING expression
    ;

performUsage
    : PERFORM ACTION? expression (BY expression)? featureRelationships? usageBody?
    | PERFORM ACTION? usageName? featureRelationships? usageBody
    ;

exhibitUsage
    : EXHIBIT STATE? expression (BY expression)? featureRelationships? usageBody?
    | EXHIBIT PARALLEL? STATE? usageName? PARALLEL? featureRelationships? stateUsageBody
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
    | SNAPSHOT usageName? featureRelationships? valueInit? usageBody?
    | SNAPSHOT usageName? featureRelationships? valueInit? SEMICOLON
    | TIMESLICE usageName? featureRelationships? valueInit? usageBody?
    | TIMESLICE usageName? featureRelationships? valueInit? SEMICOLON
    | EVENT OCCURRENCE? usageName? featureRelationships? usageBody?
    | EVENT OCCURRENCE? usageName? featureRelationships? SEMICOLON
    | EVENT expression SEMICOLON?
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
    : ACCEPT usageName? featureRelationships? (VIA expression)? acceptTiming? SEMICOLON?
    ;

acceptTiming
    : AFTER expression
    | AT expression
    | WHEN expression
    ;

sendUsage
    : SEND actionPayload? (VIA expression)? (TO expression)? SEMICOLON?
    ;

assignUsage
    : ASSIGN expression COLON_EQUALS expression SEMICOLON?
    ;

messageUsage
    : MESSAGE usageName? featureRelationships? flowEndpoints? usageBody?
    | MESSAGE usageName? featureRelationships? flowEndpoints? SEMICOLON
    ;

// SysML v2 8.2.2.3: 'dependency' ( Identification 'from' )? client ( ',' client )* 'to' supplier ( ',' supplier )*
// RelationshipBody, where RelationshipBody = ';' | '{' ... '}' (e.g. an owned doc)
dependencyDeclaration
    : DEPENDENCY usageName? (FROM expression (COMMA expression)*)? TO expression (COMMA expression)* usageBody
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
    | usageName metadataBody
    | metadataAnnotation
    ;

// ============================================================================
// END USAGES (CONNECTOR ENDPOINTS)
// ============================================================================

endUsage
    : END multiplicityClause? endModifiers? endMemberKind? usageName? hashAnnotation* featureRelationships? valueInit? usageBody?
    | END multiplicityClause? endModifiers? endMemberKind? usageName? hashAnnotation* featureRelationships? valueInit? SEMICOLON
    | END usageName? multiplicityClause? endModifiers? endMemberKind? usageName? hashAnnotation* featureRelationships? valueInit? usageBody?
    | END usageName? multiplicityClause? endModifiers? endMemberKind? usageName? hashAnnotation* featureRelationships? valueInit? SEMICOLON
    | END hashAnnotation* featureRelationships SEMICOLON?
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
    | shortName (name)?
    | shortName
    ;

name
    : ID
    | QUOTED_ID
    | DONE
    | START
    | THIS
    | nonReservedKeyword
    | kermlOnlyKeyword
    ;

// KerML keywords that SysML v2 does not reserve (SysML v2 8.2.2.1.2). Cameo 26xR2 and the Dassault
// libraries use them as names, e.g. 'in feature : Feature;' in DassaultSystemesFunctions.
kermlOnlyKeyword
    : NAMESPACE | DATATYPE | CLASS | STRUCT | ASSOC | BEHAVIOR | STEP | FUNCTION | PREDICATE
    | INTERACTION | METACLASS | CLASSIFIER | TYPE | FEATURE | CONNECTOR | INV
    | BOOL | EXPR | VAR | INVERSE | SUBSET | SUBCLASSIFIER | SUBTYPE | SPECIALIZATION | REDEFINITION
    ;

// KerML relationship members (KerML 8.2.3.3): 'subset a.b subsets c.d;', 'subclassifier A specializes B;'
kermlRelationshipMember
    : (SPECIALIZATION usageName?)? (SUBTYPE | SUBCLASSIFIER) featureChain (SPECIALIZES | SPECIALIZES_OP) featureChain usageBody
    | (SPECIALIZATION usageName?)? SUBSET featureChain (SUBSETS | SPECIALIZES_OP) featureChain usageBody
    | (SPECIALIZATION usageName?)? REDEFINITION featureChain (REDEFINES | REDEFINES_OP) featureChain usageBody
    | DISJOINT featureChain FROM featureChain usageBody
    ;

// Words that the lexer has keyword tokens for although no parser rule uses them and SysML v2 does not reserve them
// (SysML v2 8.2.2.1.2), so they are ordinary names: 'attribute value : String;' is valid SysML v2.
// NonReservedWordNameTest keeps this list to exactly the keyword tokens that no other rule refers to.
nonReservedKeyword
    : ANY
    | CHAINS
    | FEATURING
    | INVERSES
    | MULTIPLICITY
    | SEQUENCE
    | TYPING
    | VALUE
    ;

shortName
    : LT name GT
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
    | INVERSE OF (featureChain | qualifiedName)
    | multiplicityClause
    | valueInit
    | READONLY
    | PORTION
    | ORDERED
    | NONUNIQUE
    ;

specializesClause
    : (SPECIALIZES_OP | SPECIALIZES) (featureChain | qualifiedName) (COMMA (featureChain | qualifiedName))*
    ;

redefinesClause
    : (REDEFINES_OP | REDEFINES) (featureChain | qualifiedName) (COMMA (featureChain | qualifiedName))*
    ;

subsetsClause
    : SUBSETS (featureChain | qualifiedName) (COMMA (featureChain | qualifiedName))*
    ;

referencesClause
    : (REFERENCE_SUBSETTING | REFERENCES) (featureChain | qualifiedName) (COMMA (featureChain | qualifiedName))*
    ;

conjugatesClause
    : (TILDE | CONJUGATES) qualifiedName // Reference: SysMLv2.md, Section 7.12.3 / Line 2158
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
    : CONNECT? connectionEnd TO connectionEnd
    | CONNECT LPAREN connectionEnd (COMMA connectionEnd)* RPAREN
    ;

connectionEnd
    : multiplicityClause? (name (REFERENCE_SUBSETTING | REFERENCES))? expression
    ;

expressionList
    : expression (COMMA expression)*
    ;

flowEndpoints
    : FROM expression TO expression
    | OF actionPayload (FROM expression)? (TO expression)?
    ;

successionEndpoints
    : FIRST connectionEnd THEN connectionEnd
    | FROM connectionEnd TO connectionEnd
    | connectionEnd THEN connectionEnd
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
    : usageName? featureRelationships valueInit? (SEMICOLON | usageBody)
    | FEATURE REDEFINES qualifiedName featureRelationships? valueInit? usageBody?
    | FEATURE REDEFINES qualifiedName featureRelationships? valueInit? SEMICOLON
    | FEATURE usageName? featureRelationships valueInit? usageBody?
    | FEATURE usageName? featureRelationships valueInit? SEMICOLON
    ;

returnFeature
    : RETURN (REF | FEATURE | PART | ATTRIBUTE | PORT | ITEM | ACTION | STATE | REQUIREMENT | CALC | CONSTRAINT | EVENT | OCCURRENCE)? usageName? featureRelationships? valueInit? (SEMICOLON | usageBody)
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
    | simpleParameter
    | metadataAnnotation
    ;

simpleParameter
    : (ID | OBJECTIVE | SUBJECT) COLON qualifiedName SEMICOLON
    ;

anonymousRedefines
    : (REDEFINES | REDEFINES_OP) redefinedList DEFAULT? valueInit? metaExpression? (SEMICOLON | usageBody)
    | (REDEFINES | REDEFINES_OP) redefinedList featureRelationships? valueInit? metaExpression? (SEMICOLON | usageBody)
    ;

metaExpression
    : META qualifiedName
    ;

redefinedList
    : (featureChain | qualifiedName) (COMMA (featureChain | qualifiedName))*
    ;

stateDefinitionBody
    : LBRACE stateBodyElement* RBRACE
    | SEMICOLON
    ;

stateUsageBody
    : LBRACE stateBodyElement* RBRACE
    | SEMICOLON
    ;

stateBodyElement // Reference: SysMLv2.md, Section 7.18.2 / Line 4035
    : namespaceBodyElement
    | entryAction
    | doAction
    | exitAction
    | transitionUsage
    | acceptTransition
    | thenStatement
    | inParameter
    | outParameter
    | inoutParameter
    ;

acceptTransition
    : ACCEPT usageName? (COLON qualifiedName)? (VIA expression)? doActionClause? THEN expression SEMICOLON?
    ;

doActionClause
    : DO (ACTION usageName? featureRelationships? | SEND expression (VIA expression)? (TO expression)? | expression)
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
    | verifyRequirement
    | statement
    | anonymousRedefines
    | returnFeature
    | inParameter
    | outParameter
    | inoutParameter
    | simpleParameter
    | metadataAnnotation
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
    : visibility? hashAnnotation* ENUM name? (COLON qualifiedName)? (EQUALS expression)? SEMICOLON?
    | visibility? hashAnnotation* ENUM name? enumMemberBody
    | visibility? hashAnnotation* name enumMemberBody
    | visibility? hashAnnotation* name (COLON qualifiedName)? (EQUALS expression)? SEMICOLON?
    | hashAnnotation* EQUALS expression SEMICOLON?
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
    : ENTRY actionUsage
    | ENTRY ACTION? usageName? featureRelationships? usageBody
    | ENTRY assignStatement
    | ENTRY assignUsage
    | ENTRY sendStatement
    | ENTRY expression? SEMICOLON
    ;

doAction
    : DO actionUsage
    | DO ACTION? usageName? featureRelationships? usageBody
    | DO ACTION? expression SEMICOLON
    | DO sendStatement
    | DO assignStatement
    | DO assignUsage
    ;

exitAction
    : EXIT actionUsage
    | EXIT ACTION? usageName? featureRelationships? usageBody
    | EXIT assignStatement
    | EXIT sendStatement
    | EXIT expression? SEMICOLON
    ;

assignStatement
    : ASSIGN qualifiedName COLON_EQUALS expression SEMICOLON
    ;

acceptClause
    : ACCEPT usageName? (COLON qualifiedName)? (VIA expression)? acceptTiming?
    | ACCEPT qualifiedName (VIA expression)? acceptTiming?
    ;

guardClause
    : IF expression
    ;

effectClause
    : DO ACTION usageName? featureRelationships? (LBRACE usageBodyElement* RBRACE)?
    | DO SEND actionPayload? (VIA expression)? (TO expression)?
    | DO expression
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
    : FRAME CONCERN? usageName? featureRelationships? SEMICOLON
    | FRAME expression SEMICOLON
    ;

assumeConstraint
    : ASSUME CONSTRAINT usageName? featureRelationships? (expression | usageBody)? SEMICOLON?
    ;

requireConstraint
    : REQUIRE CONSTRAINT? usageName? featureRelationships? expression? (requirementBody | SEMICOLON?)
    | REQUIRE expression (requirementBody | SEMICOLON?)
    ;

objectiveRequirement
    : OBJECTIVE requirementBody
    | OBJECTIVE usageName? featureRelationships? (requirementBody | SEMICOLON?)
    ;

verifyRequirement
    : VERIFY REQUIREMENT? usageName? featureRelationships? (requirementBody | SEMICOLON?)
    | VERIFY expression (requirementBody | SEMICOLON?)
    ;

// ============================================================================
// VIEW ELEMENTS
// ============================================================================

exposeStatement
    : EXPOSE qualifiedNameWithWildcard (filterClause)* SEMICOLON
    ;

renderStatement
    : RENDER (AS qualifiedName | ID)? (SEMICOLON | usageBody)
    | RENDER RENDERING usageName? featureRelationships? (SEMICOLON | usageBody)
    | RENDER expression (SEMICOLON | usageBody)
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

valueInit // Reference: SysMLv2.md, Section 7.13.4 / Line 2214
    : EQUALS expression
    | COLON_EQUALS expression
    | DEFAULT (EQUALS | COLON_EQUALS)? expression
    ;

// ============================================================================
// STATEMENTS
// ============================================================================

statement
    : assignmentStatement
    | expressionStatement
    | ifStatement
    | whileStatement
    | loopStatement
    | forStatement
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
    | THEN (ACTION usageName? featureRelationships?)? ACCEPT actionPayload? (VIA expression)? acceptTiming? SEMICOLON?
    | THEN (ACTION usageName? featureRelationships?)? SEND actionPayload? (VIA expression)? (TO expression)? SEMICOLON?
    | THEN DECIDE SEMICOLON? decideBody?
    | THEN FORK usageName? usageBody?
    | THEN FORK usageName? SEMICOLON
    | THEN JOIN usageName? SEMICOLON
    | THEN TERMINATE expression? SEMICOLON
    | THEN ACTION usageName? featureRelationships? usageBody
    | THEN ACTION usageName? WHILE expression LBRACE statement* RBRACE (UNTIL expression)? SEMICOLON?
    | THEN STATE usageName? featureRelationships? stateUsageBody
    | THEN STATE usageName? featureRelationships? SEMICOLON
    | THEN EVENT OCCURRENCE? usageName? featureRelationships? usageBody?
    | THEN EVENT OCCURRENCE? usageName? featureRelationships? SEMICOLON
    | THEN EVENT expression SEMICOLON?
    | THEN WHILE expression LBRACE statement* RBRACE (UNTIL expression)? SEMICOLON?
    | THEN (ACTION usageName? featureRelationships?)? ASSIGN expression COLON_EQUALS expression SEMICOLON?
    | THEN ifStatement
    | THEN visibility? member
    | THEN expression SEMICOLON?
    ;

invariantStatement
    : INV NOT? usageName? usageBody
    ;

flowStatement
    : FIRST (START | DONE | expression) (THEN (START | DONE | expression))? SEMICOLON
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
    | IF expression LBRACE usageBodyElement* RBRACE elseIfClause* elseClause?
    ;

elseIfClause
    : ELSE IF expression LBRACE usageBodyElement* RBRACE
    ;

elseClause
    : ELSE LBRACE usageBodyElement* RBRACE
    ;

whileStatement
    : WHILE expression LBRACE statement* RBRACE
    ;

loopStatement // Reference: SysMLv2.md, Section 7.17.3 / Line 3867
    : LOOP expression? LBRACE statement* RBRACE (UNTIL expression SEMICOLON?)?
    ;

forStatement
    : FOR usageName featureRelationships? IN expression LBRACE statement* RBRACE
    ;

assignmentStatement
    : (qualifiedName | featureChain) (EQUALS | COLON_EQUALS | PLUS_EQUALS) expression SEMICOLON
    ;

sendStatement
    : SEND actionPayload? (VIA expression)? (TO expression)? SEMICOLON
    ;

acceptStatement
    : ACCEPT actionPayload? (VIA expression)? SEMICOLON
    ;

// ============================================================================
// EXPRESSIONS
// ============================================================================

expression
    : conditionalExpression
    ;

conditionalExpression
    : IF nullCoalescingExpression QUESTION expression ELSE expression
    | nullCoalescingExpression (QUESTION expression COLON expression)?
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
    : rangeExpression ((HASTYPE | ISTYPE | AT_SIGN | META | AS) qualifiedName)?
    | (HASTYPE | ISTYPE | AT_SIGN) qualifiedName
    ;

rangeExpression
    : relationalExpression (DOTDOT relationalExpression)?
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
    : unaryExpression ((DOUBLE_STAR | CARET) unaryExpression)*
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
    | DOT METADATA                                        // Metadata access: Type.metadata (KerML MetadataAccessExpression)
    | ARROW name (LPAREN argumentList? RPAREN)? bodyExpression? // Arrow operation
    | ARROW name qualifiedName                            // Arrow operation with a function reference
    | DOT QUESTION (LPAREN argumentList? RPAREN)? bodyExpression? // Filter operation
    | HASH LPAREN expression RPAREN                       // Positional Index
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
    | AS qualifiedName
    | collectionLiteral                                   // Collection literal
    | LPAREN expression RPAREN                            // Parenthesized
    ;

collectionLiteral
    : LPAREN expression (COMMA expression)+ RPAREN        // Tuple/sequence (1, 2, 3)
    | LPAREN RPAREN                                       // Empty sequence (null)
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
    : LBRACE constraintBodyElement* constraintExpression? RBRACE
    ;

metadataAccessExpression
    : (qualifiedName DOT)? AT_SIGN qualifiedName
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
    : COMMENT_KW usageName? (ABOUT qualifiedName (COMMA qualifiedName)*)? (LOCALE STRING)? documentationBody? SEMICOLON?
    ;

documentation
    : DOC (LOCALE STRING)? documentationBody? SEMICOLON?
    | REP usageName? (LANGUAGE STRING)? documentationBody? SEMICOLON?
    | LOCALE STRING documentationBody? SEMICOLON?
    ;

documentationBody
    : STRING
    ;

textualRepresentation
    : REP usageName (LANGUAGE STRING)? documentationBody?
    ;

languageStatement
    : LANGUAGE STRING documentationBody? SEMICOLON?
    ;
