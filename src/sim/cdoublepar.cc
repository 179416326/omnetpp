//==========================================================================
//   CDOUBLEPAR.CC  - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//  Author: Andras Varga
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include "cdoublepar.h"
#include "cstrtokenizer.h"
#include "cdynamicexpression.h"
#include "ccomponent.h"


cDoublePar::cDoublePar()
{
    val = 0;
}

cDoublePar::~cDoublePar()
{
    deleteOld();
}

void cDoublePar::operator=(const cDoublePar& other)
{
    if (this==&other) return;

    deleteOld();

    cParValue::operator=(other);
    if (flags & FL_ISEXPR)
        expr = (cExpression *) other.expr->dup();
    else
        val = other.val;
}

void cDoublePar::netPack(cCommBuffer *buffer)
{
    //TBD
}

void cDoublePar::netUnpack(cCommBuffer *buffer)
{
    //TBD
}

void cDoublePar::setBoolValue(bool b)
{
    throw cRuntimeError(this, eBADCAST, "bool", "double");
}

void cDoublePar::setLongValue(long l)
{
    deleteOld();
    val = l;
    flags |= FL_HASVALUE;
}

void cDoublePar::setDoubleValue(double d)
{
    deleteOld();
    val = d;
    flags |= FL_HASVALUE;
}

void cDoublePar::setStringValue(const char *s)
{
    throw cRuntimeError(this, eBADCAST, "string", "double");
}

void cDoublePar::setXMLValue(cXMLElement *node)
{
    throw cRuntimeError(this, eBADCAST, "XML", "double");
}

void cDoublePar::setExpression(cExpression *e)
{
    deleteOld();
    expr = e;
    flags |= FL_ISEXPR | FL_HASVALUE;
    setUnit(e->unit());
}

bool cDoublePar::boolValue(cComponent *) const
{
    throw cRuntimeError(this, eBADCAST, "double", "bool");
}

long cDoublePar::longValue(cComponent *context) const
{
    return double_to_long(evaluate(context));
}

double cDoublePar::doubleValue(cComponent *context) const
{
    return evaluate(context);
}

const char *cDoublePar::stringValue(cComponent *) const
{
    throw cRuntimeError(this, eBADCAST, "double", "string");
}

std::string cDoublePar::stdstringValue(cComponent *) const
{
    throw cRuntimeError(this, eBADCAST, "double", "string");
}

cXMLElement *cDoublePar::xmlValue(cComponent *) const
{
    throw cRuntimeError(this, eBADCAST, "double", "XML");
}

cExpression *cDoublePar::expression() const
{
    return (flags | FL_ISEXPR) ? expr : NULL;
}

double cDoublePar::evaluate(cComponent *context) const
{
    return (flags & FL_ISEXPR) ? expr->doubleValue(context) : val;
}

void cDoublePar::deleteOld()
{
    if (flags & FL_ISEXPR)
    {
        delete expr;
        flags &= ~FL_ISEXPR;
    }
}

cPar::Type cDoublePar::type() const
{
    return cPar::DOUBLE;
}

bool cDoublePar::isNumeric() const
{
    return true;
}

void cDoublePar::convertToConst(cComponent *context)
{
    setDoubleValue(doubleValue(context));
}

std::string cDoublePar::toString() const
{
    if (flags & FL_ISEXPR)
        return expr->toString();

    char buf[32];
    sprintf(buf, "%g", val);
    return buf;
}

bool cDoublePar::parse(const char *text)
{
    // maybe it's just a number
    cStringTokenizer tok(text);
    const char *word = tok.nextToken();
    if (word!=NULL && !tok.hasMoreTokens())
    {
        char *endp;
        double num = strtod(word, &endp);  // FIXME TBD try as "units" as well
        if (*endp == '\0')
        {
            setDoubleValue(num);
            return true;
        }
    }

    // try parsing it as an expression
    cDynamicExpression *dynexpr = new cDynamicExpression();
    if (dynexpr->parse(text))   //FIXME catch exceptions!!!!! in all partypes!!!!!
    {
        setExpression(dynexpr);
        if (dynexpr->isAConstant()) //FIXME add this trick to all param types???
            convertToConst(NULL); // optimization: store as a constant value instead of an expression
        return true;
    }

    // bad luck
    return false;
}

int cDoublePar::compare(const cParValue *other) const
{
    int ret = cParValue::compare(other);
    if (ret!=0)
        return ret;

    const cDoublePar *other2 = dynamic_cast<const cDoublePar *>(other);
    if (flags & FL_ISEXPR)
        throw cRuntimeError(this, "cannot compare expressions yet"); //FIXME
    else
        return (val == other2->val) ? 0 : (val < other2->val) ? -1 : 1;
}

