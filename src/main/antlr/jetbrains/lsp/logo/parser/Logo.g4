grammar Logo;

options { caseInsensitive = true; }

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

repeatCmd
    : REPEAT expr LBRACK statement* RBRACK
    ;

ifCmd
    : IF expr LBRACK statement* RBRACK
    ;

ifelseCmd
    : IFELSE expr LBRACK statement* RBRACK LBRACK statement* RBRACK
    ;

makeCmd
    : MAKE QUOTED_STRING expr
    ;

outputCmd
    : OUTPUT expr
    ;

stopCmd
    : STOP
    ;

printCmd
    : PRINT expr
    ;

builtinCmd
    : FORWARD expr
    | BACK expr
    | LEFT expr
    | RIGHT expr
    | SETX expr
    | SETY expr
    | SETXY expr expr
    | SETPC expr
    | SETPENCOLOR expr
    | ARC expr expr
    | PENUP
    | PENDOWN
    | HOME
    | CLEARSCREEN
    | HIDETURTLE
    | SHOWTURTLE
    ;

procedureCall
    : IDENT expr*
    ;

expr
    : LPAREN expr RPAREN                                    # parenExpr
    | op=MINUS expr                                         # negExpr
    | left=expr op=(STAR | SLASH) right=expr               # mulExpr
    | left=expr op=(PLUS | MINUS) right=expr               # addExpr
    | left=expr op=(EQUAL | NOTEQUAL | LT | GT | LE | GE) right=expr # cmpExpr
    | NUMBER                                                # numberExpr
    | COLON IDENT                                           # varExpr
    | QUOTED_STRING                                         # stringExpr
    | IDENT LPAREN expr* RPAREN                             # callExprParen
    | IDENT                                                 # callExprNoArg
    ;

// Lexer Rules

// Keywords (must appear before IDENT)
TO          : 'TO' ;
END         : 'END' ;
REPEAT      : 'REPEAT' ;
IF          : 'IF' ;
IFELSE      : 'IFELSE' ;
MAKE        : 'MAKE' ;
OUTPUT      : 'OUTPUT' | 'OP' ;
STOP        : 'STOP' ;
PRINT       : 'PRINT' | 'PR' ;

// Built-in turtle / pen commands
FORWARD     : 'FORWARD'     | 'FD' ;
BACK        : 'BACK'        | 'BK' ;
LEFT        : 'LEFT'        | 'LT' ;
RIGHT       : 'RIGHT'       | 'RT' ;
PENUP       : 'PENUP'       | 'PU' ;
PENDOWN     : 'PENDOWN'     | 'PD' ;
HOME        : 'HOME' ;
CLEARSCREEN : 'CLEARSCREEN' | 'CS' ;
SETX        : 'SETX' ;
SETY        : 'SETY' ;
SETXY       : 'SETXY' ;
SETPC       : 'SETPC' ;
SETPENCOLOR : 'SETPENCOLOR' ;
HIDETURTLE  : 'HIDETURTLE'  | 'HT' ;
SHOWTURTLE  : 'SHOWTURTLE'  | 'ST' ;
ARC         : 'ARC' ;

// Operators & delimiters
LBRACK  : '[' ;
RBRACK  : ']' ;
LPAREN  : '(' ;
RPAREN  : ')' ;
PLUS    : '+' ;
MINUS   : '-' ;
STAR    : '*' ;
SLASH   : '/' ;
EQUAL   : '=' ;
NOTEQUAL: '<>' ;
LT      : '<' ;
GT      : '>' ;
LE      : '<=' ;
GE      : '>=' ;
COLON   : ':' ;

// Literals
NUMBER        : [0-9]+ ('.' [0-9]+)? ;
QUOTED_STRING : '"' ~[ \t\r\n]* ;   // Logo "word — no closing quote
IDENT         : [A-Z_] [A-Z0-9_]* ;

// Whitespace & structure
EOL     : [\r\n]+ ;
WS      : [ \t]+ -> skip ;
COMMENT : ';' ~[\r\n]* -> channel(HIDDEN) ;
