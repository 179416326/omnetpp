//==========================================================================
// nedyylib.h  -
//
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2002-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __NEDYYLIB_H
#define __NEDYYLIB_H

#include <string>
#include "nedelements.h"
#include "nedyydefs.h"
#include "nedparser.h"


std::string slashifyFilename(const char *fname);

NEDElement *createNodeWithTag(int tagcode, NEDElement *parent=NULL);
void storePos(NEDElement *node, YYLTYPE pos);

PropertyNode *addProperty(NEDElement *node, const char *name);  // directly under the node
PropertyNode *addComponentProperty(NEDElement *node, const char *name); // into ParametersNode child of node

PropertyNode *storeSourceCode(NEDElement *node, YYLTYPE tokenpos);  // directly under the node
PropertyNode *storeComponentSourceCode(NEDElement *node, YYLTYPE tokenpos); // into ParametersNode child

void setFileComment(NEDElement *node);
void setBannerComment(NEDElement *node, YYLTYPE tokenpos);
void setRightComment(NEDElement *node, YYLTYPE tokenpos);
void setTrailingComment(NEDElement *node, YYLTYPE tokenpos);
void setComments(NEDElement *node, YYLTYPE pos);
void setComments(NEDElement *node, YYLTYPE firstpos, YYLTYPE lastpos);

ParamNode *addParameter(NEDElement *params, YYLTYPE namepos);
GateNode *addGate(NEDElement *gates, YYLTYPE namepos);

YYLTYPE trimBrackets(YYLTYPE vectorpos);
YYLTYPE trimAngleBrackets(YYLTYPE vectorpos);
YYLTYPE trimQuotes(YYLTYPE vectorpos);
YYLTYPE trimDoubleBraces(YYLTYPE vectorpos);
void swapAttributes(NEDElement *node, const char *attr1, const char *attr2);
void swapExpressionChildren(NEDElement *node, const char *attr1, const char *attr2);
void swapConnection(NEDElement *conn);
void moveChildren(NEDElement *from, NEDElement *to);

const char *toString(YYLTYPE);
const char *toString(long);

ExpressionNode *createExpression(NEDElement *expr);
OperatorNode *createOperator(const char *op, NEDElement *operand1, NEDElement *operand2=NULL, NEDElement *operand3=NULL);
FunctionNode *createFunction(const char *funcname, NEDElement *arg1=NULL, NEDElement *arg2=NULL, NEDElement *arg3=NULL, NEDElement *arg4=NULL);
IdentNode *createIdent(YYLTYPE parampos);
IdentNode *createIdent(YYLTYPE parampos, YYLTYPE modulepos, NEDElement *moduleindexexpr=NULL);
LiteralNode *createLiteral(int type, YYLTYPE valuepos, YYLTYPE textpos);
LiteralNode *createQuantity(const char *text);
NEDElement *unaryMinus(NEDElement *node);

void addVector(NEDElement *elem, const char *attrname, YYLTYPE exprpos, NEDElement *expr);
void addLikeParam(NEDElement *elem, const char *attrname, YYLTYPE exprpos, NEDElement *expr);
void addExpression(NEDElement *elem, const char *attrname, YYLTYPE exprpos, NEDElement *expr);

#endif

