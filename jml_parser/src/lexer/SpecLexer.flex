package lexer;

import java_cup.runtime.*;
import parser.sym;

%%

%class SpecLexer
%unicode
%cup
%line
%column

%{
    private Symbol sym(int type) {
        return new Symbol(type, yyline, yycolumn);
    }
    private Symbol sym(int type, Object value) {
        return new Symbol(type, yyline, yycolumn, value);
    }
%}

/* Macros */
LineTerminator  = \r|\n|\r\n
WhiteSpace      = {LineTerminator} | [ \t\f]
Identifier      = [a-zA-Z_][a-zA-Z0-9_]*
IntLiteral      = [0-9]+
DoubleLiteral   = [0-9]+"."[0-9]+

%%

/* Whitespace and comments — skip */
{WhiteSpace}+               { /* skip */ }
"//" [^\r\n]*               { /* skip line comment */ }
"/*" ~"*/"                  { /* skip block comment */ }

/* Keywords */
"spec"                      { return sym(sym.SPEC); }
"signature"                 { return sym(sym.SIGNATURE); }
"requires"                  { return sym(sym.REQUIRES); }
"ensures"                   { return sym(sym.ENSURES); }
"true"                      { return sym(sym.TRUE); }
"false"                     { return sym(sym.FALSE); }
"null"                      { return sym(sym.NULL); }
"void"                      { return sym(sym.VOID); }
"int"                       { return sym(sym.INT); }
"double"                    { return sym(sym.DOUBLE); }
"boolean"                   { return sym(sym.BOOLEAN); }
"float"                     { return sym(sym.FLOAT); }
"long"                      { return sym(sym.LONG); }
"char"                      { return sym(sym.CHAR); }
"byte"                      { return sym(sym.BYTE); }
"short"                     { return sym(sym.SHORT); }
"String"                    { return sym(sym.STRING_TYPE); }

/* JML Special tokens */
"\\result"                  { return sym(sym.RESULT); }
"\\old"                     { return sym(sym.OLD); }

/* Literals */
{DoubleLiteral}             { return sym(sym.DOUBLE_LIT, Double.parseDouble(yytext())); }
{IntLiteral}                { return sym(sym.INT_LIT,    Integer.parseInt(yytext())); }
\"[^\"]*\"                  { return sym(sym.STRING_LIT, yytext().substring(1, yytext().length()-1)); }

/* Identifier (must come after keywords) */
{Identifier}                { return sym(sym.IDENT, yytext()); }

/* Brackets and punctuation */
"{"                         { return sym(sym.LBRACE); }
"}"                         { return sym(sym.RBRACE); }
"("                         { return sym(sym.LPAREN); }
")"                         { return sym(sym.RPAREN); }
"["                         { return sym(sym.LBRACKET); }
"]"                         { return sym(sym.RBRACKET); }
";"                         { return sym(sym.SEMI); }
":"                         { return sym(sym.COLON); }
","                         { return sym(sym.COMMA); }
"."                         { return sym(sym.DOT); }

/* Operators — longer tokens first to avoid prefix conflicts */
"=="                        { return sym(sym.EQ); }
"!="                        { return sym(sym.NEQ); }
"<="                        { return sym(sym.LEQ); }
">="                        { return sym(sym.GEQ); }
"<"                         { return sym(sym.LT); }
">"                         { return sym(sym.GT); }
"&&"                        { return sym(sym.AND); }
"||"                        { return sym(sym.OR); }
"!"                         { return sym(sym.NOT); }
"+"                         { return sym(sym.PLUS); }
"-"                         { return sym(sym.MINUS); }
"*"                         { return sym(sym.TIMES); }
"/"                         { return sym(sym.DIVIDE); }
"="                         { return sym(sym.ASSIGN); }

/* Fallback */
[^]                         { throw new Error("Illegal character: <" + yytext() + "> at line " + yyline + ", col " + yycolumn); }
