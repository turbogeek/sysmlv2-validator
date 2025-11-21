parser grammar SysMLv2Parser;

options {
    tokenVocab = SysMLv2Lexer;
}

// Top-level rule
compilationUnit
    : (packageDeclaration | importDeclaration | element)* EOF
    ;

// Package
packageDeclaration
    : PACKAGE qualifiedName (SEMICOLON | packageBody)
    ;

packageBody
    : LBRACE (importStatement | element)* RBRACE
    ;

// Import
importDeclaration
    : visibility? IMPORT qualifiedName (DOUBLE_COLON STAR)? SEMICOLON
    ;

importStatement
    : visibility? IMPORT qualifiedName (DOUBLE_COLON STAR)? SEMICOLON
    ;

visibility
    : PUBLIC
    | PRIVATE
    | PROTECTED
    ;

// Elements (top-level definitions and usages)
element
    : partDefinition
    | partUsage
    | actionDefinition
    | actionUsage
    | stateDefinition
    | stateUsage
    | requirementDefinition
    | requirementUsage
    | viewDefinition
    | viewUsage
    | constraintDefinition
    | constraintUsage
    | attributeDefinition
    | attributeUsage
    | portDefinition
    | connectionDefinition
    | connectionUsage
    | transitionUsage
    | successionUsage
    | satisfyRequirement
    | comment
    ;

// Part Definition
partDefinition
    : visibility? PART_DEF name (specialization)? (featureBody)?
    ;

// Part Usage
partUsage
    : PART name? (COLON qualifiedName)? (multiplicity)? (specialization)? (featureBody)? SEMICOLON?
    ;

// Action Definition
actionDefinition
    : visibility? ACTION_DEF name (specialization)? (featureBody)?
    ;

// Action Usage
actionUsage
    : ACTION name? (COLON qualifiedName)? (specialization)? (featureBody)? SEMICOLON?
    ;

// State Definition
stateDefinition
    : visibility? STATE_DEF name (specialization)? (stateBody)?
    ;

// State Usage
stateUsage
    : STATE name? (modifiers)? (COLON qualifiedName)? (specialization)? (stateBody)? SEMICOLON?
    ;

// Requirement Definition
requirementDefinition
    : visibility? REQUIREMENT_DEF name (specialization)? (requirementBody)?
    ;

// Requirement Usage
requirementUsage
    : REQUIREMENT name? (COLON qualifiedName)? (specialization)? (requirementBody)? SEMICOLON?
    ;

// View Definition
viewDefinition
    : visibility? VIEW_DEF name (specialization)? (viewBody)?
    ;

// View Usage
viewUsage
    : VIEW name? (COLON qualifiedName)? (viewBody)? SEMICOLON?
    ;

// Constraint Definition
constraintDefinition
    : visibility? CONSTRAINT_DEF name (specialization)? (featureBody)?
    ;

// Constraint Usage
constraintUsage
    : CONSTRAINT name? (COLON qualifiedName)? (specialization)? (featureBody)? SEMICOLON?
    ;

// Attribute Definition
attributeDefinition
    : visibility? ATTRIBUTE_DEF name (specialization)? (featureBody)?
    ;

// Attribute Usage
attributeUsage
    : ATTRIBUTE name? (COLON qualifiedName)? (multiplicity)? (specialization)? (featureValueInit)? SEMICOLON?
    ;

// Port Definition
portDefinition
    : visibility? PORT_DEF name (specialization)? (featureBody)?
    ;

// Connection Definition
connectionDefinition
    : visibility? CONNECTION_DEF name (specialization)? (featureBody)?
    ;

// Connection Usage
connectionUsage
    : CONNECT expression TO expression SEMICOLON?
    ;

// Transition
transitionUsage
    : TRANSITION name? transitionBody SEMICOLON?
    ;

// Succession (flow control)
successionUsage
    : FIRST expression THEN expression SEMICOLON?
    ;

// Satisfy Requirement
satisfyRequirement
    : (ASSERT)? (NOT)? SATISFY expression BY expression SEMICOLON?
    ;

// Bodies
featureBody
    : LBRACE featureBodyElement* RBRACE
    ;

featureBodyElement
    : element
    | statement
    ;

