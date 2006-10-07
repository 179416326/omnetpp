//==========================================================================
//   CEXPRESSIONBUILDER.CC
//
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2002-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `terms' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#include "nedelements.h"
#include "nederror.h"
#include "cexpressionbuilder.h"
#include "cfunction.h"
#include "cnedfunction.h"


cExpressionBuilder::cExpressionBuilder()
{
    elems = NULL;
}

cExpressionBuilder::~cExpressionBuilder()
{
}

void cExpressionBuilder::doNode(NEDElement *node)
{
    if (pos > limit)
        throw new cRuntimeError("dynamic module builder: expression too long");
    int tagcode = node->getTagCode();
    switch (tagcode)
    {
        case NED_OPERATOR:
            doOperator((OperatorNode *)node); break;
        case NED_FUNCTION:
            doFunction((FunctionNode *)node); break;
        case NED_IDENT:
            doIdent((IdentNode *)node); break;
        case NED_LITERAL:
            doLiteral((LiteralNode *)node); break;
        default:
            throw new cRuntimeError("dynamic module builder: unexpected tag in expression: %s", node->getTagName());
    }
}

void cExpressionBuilder::doOperator(OperatorNode *node)
{
    // push args first
    for (NEDElement *op=node->getFirstChild(); op; op=op->getNextSibling())
        doNode(op);

    // determine name and arg count
    const char *name = node->getName();
    NEDElement *op1 = node->getFirstChild();
    NEDElement *op2 = op1 ? op1->getNextSibling() : NULL;
    NEDElement *op3 = op2 ? op2->getNextSibling() : NULL;

    if (!op2)
    {
        // unary:
        if (!strcmp(name,"-"))
            elems[pos++] = 'M';
        else if (!strcmp(name,"!"))
            elems[pos++] = 'N';
        else if (!strcmp(name,"~"))
            elems[pos++] = '~';
        else
            throw new cRuntimeError("dynamic module builder: unexpected operator %s", name);
    }
    else if (!op3)
    {
        // binary:

        // arithmetic
        if (!strcmp(name,"+"))
            elems[pos++] = '+';
        else if (!strcmp(name,"-"))
            elems[pos++] = '-';
        else if (!strcmp(name,"*"))
            elems[pos++] = '*';
        else if (!strcmp(name,"/"))
            elems[pos++] = '/';
        else if (!strcmp(name,"%"))
            elems[pos++] = '%';
        else if (!strcmp(name,"^"))
            elems[pos++] = '^';

        // logical
        else if (!strcmp(name,"=="))
            elems[pos++] = '=';
        else if (!strcmp(name,"!="))
            elems[pos++] = '!';
        else if (!strcmp(name,"<"))
            elems[pos++] = '<';
        else if (!strcmp(name,"<="))
            elems[pos++] = '{';
        else if (!strcmp(name,">"))
            elems[pos++] = '>';
        else if (!strcmp(name,">="))
            elems[pos++] = '}';
        else if (!strcmp(name,"&&"))
            elems[pos++] = 'A';
        else if (!strcmp(name,"||"))
            elems[pos++] = 'O';
        else if (!strcmp(name,"##"))
            elems[pos++] = 'X';

        // bitwise
        else if (!strcmp(name,"&"))
            elems[pos++] = '&';
        else if (!strcmp(name,"|"))
            elems[pos++] = '|';
        else if (!strcmp(name,"#"))
            elems[pos++] = '#';
        else if (!strcmp(name,"<<"))
            elems[pos++] = 'L';
        else if (!strcmp(name,">>"))
            elems[pos++] = 'R';
        else
            throw new cRuntimeError("dynamic module builder: unexpected operator %s", name);
    }
    else
    {
        // tertiary can only be "?:"
        if (!strcmp(name,"?:"))
            elems[pos++] = '?';
        else
            throw new cRuntimeError("dynamic module builder: unexpected operator %s", name);
    }
}

void cExpressionBuilder::doFunction(FunctionNode *node)
{
    // get function name, arg count, args
    const char *funcname = node->getName();
    int argcount = node->getNumChildren();

    // operators should be handled specially
//FIXME handle "const" operator as well!
    if (!strcmp(funcname,"index"))
    {
        elems[pos++] = cNEDFunction::find("moduleIndex",0);   //FIXME rename to "index";  what if we don't find it
        return;
    }
    else if (!strcmp(funcname,"sizeof"))
    {
        throw new cRuntimeError("dynamic module builder: sizeof: not yet");
/*FIXME convert into sizeof; this will depend on inSubcomponentScope

        IdentNode *op1 = node->getFirstIdentChild();
        ASSERT(op1);
        const char *name = op1->getName();

        elems[pos++] = name;
        elems[pos++] = cNEDFunction::find("sizeof",0);   //FIXME write "sizeof"; what if we don't find it

        ele// TBD this is duplicate code -- same occurs in evaluated expressions as well
        cModule *parentmodp = submodp->parentModule();
        if (!parentmodp)
            throw new cRuntimeError("dynamic module builder: sizeof() occurs in wrong context", name);

        // find among parent module gates
        cGate *g = parentmodp->gate(name);
        if (g)
        {
            elems[pos++] = g->size();
            return;
        }

        // if not found, find among submodules. If there's no such submodule, it may
        // be that such submodule vector never existed, or can be that it's zero
        // size -- we cannot tell, so we have to return 0.
        cModule *m = _submodule(parentmodp, name,0);
        if (!m && _submodule(parentmodp, name))
            throw new cRuntimeError("dynamic module builder: sizeof(): %s is not a vector submodule", name);
        elems[pos++] = m ? m->size() : 0;
        return;
*/
    }

    // push args first
    for (NEDElement *child=node->getFirstChild(); child; child=child->getNextSibling())
        doNode(child);

    // normal function: find it and add to reverse Polish expression
    cMathFunction *functype = cMathFunction::find(funcname,argcount);
    cNEDFunction *nedfunctype = cNEDFunction::find(funcname,argcount);
    if (functype)
    {
        switch (argcount)
        {
            case 0: elems[pos++] = functype->mathFuncNoArg(); break;
            case 1: elems[pos++] = functype->mathFunc1Arg(); break;
            case 2: elems[pos++] = functype->mathFunc2Args(); break;
            case 3: elems[pos++] = functype->mathFunc3Args(); break;
            case 4: elems[pos++] = functype->mathFunc4Args(); break;
            default: throw new cRuntimeError("dynamic module builder: internal error: function with %d args???", funcname, argcount);
        }
    }
    else if (nedfunctype)
    {
        elems[pos++] = nedfunctype;
    }
    else
    {
        throw new cRuntimeError("dynamic module builder: function %s with %d args not found", funcname, argcount);
    }
}

void cExpressionBuilder::doIdent(IdentNode *node)
{
    const char *varname = node->getName();
    elems[pos++] = varname;
    //FIXME todo: maybe parentParameter etc, depending on inSubcomponentScope
    elems[pos++] = cNEDFunction::find("parameter",0);   //FIXME what if we don't find it

}

void cExpressionBuilder::doLiteral(LiteralNode *node)
{
    const char *val = node->getValue();
    switch (node->getType())
    {
        case NED_CONST_BOOL:   elems[pos++] = !strcmp(val,"true"); break;
        case NED_CONST_INT:    elems[pos++] = strtol(node->getValue(), NULL, 0); break; // this handles hex as well
        case NED_CONST_DOUBLE: elems[pos++] = atof(node->getValue()); break;
        case NED_CONST_UNIT:   elems[pos++] = strToSimtime(node->getValue()); break;//XXX
        case NED_CONST_STRING: elems[pos++] = node->getValue(); break;
        default: throw new cRuntimeError("dynamic module builder: evaluate: internal error: wrong constant type");
    }
}

cDynamicExpression *cExpressionBuilder::process(ExpressionNode *node, bool inSubcomponentScope)
{
    // create dynamically evaluated expression (reverse Polish).
    // we don't know the size in advance, so first collect it in elems[1000],
    // then make a copy
    this->inSubcomponentScope = inSubcomponentScope;
    elems = new cDynamicExpression::Elem[1000];
    pos = 0;
    limit = 990;

    doNode(node->getFirstChild());

    int n = pos;
    cDynamicExpression::Elem *newElems = new cDynamicExpression::Elem[n];
    for (int i=0; i<n; i++)
        newElems[i] = elems[i];

    cDynamicExpression *ret = new cDynamicExpression();
    ret->setExpression(newElems, n);

    delete [] elems;
    elems = NULL;

    return ret;
}

