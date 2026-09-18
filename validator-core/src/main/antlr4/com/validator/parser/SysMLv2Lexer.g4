lexer grammar SysMLv2Lexer;

@members {
    /**
     * True when the character after the current token cannot continue an identifier (ID is
     * [a-zA-Z_][a-zA-Z0-9_]*). Compound keywords such as 'attribute def' end with this check: without it ANTLR's
     * longest match took 'attribute def' out of 'attribute defaultForm' and left 'aultForm', which parsed silently
     * as a definition with a clipped name.
     */
    private boolean atWordEnd() {
        int c = _input.LA(1);
        return !((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_');
    }
}

// ============================================================================
// COMPOUND KEYWORDS (must come before simple keywords due to longest match)
// ============================================================================

// Definition compound keywords
PART_DEF: 'part' WS+ 'def' {atWordEnd()}?;
ACTION_DEF: 'action' WS+ 'def' {atWordEnd()}?;
STATE_DEF: 'state' WS+ 'def' {atWordEnd()}?;
REQUIREMENT_DEF: 'requirement' WS+ 'def' {atWordEnd()}?;
USE_CASE_DEF: 'use' WS+ 'case' WS+ 'def' {atWordEnd()}?;
VIEW_DEF: 'view' WS+ 'def' {atWordEnd()}?;
VIEWPOINT_DEF: 'viewpoint' WS+ 'def' {atWordEnd()}?;
CONSTRAINT_DEF: 'constraint' WS+ 'def' {atWordEnd()}?;
ATTRIBUTE_DEF: 'attribute' WS+ 'def' {atWordEnd()}?;
ENUM_DEF: 'enum' WS+ 'def' {atWordEnd()}?;
CONNECTION_DEF: 'connection' WS+ 'def' {atWordEnd()}?;
INTERFACE_DEF: 'interface' WS+ 'def' {atWordEnd()}?;
ALLOCATION_DEF: 'allocation' WS+ 'def' {atWordEnd()}?;
PORT_DEF: 'port' WS+ 'def' {atWordEnd()}?;
ITEM_DEF: 'item' WS+ 'def' {atWordEnd()}?;
CALC_DEF: 'calc' WS+ 'def' {atWordEnd()}?;
ANALYSIS_DEF: 'analysis' WS+ 'def' {atWordEnd()}?;
CASE_DEF: 'case' WS+ 'def' {atWordEnd()}?;
VERIFICATION_DEF: 'verification' WS+ 'def' {atWordEnd()}?;
CONCERN_DEF: 'concern' WS+ 'def' {atWordEnd()}?;
RENDERING_DEF: 'rendering' WS+ 'def' {atWordEnd()}?;
OCCURRENCE_DEF: 'occurrence' WS+ 'def' {atWordEnd()}?;
FLOW_DEF: 'flow' WS+ 'def' {atWordEnd()}?;
METADATA_DEF: 'metadata' WS+ 'def' {atWordEnd()}?;
INDIVIDUAL_DEF: 'individual' WS+ 'def' {atWordEnd()}?;

// KerML compound keywords
ASSOC_STRUCT: 'assoc' WS+ 'struct' {atWordEnd()}?;

// Other compound keywords
USE_CASE: 'use' WS+ 'case' {atWordEnd()}?;
DEFINED_BY: 'defined' WS+ 'by' {atWordEnd()}?;
TYPED_BY: 'typed' WS+ 'by' {atWordEnd()}?;

// ============================================================================
// KEYWORDS - Core Structure
// ============================================================================
PACKAGE: 'package';
IMPORT: 'import';
PUBLIC: 'public';
PRIVATE: 'private';
PROTECTED: 'protected';
NAMESPACE: 'namespace';

// ============================================================================
// KEYWORDS - KerML Types (foundation layer)
// ============================================================================
DATATYPE: 'datatype';
CLASS: 'class';
STRUCT: 'struct';
ASSOC: 'assoc';
BEHAVIOR: 'behavior';
STEP: 'step';
FUNCTION: 'function';
PREDICATE: 'predicate';
INTERACTION: 'interaction';
METACLASS: 'metaclass';
CLASSIFIER: 'classifier';
TYPE: 'type';
FEATURE: 'feature';
MULTIPLICITY: 'multiplicity';
CONNECTOR: 'connector';
BINDING: 'binding';

// ============================================================================
// KEYWORDS - SysML Definitions and Usages
// ============================================================================
PART: 'part';
ACTION: 'action';
STATE: 'state';
REQUIREMENT: 'requirement';
VIEW: 'view';
VIEWPOINT: 'viewpoint';
CONSTRAINT: 'constraint';
ATTRIBUTE: 'attribute';
ENUM: 'enum';
CONNECTION: 'connection';
INTERFACE: 'interface';
ALLOCATION: 'allocation';
PORT: 'port';
ITEM: 'item';
CALC: 'calc';
ANALYSIS: 'analysis';
CASE: 'case';
VERIFICATION: 'verification';
CONCERN: 'concern';
RENDERING: 'rendering';
OCCURRENCE: 'occurrence';

// ============================================================================
// KEYWORDS - Flow Control
// ============================================================================
FIRST: 'first';
THEN: 'then';
START: 'start';
DONE: 'done';
SUCCESSION: 'succession';
TRANSITION: 'transition';
DECIDE: 'decide';
MERGE: 'merge';
FORK: 'fork';
JOIN: 'join';
IF: 'if';
ELSE: 'else';
WHILE: 'while';
LOOP: 'loop';
UNTIL: 'until';
AFTER: 'after';
WHEN: 'when';
TERMINATE: 'terminate';

// ============================================================================
// KEYWORDS - Relationships and Usage
// ============================================================================
PERFORM: 'perform';
EXHIBIT: 'exhibit';
SATISFY: 'satisfy';
ALLOCATE: 'allocate';
CONNECT: 'connect';
BIND: 'bind';
FLOW: 'flow';
MESSAGE: 'message';
INCLUDE: 'include';
ACTOR: 'actor';
STAKEHOLDER: 'stakeholder';

// ============================================================================
// KEYWORDS - Relationship Operators
// ============================================================================
SPECIALIZES: 'specializes';
REDEFINES: 'redefines';
SUBSETS: 'subsets';
REFERENCES: 'references';
CHAINS: 'chains';
INVERSES: 'inverses';
CONJUGATES: 'conjugates';
UNIONS: 'unions';
INTERSECTS: 'intersects';
DIFFERENCES: 'differences';
DISJOINT: 'disjoint';
TYPING: 'typing';
FEATURING: 'featuring';
DEPENDENCY: 'dependency';

// ============================================================================
// KEYWORDS - Modifiers
// ============================================================================
ABSTRACT: 'abstract';
VARIATION: 'variation';
VARIANT: 'variant';
READONLY: 'readonly';
DERIVED: 'derived';
END: 'end';
ORDERED: 'ordered';
NONUNIQUE: 'nonunique';
PARALLEL: 'parallel';
COMPOSITE: 'composite';
PORTION: 'portion';
ISTYPE: 'istype';
HASTYPE: 'hastype';

// ============================================================================
// KEYWORDS - Parameters/Features Direction and Modifiers
// ============================================================================
IN: 'in';
OUT: 'out';
INOUT: 'inout';
DEFAULT: 'default';
REF: 'ref';
VALUE: 'value';
CONSTANT: 'constant';

// ============================================================================
// KEYWORDS - Documentation and Metadata
// ============================================================================
DOC: 'doc';
COMMENT_KW: 'comment';
METADATA: 'metadata';
REP: 'rep';
LANGUAGE: 'language';
LOCALE: 'locale';

// ============================================================================
// KEYWORDS - Calculations and Expressions
// ============================================================================
ASSERT: 'assert';
ASSUME: 'assume';
REQUIRE: 'require';
OBJECTIVE: 'objective';
FRAME: 'frame';

// ============================================================================
// KEYWORDS - Views
// ============================================================================
EXPOSE: 'expose';
RENDER: 'render';
AS: 'as';

// ============================================================================
// KEYWORDS - Other
// ============================================================================
ABOUT: 'about';
FROM: 'from';
TO: 'to';
OF: 'of';
AT: 'at';
VIA: 'via';
ALL: 'all';
ANY: 'any';
SEQUENCE: 'sequence';
ACCEPT: 'accept';
VERIFY: 'verify';
SEND: 'send';
NEW: 'new';
ENTRY: 'entry';
EXIT: 'exit';
DO: 'do';
NOT: 'not';
BY: 'by';
SUBJECT: 'subject';
FOR: 'for';
ALIAS: 'alias';
ASSIGN: 'assign';
EVENT: 'event';
THIS: 'this';

// ============================================================================
// KEYWORDS - KerML Library/Expressions
// ============================================================================
LIBRARY: 'library';
STANDARD: 'standard';
FILTER: 'filter';
INV: 'inv';
XOR: 'xor';
IMPLIES: 'implies';
META: 'meta';
INDIVIDUAL: 'individual';
RETURN: 'return';
SNAPSHOT: 'snapshot';
TIMESLICE: 'timeslice';

// ============================================================================
// OPERATORS - Multi-character (order matters - longest first)
// ============================================================================

// Relationship operators
REFERENCE_SUBSETTING: '::>';
REDEFINES_OP: ':>>';
SPECIALIZES_OP: ':>';
CROSSES_OP: '=>';
// FEATURE_CHAIN is same as DOT - use DOT instead and handle in parser

// Namespace operators
TRIPLE_COLON: ':::';
DOUBLE_COLON: '::';
DOUBLE_STAR: '**';

// Flow and arrows
ARROW: '->';
// Note: THICK_ARROW is same as CROSSES_OP, removed duplicate

// Multiplicity
DOTDOT: '..';

// Comparison operators
LE: '<=';
GE: '>=';
EQ: '==';
NE: '!=';
SAME: '===';
NOT_SAME: '!==';

// Assignment operators
COLON_EQUALS: ':=';
PLUS_EQUALS: '+=';

// Logical operators
AND: '&&' | '&' | 'and';
OR: '||' | '|' | 'or';
// Note: IMPLIES_OP removed - 'implies' is already the IMPLIES keyword
NULL_COALESCING: '??';

// ============================================================================
// COMMENTS - Skip these tokens (MUST come before SLASH to match properly)
// ============================================================================
BLOCK_COMMENT: '/*' .*? '*/' -> skip;
// Note: LINE_COMMENT sent to channel 1 for warning detection (not persisted to model)
LINE_COMMENT: '//' ~[\r\n]* -> channel(1);

// ============================================================================
// OPERATORS - Single character
// ============================================================================
COLON: ':';
SEMICOLON: ';';
COMMA: ',';
DOT: '.';
EQUALS: '=';
LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
LBRACK: '[';
RBRACK: ']';
LT: '<';
GT: '>';
PLUS: '+';
MINUS: '-';
STAR: '*';
SLASH: '/';
PERCENT: '%';
BANG: '!';
TILDE: '~';
QUESTION: '?';
AT_SIGN: '@';
HASH: '#';
CARET: '^';

// ============================================================================
// LITERALS
// ============================================================================
TRUE: 'true';
FALSE: 'false';
NULL: 'null';

// ============================================================================
// IDENTIFIERS
// ============================================================================
ID: LETTER (LETTER | DIGIT | '_')*;
QUOTED_ID: '\'' (~['\r\n\\] | '\\' .)* '\'';
UNRESTRICTED_NAME: '<' (~[>\r\n])* '>';

// ============================================================================
// NUMERIC LITERALS
// ============================================================================
HEX_INTEGER: '0' [xX] HEX_DIGIT+;
BINARY_INTEGER: '0' [bB] [01]+;
INTEGER: DIGIT+;
// Note: REAL must have at least one digit after decimal to avoid matching "0." in "0..*"
REAL: DIGIT+ '.' DIGIT+ EXPONENT?
    | '.' DIGIT+ EXPONENT?
    | DIGIT+ EXPONENT;
DECIMAL: DIGIT+ '.' DIGIT+;

// ============================================================================
// STRING LITERALS
// ============================================================================
STRING: '"' (~["\r\n\\] | ESCAPE_SEQUENCE)* '"';

// ============================================================================
// WHITESPACE - Skip
// ============================================================================
WS: [ \t\r\n]+ -> skip;

// ============================================================================
// FRAGMENTS - Building blocks
// ============================================================================
fragment LETTER: [a-zA-Z_];
fragment DIGIT: [0-9];
fragment HEX_DIGIT: [0-9a-fA-F];
fragment EXPONENT: [eE] [+-]? DIGIT+;
fragment ESCAPE_SEQUENCE: '\\' [btnfr"'\\] | UNICODE_ESCAPE;
fragment UNICODE_ESCAPE: '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;