stateBody
    : LBRACE stateElement* RBRACE
    ;

stateElement
    : element
    | entryAction
    | exitAction
    | doAction
    | transitionUsage
    | statement
    ;

requirementBody
    : LBRACE requirementElement* RBRACE
    ;

requirementElement
    : element
    | assumeConstraint
    | requireConstraint
    | subjectDeclaration
    | docString
    ;

viewBody
    : LBRACE viewElement* RBRACE
    ;

viewElement
    : exposeStatement
    | renderStatement
    ;

transitionBody
    : FIRST expression (acceptClause)? (ifClause)? (doClause)? THEN expression
    ;

// View statements
exposeStatement
    : EXPOSE qualifiedName (DOUBLE_COLON DOUBLE_STAR | DOUBLE_COLON STAR)? SEMICOLON
    ;

renderStatement
    : RENDER (AS_DEFAULT | AS name) SEMICOLON
    ;

// State actions
entryAction
    : ENTRY SEMICOLON (THEN expression SEMICOLON)?
    ;

exitAction
    : EXIT expression SEMICOLON
    ;

doAction
    : DO (ACTION)? expression SEMICOLON
    ;

acceptClause
    : ACCEPT name (COLON qualifiedName)? (VIA expression)?
    ;

ifClause
    : IF expression
    ;

doClause
    : DO (SEND expression TO expression | expression)
    ;

// Requirement elements
assumeConstraint
    : ASSUME CONSTRAINT name (COLON qualifiedName)? SEMICOLON
    ;

requireConstraint
    : REQUIRE (CONSTRAINT name? (COLON qualifiedName)?)? expression SEMICOLON
    ;

subjectDeclaration
    : SUBJECT name (COLON qualifiedName)? SEMICOLON
    ;

// Specialization
specialization
    : SPECIALIZES_OP qualifiedName (COMMA qualifiedName)*
    | REDEFINES_OP qualifiedName (COMMA qualifiedName)*
    | COLON qualifiedName (COMMA qualifiedName)*
    ;

// Modifiers
modifiers
    : PARALLEL
    | ABSTRACT
    | VARIATION
    | READONLY
    | ORDERED
    ;

// Multiplicity
multiplicity
    : LBRACK multiplicityRange RBRACK
    ;

multiplicityRange
    : expression (DOTDOT expression)?
    | STAR
    ;

// Feature value
featureValueInit
    : (EQUALS | COLON_EQUALS | DEFAULT) expression
    ;

// Expressions
expression
    : primary
    | qualifiedName
    | expression DOT qualifiedName                                      // Member access
    | expression ARROW qualifiedName LPAREN argumentList? RPAREN        // Arrow operator (collection operations)
    | expression LPAREN argumentList? RPAREN                            // Function call
    | expression LBRACK expression RBRACK                               // Index
    | MINUS expression                                                  // Unary minus
    | BANG expression                                                   // Logical not
    | NEW qualifiedName LPAREN argumentList? RPAREN                     // New instance
    | expression (STAR | SLASH | PERCENT) expression                    // Multiplicative
    | expression (PLUS | MINUS) expression                              // Additive
    | expression (LT | GT | LE | GE) expression                         // Relational
    | expression (EQ | NE) expression                                   // Equality
    | expression AND expression                                         // Logical and
    | expression OR expression                                          // Logical or
    | LPAREN expression RPAREN                                          // Parenthesized
    ;

argumentList
    : expression (COMMA expression)*
    ;

primary
    : literal
    | name
    ;

literal
    : INTEGER
    | REAL
    | STRING
    | TRUE
    | FALSE
    | NULL
    ;

// Documentation
docString
    : DOC STRING
    | DOC BLOCK_COMMENT
    ;

comment
    : COMMENT (ABOUT qualifiedName)? STRING SEMICOLON?
    ;

// Names
name
    : ID
    | QUOTED_ID
    | UNRESTRICTED_ID
    ;

qualifiedName
    : name (DOUBLE_COLON name)*
    ;

// Statements - for simple action invocations and flow control
statement
    : expressionStatement
    | successionStatement
    ;

expressionStatement
    : expression SEMICOLON
    ;

successionStatement
    : FIRST expression SEMICOLON
    | THEN expression SEMICOLON
    ;
