/*==================================================
 * File: msg2.lex
 *
 *  Lexical analyser for OMNeT++ MSG-2.
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
 * MSG-2 reserved words:
 *    non-component:   cplusplus
 *    component:       struct message class noncobject enum
 *    inheritance:     extends
 *    field types:     char short int long double unsigned
 *    field modifiers: abstract readonly
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

// #include "nedgrammar.h"
#include "msg2.tab.h"

extern YYSTYPE yylval;
extern YYLTYPE yylloc;

// wrap symbols to allow several .lex files coexist
#define comment     msgcomment
#define count       msgcount
#define extendCount msgextendCount

void comment(void);
void count(void);
void extendCount(void);

/* Vars updated by count(): */
LineColumn pos,prevpos;

#define TEXTBUF_LEN 1024
char textbuf[TEXTBUF_LEN];

%}

%%
"//"                    { comment(); }

"cplusplus"             { count(); return CPLUSPLUS; }
"struct"                { count(); return STRUCT; }
"message"               { count(); return MESSAGE; }
"class"                 { count(); return CLASS; }
"noncobject"            { count(); return NONCOBJECT; }
"enum"                  { count(); return ENUM; }
"extends"               { count(); return EXTENDS; }
"abstract"              { count(); return ABSTRACT; }
"readonly"              { count(); return READONLY; }

"bool"                  { count(); return BOOLTYPE; }
"char"                  { count(); return CHARTYPE; }
"short"                 { count(); return SHORTTYPE; }
"int"                   { count(); return INTTYPE; }
"long"                  { count(); return LONGTYPE; }
"double"                { count(); return DOUBLETYPE; }
"unsigned"              { count(); return UNSIGNED_; }
"string"                { count(); return STRINGTYPE; }

{L}({L}|{D})*           { count(); return NAME; }
{D}+                    { count(); return INTCONSTANT; }
0[xX]{X}+               { count(); return INTCONSTANT; }
{D}+{E}                 { count(); return REALCONSTANT; }
{D}*"."{D}+({E})?       { count(); return REALCONSTANT; }

\"[^\"]*\"              { count(); return STRINGCONSTANT; }
\'[^\']\'               { count(); return CHARCONSTANT; }

"{{"                    { count(); BEGIN(cplusplusbody); }
<cplusplusbody>"}}"     { extendCount(); BEGIN(INITIAL); return CPLUSPLUSBODY; }
<cplusplusbody>{S}      { extendCount(); }
<cplusplusbody>.        { extendCount(); }

"++"                    { count(); return PLUSPLUS; }

";"                     { count(); return (';'); }
","                     { count(); return (','); }
":"                     { count(); return (':'); }
"="                     { count(); return ('='); }
"("                     { count(); return ('('); }
")"                     { count(); return (')'); }
"["                     { count(); return ('['); }
"]"                     { count(); return (']'); }
"{"                     { count(); return ('{'); }
"}"                     { count(); return ('}'); }
"."                     { count(); return ('.'); }
"?"                     { count(); return ('?'); }

/* FIXME are the next ones really needed? */

"||"                    { count(); return OR; }
"&&"                    { count(); return AND; }
"##"                    { count(); return XOR; }
"!"                     { count(); return NOT; }

"|"                     { count(); return BIN_OR; }
"&"                     { count(); return BIN_AND; }
"#"                     { count(); return BIN_XOR; }
"~"                     { count(); return BIN_COMPL; }
"<<"                    { count(); return SHIFT_LEFT; }
">>"                    { count(); return SHIFT_RIGHT; }

"^"                     { count(); return '^'; }
"+"                     { count(); return '+'; }
"-"                     { count(); return '-'; }
"*"                     { count(); return '*'; }
"/"                     { count(); return '/'; }
"%"                     { count(); return '%'; }

"=="                    { count(); return EQ; }
"!="                    { count(); return NE; }
"<"                     { count(); return LS; }
"<="                    { count(); return LE; }
">"                     { count(); return GT; }
">="                    { count(); return GE; }

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
void _count(int updateprevpos)
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
    /* printf("li=%d co=%d\n", pos.li, pos.co); good for debugging... */
    yylloc.first_line   = prevpos.li;
    yylloc.first_column = prevpos.co;
    yylloc.last_line    = pos.li;
    yylloc.last_column  = pos.co;
}

void count(void)
{
    _count(1);
}

void extendCount(void)
{
    _count(0);
}


