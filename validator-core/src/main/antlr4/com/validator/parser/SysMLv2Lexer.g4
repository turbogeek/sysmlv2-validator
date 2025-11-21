lexer grammar SysMLv2Lexer;

// Keywords - Core Structure
PACKAGE: 'package';
IMPORT: 'import';
PUBLIC: 'public';
PRIVATE: 'private';
PROTECTED: 'protected';

// Keywords - Definitions
PART_DEF: 'part' WS+ 'def';
PART: 'part';
ACTION_DEF: 'action' WS+ 'def';
ACTION: 'action';
STATE_DEF: 'state' WS+ 'def';
STATE: 'state';
REQUIREMENT_DEF: 'requirement' WS+ 'def';
REQUIREMENT: 'requirement';
USE_CASE_DEF: 'use' WS+ 'case' WS+ 'def';
USE_CASE: 'use' WS+ 'case';
VIEW_DEF: 'view' WS+ 'def';
VIEW: 'view';
VIEWPOINT_DEF: 'viewpoint' WS+ 'def';
VIEWPOINT: 'viewpoint';
CONSTRAINT_DEF: 'constraint' WS+ 'def';
CONSTRAINT: 'constraint';
ATTRIBUTE_DEF: 'attribute' WS+ 'def';
ATTRIBUTE: 'attribute';
ENUM_DEF: 'enum' WS+ 'def';
ENUM: 'enum';
CONNECTION_DEF: 'connection' WS+ 'def';
CONNECTION: 'connection';
INTERFACE_DEF: 'interface' WS+ 'def';
INTERFACE: 'interface';
ALLOCATION_DEF: 'allocation' WS+ 'def';
ALLOCATION: 'allocation';
PORT_DEF: 'port' WS+ 'def';
PORT: 'port';
ITEM_DEF: 'item' WS+ 'def';
ITEM: 'item';

// Keywords - Flow Control
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

// Keywords - Usage
PERFORM: 'perform';
EXHIBIT: 'exhibit';
SATISFY: 'satisfy';
ALLOCATE: 'allocate';
CONNECT: 'connect';
BIND: 'bind';
FLOW: 'flow';
MESSAGE: 'message';

// Keywords - Relationships
SPECIALIZES: 'specializes';
REDEFINES: 'redefines';
SUBSETS: 'subsets';
REFERENCES: 'references';
CHAINS: 'chains';
INVERSES: 'inverses';
CONJUGATES: 'conjugates';

// Keywords - Modifiers
ABSTRACT: 'abstract';
VARIATION: 'variation';
READONLY: 'readonly';
DERIVED: 'derived';
END: 'end';
ORDERED: 'ordered';
NONUNIQUE: 'nonunique';
PARALLEL: 'parallel';

// Keywords - Parameters/Features
IN: 'in';
OUT: 'out';
INOUT: 'inout';
DEFAULT: 'default';
REF: 'ref';
VALUE: 'value';

// Keywords - Documentation
DOC: 'doc';
COMMENT: 'comment';
METADATA: 'metadata';

// Keywords - Calculations
CALC: 'calc';
ASSERT: 'assert';
ASSUME: 'assume';
REQUIRE: 'require';

// Keywords - Views
EXPOSE: 'expose';
RENDER: 'render';
AS: 'as';
AS_DEFAULT: 'asDefault';

// Keywords - Others
ABOUT: 'about';
FROM: 'from';
TO: 'to';
AT: 'at';
ALL: 'all';
ANY: 'any';
SEQUENCE: 'sequence';
ACCEPT: 'accept';
VERIFY: 'verify';
VIA: 'via';
SEND: 'send';
NEW: 'new';
ENTRY: 'entry';
EXIT: 'exit';
DO: 'do';
NOT: 'not';
BY: 'by';
SUBJECT: 'subject';

// Operators and Symbols
COLON: ':';
SEMICOLON: ';';
COMMA: ',';
DOT: '.';
DOTDOT: '..';
DOUBLE_COLON: '::';
DOUBLE_STAR: '**';
TRIPLE_COLON: ':::';
ARROW: '->';  // Collection pipeline operator (must be before MINUS and GT)

// Relationship operators
SPECIALIZES_OP: ':>';
REDEFINES_OP: ':>>';
CONJUGATE_OP: '~';
TYPED_BY: ':';

// Assignment
EQUALS: '=';
COLON_EQUALS: ':=';

// Brackets
LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
LBRACK: '[';
RBRACK: ']';

// Comparison
LT: '<';
GT: '>';
LE: '<=';
GE: '>=';
EQ: '==';
NE: '!=';

// Arithmetic
PLUS: '+';
MINUS: '-';
STAR: '*';
SLASH: '/';
PERCENT: '%';

// Logical
AND: '&&' | 'and';
OR: '||' | 'or';
BANG: '!';

// Literals
TRUE: 'true';
FALSE: 'false';
NULL: 'null';

// Identifiers and Literals
ID: LETTER (LETTER | DIGIT | '_')*;
QUOTED_ID: '\'' (~['\r\n])* '\'';
UNRESTRICTED_ID: '<' (~[>\r\n])* '>';

INTEGER: DIGIT+;
REAL: DIGIT+ '.' DIGIT+ ([eE] [+-]? DIGIT+)?;
STRING: '"' (~["\r\n\\] | '\\' .)* '"';

// Comments
BLOCK_COMMENT: '/*' .*? '*/' -> skip;
LINE_COMMENT: '//' ~[\r\n]* -> skip;

// Whitespace
WS: [ \t\r\n]+ -> skip;

// Fragments
fragment LETTER: [a-zA-Z];
fragment DIGIT: [0-9];
