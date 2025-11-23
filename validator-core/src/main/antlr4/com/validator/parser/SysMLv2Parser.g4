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
    : qualifiedName (DOUBLE_COLON DOUBLE_STAR | DOUBLE_COLON STAR)?
    ;

// ============================================================================
// PREFIXES AND MODIFIERS
// ============================================================================

prefixes
    : (metadataAnnotation | featurePrefix)+
    ;

metadataAnnotation
    : AT_SIGN qualifiedName (LPAREN annotationArguments RPAREN)?
    | HASH qualifiedName
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
    ;

usagePrefix
    : REF
    | READONLY
    | DERIVED
    | END
    | ORDERED
    | NONUNIQUE
    | COMPOSITE
    | PORTION
    | INDIVIDUAL
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
    : VIEWPOINT_DEF declarationName typeRelationships? definitionBody
    ;

constraintDefinition
    : CONSTRAINT_DEF declarationName typeRelationships? definitionBody
    ;

attributeDefinition
    : ATTRIBUTE_DEF declarationName typeRelationships? definitionBody
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
    : METADATA_DEF declarationName typeRelationships? definitionBody
    ;

// ============================================================================
// KERML DEFINITIONS
// ============================================================================

datatypeDefinition
    : DATATYPE declarationName typeRelationships? (definitionBody | SEMICOLON)
    ;

classDefinition
    : CLASS declarationName typeRelationships? (definitionBody | SEMICOLON)?
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
    ;

actionUsage
    : directionPrefix? ACTION usageName? featureRelationships? usageBody?
    ;

stateUsage
    : PARALLEL? STATE usageName? featureRelationships? stateUsageBody?
    ;

requirementUsage
    : REQUIREMENT usageName? featureRelationships? requirementBody?
    ;

viewUsage
    : VIEW usageName? featureRelationships? viewBody?
    ;

constraintUsage
    : CONSTRAINT usageName? featureRelationships? usageBody?
    ;

attributeUsage
    : directionPrefix? ATTRIBUTE usageName? featureRelationships? valueInit? (SEMICOLON | usageBody)
    ;

portUsage
    : directionPrefix? PORT usageName? featureRelationships? usageBody?
    ;

itemUsage
    : directionPrefix? ITEM usageName? featureRelationships? usageBody?
    ;

refUsage
    : directionPrefix? REF usageName featureRelationships? valueInit? (SEMICOLON | usageBody)
    ;

connectionUsage
    : CONNECTION usageName? connectionEndpoints? usageBody?
    | CONNECT connectionEndpoints SEMICOLON?
    ;

flowConnectionUsage
    : FLOW usageName? flowEndpoints? usageBody?
    | FLOW flowEndpoints SEMICOLON?
    ;

successionUsage
    : FIRST expression (THEN expression)? SEMICOLON?
    | THEN expression SEMICOLON?
    | SUCCESSION usageName? successionEndpoints? SEMICOLON?
    ;

transitionUsage
    : TRANSITION usageName? transitionRelationships? transitionBody
    ;

satisfyRequirement
    : ASSERT? NOT? SATISFY qualifiedName (BY expression)? SEMICOLON?
    ;

allocateUsage
    : ALLOCATION usageName? featureRelationships? usageBody?
    | ALLOCATE expression TO expression SEMICOLON?
    ;

performUsage
    : PERFORM expression (BY expression)? SEMICOLON?
    ;

exhibitUsage
    : EXHIBIT expression (BY expression)? SEMICOLON?
    ;

includeUsage
    : INCLUDE expression SEMICOLON?
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

// ============================================================================
// NAMES AND DECLARATIONS
// ============================================================================

declarationName
    : name (shortName)?
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
    : (REDEFINES_OP | REDEFINES) qualifiedName (COMMA qualifiedName)*
    ;

subsetsClause
    : SUBSETS qualifiedName (COMMA qualifiedName)*
    ;

referencesClause
    : (REFERENCE_SUBSETTING | REFERENCES) qualifiedName (COMMA qualifiedName)*
    ;

conjugatesClause
    : (TILDE | CONJUGATES) qualifiedName
    ;

