/*==================================================
 * File: expr.lex
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


%{
#include <string.h>

#if defined(_MSC_VER)
# include <io.h>
# define isatty _isatty
#else
# include <unistd.h>  // isatty
#endif

#include "expryydefs.h"
#include "expr.tab.h"

#define yylval expryylval
extern YYSTYPE yylval;

// wrap symbols to allow several .lex files coexist
#define comment     exprcomment
#define count       exprcount
#define extendCount exprextendCount

void comment(void);
void count(void);
void extendCount(void);

#define TEXTBUF_LEN 1024
static char textbuf[TEXTBUF_LEN];

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

{L}({L}|{D})*           { count(); yylval = opp_strdup(yytext); return NAME; }
{D}+                    { count(); yylval = opp_strdup(yytext); return INTCONSTANT; }
0[xX]{X}+               { count(); yylval = opp_strdup(yytext); return INTCONSTANT; }
{D}+{E}                 { count(); yylval = opp_strdup(yytext); return REALCONSTANT; }
{D}*"."{D}+({E})?       { count(); yylval = opp_strdup(yytext); return REALCONSTANT; }

\"[^\"]*\"              { count(); yylval = opp_strdup(yytext); return STRINGCONSTANT; }

","                     { count(); return (','); }
":"                     { count(); return (':'); }
"="                     { count(); return ('='); }
"("                     { count(); return ('('); }
")"                     { count(); return (')'); }
"["                     { count(); return ('['); }
"]"                     { count(); return (']'); }
"."                     { count(); return ('.'); }
"?"                     { count(); return ('?'); }

"||"                    { count(); return OR_; }
"&&"                    { count(); return AND_; }
"##"                    { count(); return XOR_; }
"!"                     { count(); return NOT_; }

"|"                     { count(); return BINOR_; }
"&"                     { count(); return BINAND_; }
"#"                     { count(); return BINXOR_; }
"~"                     { count(); return BINCOMPL_; }
"<<"                    { count(); return SHIFTLEFT_; }
">>"                    { count(); return SHIFTRIGHT_; }

"^"                     { count(); return '^'; }
"+"                     { count(); return '+'; }
"-"                     { count(); return '-'; }
"*"                     { count(); return '*'; }
"/"                     { count(); return '/'; }
"%"                     { count(); return '%'; }
"<"                     { count(); return '<'; }
">"                     { count(); return '>'; }

"=="                    { count(); return EQ_; }
"!="                    { count(); return NE_; }
"<="                    { count(); return LE_; }
">="                    { count(); return GE_; }

{S}                     { count(); }
.                       { count(); return INVALID_CHAR; }

%%

int yywrap(void)
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

void comment(void)
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
static void _count(int updateprevpos)
{
    static int textbuflen;
    int i;

    /* printf("DBG: count(): prev=%d,%d  pos=%d,%d yytext=>>%s<<\n",
           prevpos.li, prevpos.co, pos.li, pos.co, yytext);
    */

    /* init textbuf */
    if (pos.li==1 && pos.co==0) {
        textbuf[0]='\0'; textbuflen=0;
    }

    if (updateprevpos) {
        prevpos = pos;
    }
    for (i = 0; yytext[i] != '\0'; i++) {
        if (yytext[i] == '\n') {
            pos.co = 0;
            pos.li++;
            textbuflen=0; textbuf[0]='\0';
        } else if (yytext[i] == '\t')
            pos.co += 8 - (pos.co % 8);
        else
            pos.co++;
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

void count(void)
{
    _count(1);
}

void extendCount(void)
{
    _count(0);
}


