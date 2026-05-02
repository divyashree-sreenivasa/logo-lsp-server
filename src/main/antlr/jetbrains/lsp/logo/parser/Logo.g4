grammar Logo;

// Parser Rules

program
    : statement* EOF
    ;

statement
    : procedureDef
    | commandStatement
    ;

procedureDef
    : TO IDENT paramDecl* EOL
      statement*
      END EOL?
    ;

paramDecl
    : COLON IDENT
    ;

commandStatement
    : repeatCmd
    | whileCmd
    | untilCmd
    | forCmd
    | foreverCmd
    | ifCmd
    | ifelseCmd
    | makeCmd
    | outputCmd
    | stopCmd
    | printCmd
    | builtinCmd
    | procedureCall
    | EOL
    ;

repeatCmd  : REPEAT  expr LBRACK statement* RBRACK ;
whileCmd   : WHILE   LBRACK expr RBRACK LBRACK statement* RBRACK ;
untilCmd   : UNTIL   LBRACK expr RBRACK LBRACK statement* RBRACK ;
forCmd     : FOR     LBRACK IDENT expr expr expr? RBRACK LBRACK statement* RBRACK ;
foreverCmd : FOREVER LBRACK statement* RBRACK ;

ifCmd     : IF     expr LBRACK statement* RBRACK ;
ifelseCmd : IFELSE expr LBRACK statement* RBRACK LBRACK statement* RBRACK ;

makeCmd   : MAKE QUOTED_STRING expr ;
outputCmd : OUTPUT expr ;
stopCmd   : STOP ;
printCmd  : PRINT expr ;

builtinCmd
    : (FORWARD | BACK | LEFT | RIGHT | SETX | SETY | SETHEADING
      | SETPC | SETPENCOLOR | SETPENSIZE | SHOW) expr
    | SETXY expr expr
    | ARC expr expr
    | LOCALMAKE QUOTED_STRING expr
    | (PENUP | PENDOWN | HOME | CLEARSCREEN | HIDETURTLE | SHOWTURTLE)
    ;

procedureCall
    : IDENT expr*
    ;

expr
    : LPAREN expr RPAREN                                              # parenExpr
    | op=MINUS expr                                                   # negExpr
    | left=expr op=(STAR | SLASH) right=expr                         # mulExpr
    | left=expr op=(PLUS | MINUS) right=expr                         # addExpr
    | left=expr op=(EQUAL | NOTEQUAL | LT | GT | LE | GE) right=expr # cmpExpr
    | builtinFunc expr*                                               # builtinFuncExpr
    | NUMBER                                                          # numberExpr
    | COLON IDENT                                                     # varExpr
    | QUOTED_STRING                                                   # stringExpr
    | LBRACK item* RBRACK                                             # listExpr
    | IDENT LPAREN expr* RPAREN                                       # callExprParen
    | IDENT                                                           # callExprNoArg
    ;

item
    : NUMBER
    | QUOTED_STRING
    | IDENT
    | LBRACK item* RBRACK
    ;

// Value-returning built-in functions used in expression position
builtinFunc
    : SIN | COS | SQRT | ABS | RANDOM | INT | ROUND
    | FIRST | LAST | COUNT | SENTENCE
    | AND | OR | NOT
    ;

// Lexer Rules
// Keywords must appear before IDENT

// Control flow
TO       : 'TO' ;
END      : 'END' ;
REPEAT   : 'REPEAT' ;
WHILE    : 'WHILE' ;
UNTIL    : 'UNTIL' ;
FOR      : 'FOR' ;
FOREVER  : 'FOREVER' ;
IF       : 'IF' ;
IFELSE   : 'IFELSE' ;

// Variables
MAKE      : 'MAKE' ;
LOCAL     : 'LOCAL' ;
LOCALMAKE : 'LOCALMAKE' ;
OUTPUT    : 'OUTPUT' | 'OP' ;
STOP      : 'STOP' ;

// I/O
PRINT : 'PRINT' | 'PR' ;
SHOW  : 'SHOW' ;

// Turtle motion
FORWARD    : 'FORWARD'    | 'FD' ;
BACK       : 'BACK'       | 'BK' ;
LEFT       : 'LEFT'       | 'LT' ;
RIGHT      : 'RIGHT'      | 'RT' ;
HOME       : 'HOME' ;
SETX       : 'SETX' ;
SETY       : 'SETY' ;
SETXY      : 'SETXY' ;
SETHEADING : 'SETHEADING' | 'SETH' ;
ARC        : 'ARC' ;

// Pen / screen
PENUP       : 'PENUP'       | 'PU' ;
PENDOWN     : 'PENDOWN'     | 'PD' ;
SETPC       : 'SETPC' ;
SETPENCOLOR : 'SETPENCOLOR' ;
SETPENSIZE  : 'SETPENSIZE'  | 'SETWIDTH' ;
CLEARSCREEN : 'CLEARSCREEN' | 'CS' ;
HIDETURTLE  : 'HIDETURTLE'  | 'HT' ;
SHOWTURTLE  : 'SHOWTURTLE'  | 'ST' ;

// Math (most-used in turtle programs)
SIN    : 'SIN' ;
COS    : 'COS' ;
SQRT   : 'SQRT' ;
ABS    : 'ABS' ;
INT    : 'INT' ;
ROUND  : 'ROUND' ;
RANDOM : 'RANDOM' ;

// List / word (most-used)
FIRST    : 'FIRST' ;
LAST     : 'LAST' ;
COUNT    : 'COUNT' ;
SENTENCE : 'SENTENCE' | 'SE' ;

// Boolean
AND : 'AND' ;
OR  : 'OR' ;
NOT : 'NOT' ;

// Operators & delimiters
LBRACK   : '[' ;
RBRACK   : ']' ;
LPAREN   : '(' ;
RPAREN   : ')' ;
PLUS     : '+' ;
MINUS    : '-' ;
STAR     : '*' ;
SLASH    : '/' ;
EQUAL    : '=' ;
NOTEQUAL : '<>' ;
LT       : '<' ;
GT       : '>' ;
LE       : '<=' ;
GE       : '>=' ;
COLON    : ':' ;

// Literals
NUMBER        : [0-9]+ ('.' [0-9]+)? ;
QUOTED_STRING : '"' ~[ \t\r\n[\]]* ;  // "word — no closing quote
IDENT         : [A-Z_] [A-Z0-9_.]* ;  // allow . for names like DO.WHILE

// Whitespace & structure
EOL     : [\r\n]+ ;
WS      : [ \t]+ -> skip ;
COMMENT : ';' ~[\r\n]* -> channel(HIDDEN) ;
