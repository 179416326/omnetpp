/*==================================================
 * File: expression.lex
 *
 *  Lexical analyser for OMNeT++ NED-2 expressions.
 *
 *  Author: Andras Varga
 *
 ==================================================*/

/*--------------------------------------------------------------*
  Copyright (C) 1992,2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/


/*
 * NED-2 reserved words:
 *    non-component: import package property
 *    component:     module simple network channel interface
 *    inheritance:   extends like withcppclass
 *    sections:      types parameters gates submodules connections allowunconnected
 *    param types:   double int string bool xml function
 *    gate types:    input output inout
 *    conditional:   if
 *    connections:   --> <-- <--> while ..
 *    expressions:   true false default const sizeof index xmldoc
 */

D  [0-9]
L  [a-zA-Z_]
X  [0-9a-fA-F]
E  [Ee][+-]?{D}+
S  [ \t\v\n\r\f]

%x cplusplusbody
%x stringliteral

/* the following option keeps isatty() out */
%option never-interactive

%{
#include <string.h>
#include "expressionyydefs.h"
#include "cexception.h"
#include "expression.tab.h"

#define yylval expressionyylval
extern YYSTYPE yylval;

// wrap symbols to allow several .lex files coexist
#define comment     expressionComment
#define count       expressionCount
#define extendCount expressionExtendCount

void comment();
void count();
void extendCount();

#define TEXTBUF_LEN 1024
static char textbuf[TEXTBUF_LEN];

// buffer to collect characters during extendCount()
static std::string extendbuf;

#include "util.h"  // opp_strdup()
%}

%%
"//"                     { comment(); }

"double"                 { count(); return DOUBLETYPE; }
"int"                    { count(); return INTTYPE; }
"string"                 { count(); return STRINGTYPE; }
"bool"                   { count(); return BOOLTYPE; }
"xml"                    { count(); return XMLTYPE; }

"true"                   { count(); return TRUE_; }
"false"                  { count(); return FALSE_; }
"this"                   { count(); return THIS_; }
"default"                { count(); return DEFAULT_; }
"const"                  { count(); return CONST_; }
"sizeof"                 { count(); return SIZEOF_; }
"index"                  { count(); return INDEX_; }
"xmldoc"                 { count(); return XMLDOC_; }

{L}({L}|{D})*            { count(); yylval = opp_strdup(yytext); return NAME; }
{D}+                     { count(); yylval = opp_strdup(yytext); return INTCONSTANT; }
0[xX]{X}+                { count(); yylval = opp_strdup(yytext); return INTCONSTANT; }
{D}+{E}                  { count(); yylval = opp_strdup(yytext); return REALCONSTANT; }
{D}*"."{D}+({E})?        { count(); yylval = opp_strdup(yytext); return REALCONSTANT; }

\"                       { BEGIN(stringliteral); count(); }
<stringliteral>{
      \n                 { BEGIN(INITIAL); throw std::runtime_error("Error parsing expression: unterminated string literal (append backslash to line for multi-line strings)"); /* NOTE: BEGIN(INITIAL) is important, otherwise parsing of the next file will start from the <stringliteral> state! */  }
      \\\n               { extendCount(); /* line continuation */ }
      \\\"               { extendCount(); /* qouted quote */ }
      \\[^\n\"]          { extendCount(); /* qouted char */ }
      [^\\\n\"]+         { extendCount(); /* character inside string literal */ }
      \"                 { extendCount(); yylval = opp_strdup(extendbuf.c_str()); BEGIN(INITIAL); return STRINGCONSTANT; /* closing quote */ }
}

","                      { count(); return ','; }
":"                      { count(); return ':'; }
"="                      { count(); return '='; }
"("                      { count(); return '('; }
")"                      { count(); return ')'; }
"["                      { count(); return '['; }
"]"                      { count(); return ']'; }
"."                      { count(); return '.'; }
"?"                      { count(); return '?'; }

"||"                     { count(); return OR_; }
"&&"                     { count(); return AND_; }
"##"                     { count(); return XOR_; }
"!"                      { count(); return NOT_; }

"|"                      { count(); return BINOR_; }
"&"                      { count(); return BINAND_; }
"#"                      { count(); return BINXOR_; }
"~"                      { count(); return BINCOMPL_; }
"<<"                     { count(); return SHIFTLEFT_; }
">>"                     { count(); return SHIFTRIGHT_; }

"^"                      { count(); return '^'; }
"+"                      { count(); return '+'; }
"-"                      { count(); return '-'; }
"*"                      { count(); return '*'; }
"/"                      { count(); return '/'; }
"%"                      { count(); return '%'; }
"<"                      { count(); return '<'; }
">"                      { count(); return '>'; }

"=="                     { count(); return EQ_; }
"!="                     { count(); return NE_; }
"<="                     { count(); return LE_; }
">="                     { count(); return GE_; }

{S}                      { count(); }
.                        { count(); return INVALID_CHAR; }

%%

int yywrap()
{
     return 1;
}

/*
 * - discards all remaining characters of a line of
 *   text from the inputstream.
 * - the characters are read with the input() and
 *   unput() functions.
 * - input() is sometimes called yyinput()...
 */
#ifdef __cplusplus
#define input  yyinput
#endif

/* the following #define is needed for broken flex versions */
#define yytext_ptr yytext

void comment()
{
    int c;
    while ((c = input())!='\n' && c!=0 && c!=EOF);
    if (c=='\n') unput(c);
}

/*
 * - counts the line and column number of the current token in `pos'
 * - keeps a record of the complete current line in `textbuf[]'
 * - yytext[] is the current token passed by (f)lex
 */
static void _count(bool updateprevpos)
{
    static int textbuflen;
    int i;

    /* printf("DBG: count(): prev=%d,%d  xpos=%d,%d yytext=>>%s<<\n",
           xprevpos.li, xprevpos.co, xpos.li, xpos.co, yytext);
    */

    /* init textbuf */
    if (xpos.li==1 && xpos.co==0) {
        textbuf[0]='\0'; textbuflen=0;
    }

    if (updateprevpos) {
        extendbuf = "";
        xprevpos = xpos;
    }
    extendbuf += yytext;
    for (i = 0; yytext[i] != '\0'; i++) {
        if (yytext[i] == '\n') {
            xpos.co = 0;
            xpos.li++;
            textbuflen=0; textbuf[0]='\0';
        } else if (yytext[i] == '\t')
            xpos.co += 8 - (xpos.co % 8);
        else
            xpos.co++;
        if (yytext[i] != '\n') {
            if (textbuflen < TEXTBUF_LEN-5) {
                textbuf[textbuflen++]=yytext[i]; textbuf[textbuflen]='\0';
            }
            else if (textbuflen == TEXTBUF_LEN-5) {
                strcpy(textbuf+textbuflen, "...");
                textbuflen++;
            } else {
                /* line too long -- ignore */
            }
        }
    }
}

void count()
{
    _count(true);
}

void extendCount()
{
    _count(false);
}