typingClause
    : COLON qualifiedName (COMMA qualifiedName)*
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
    : CONNECT? expression TO expression
    ;

flowEndpoints
    : FROM expression TO expression
    | OF expression FROM expression TO expression
    ;

successionEndpoints
    : FIRST expression THEN expression
    ;

transitionRelationships
    : FIRST expression
    ;

// ============================================================================
// BODIES
// ============================================================================

definitionBody
    : LBRACE definitionBodyElement* RBRACE
    ;

definitionBodyElement
    : namespaceBodyElement
    | returnFeature
    | inParameter
    | outParameter
    | inoutParameter
    ;

returnFeature
    : RETURN usageName? featureRelationships? (SEMICOLON | usageBody)
    ;

inParameter
    : IN usageName? featureRelationships? (SEMICOLON | usageBody)
    ;

outParameter
    : OUT usageName? featureRelationships? (SEMICOLON | usageBody)
    ;

inoutParameter
    : INOUT usageName? featureRelationships? (SEMICOLON | usageBody)
    ;

usageBody
    : LBRACE usageBodyElement* RBRACE
    | SEMICOLON
    ;

usageBodyElement
    : namespaceBodyElement
    | statement
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
    : LBRACE enumMember* RBRACE
    ;

enumMember
    : visibility? ENUM name (EQUALS expression)? SEMICOLON?
    ;

transitionBody
    : acceptClause? guardClause? effectClause? THEN expression SEMICOLON?
    ;

// ============================================================================
// STATE ELEMENTS
// ============================================================================

entryAction
    : ENTRY expression? SEMICOLON
    ;

doAction
    : DO ACTION? expression? SEMICOLON
    ;

exitAction
    : EXIT expression? SEMICOLON
    ;

acceptClause
    : ACCEPT name? (COLON qualifiedName)? (VIA expression)?
    ;

guardClause
    : IF expression
    ;

effectClause
    : DO (SEND expression TO expression | expression)
    ;

// ============================================================================
// REQUIREMENT ELEMENTS
// ============================================================================

subjectDeclaration
    : SUBJECT usageName? featureRelationships? SEMICOLON
    ;

framedConcern
    : FRAME CONCERN qualifiedName SEMICOLON
    ;

assumeConstraint
    : ASSUME CONSTRAINT usageName? featureRelationships? (expression | usageBody)? SEMICOLON?
    ;

requireConstraint
    : REQUIRE CONSTRAINT? usageName? featureRelationships? expression? SEMICOLON?
    ;

objectiveRequirement
    : OBJECTIVE usageName? featureRelationships? usageBody?
    ;

// ============================================================================
// VIEW ELEMENTS
// ============================================================================

exposeStatement
    : EXPOSE qualifiedNameWithWildcard (filterClause)* SEMICOLON
    ;

renderStatement
    : RENDER (AS qualifiedName | ID)? SEMICOLON
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
    | assignmentStatement
    | sendStatement
    | flowStatement
    ;

flowStatement
    : FIRST expression SEMICOLON
    | THEN expression SEMICOLON
    ;

expressionStatement
    : expression SEMICOLON
    ;

ifStatement
    : IF expression LBRACE statement* RBRACE (ELSE LBRACE statement* RBRACE)?
    ;

whileStatement
    : WHILE expression LBRACE statement* RBRACE
    ;

loopStatement
    : LOOP expression? LBRACE statement* RBRACE (UNTIL expression)?
    ;

assignmentStatement
    : qualifiedName (EQUALS | COLON_EQUALS | PLUS_EQUALS) expression SEMICOLON
    ;

sendStatement
    : SEND expression (VIA expression)? TO expression SEMICOLON
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
    : relationalExpression ((HASTYPE | ISTYPE | AT_SIGN) qualifiedName)?
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
    | LPAREN expression RPAREN                            // Parenthesized
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
    : COMMENT_KW (ABOUT qualifiedName)? (LOCALE STRING)? STRING? SEMICOLON?
    ;

documentation
    : DOC (LOCALE STRING)? STRING SEMICOLON?
    | REP (LANGUAGE STRING)? STRING SEMICOLON?
    ;
