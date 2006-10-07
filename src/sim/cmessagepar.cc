//=========================================================================
//  CMESSAGEPAR.CC - part of
//
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//  Author: Andras Varga
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <math.h>
#include <string.h>
#include <stdio.h>
#include <ctype.h>
#include <stdlib.h>
#include <string>
#include "cmessagepar.h"
#include "cstat.h"
#include "globals.h"
#include "cfunction.h"
#include "cxmlelement.h"
#include "cconfig.h"
#include "cenvir.h"
#include "cexception.h"
#include "csimulation.h"

#ifdef WITH_PARSIM
#include "ccommbuffer.h"
#endif

using std::ostream;
using std::string;


//==============================================
//=== Registration
Register_Class(cMessagePar);

//==========================================================================
//=== cDoubleExpression - member functions

string cDoubleExpression::getAsText()
{
    char buf[40];
    sprintf(buf, "%g", evaluate());
    return string(buf);
}

bool cDoubleExpression::parseText(const char *text)
{
    throw new cRuntimeError("cDoubleExpression: parseText() does not work with compiled expressions");
}

//==========================================================================
//=== cMessagePar - member functions

char *cMessagePar::possibletypes = "SBLDFIXCTP";

static const char *typeName(char typechar)
{
    switch (typechar)
    {
        case 'S': return "string (S)";
        case 'B': return "bool (B)";
        case 'L': return "long (L)";
        case 'D': return "double (D)";
        case 'F': return "function with constant args (F)";
        case 'I': return "indirect (I)";
        case 'X': return "reverse Polish expression (X)";
        case 'C': return "compiled expression (C)";
        case 'T': return "random number from distribution (T)";
        case 'P': return "pointer (P)";
        case 'M': return "XML element (M)";
        default:  return "invalid type char";
    }
}

// constructors
cMessagePar::cMessagePar(const char *name) : cObject( name )
{
    tkownership = false;
    changedflag = inputflag = false;
    typechar = 'L'; lng.val = 0L;
}

cMessagePar::cMessagePar(const cMessagePar& par) : cObject()
{
    tkownership = false;
    changedflag = inputflag = false;
    typechar = 'L'; lng.val = 0L;

    setName( par.name() );
    cMessagePar::operator=(par);
}

cMessagePar::cMessagePar(const char *name, cMessagePar& other) : cObject(name)
{
    tkownership = false;
    changedflag = inputflag = false;
    typechar = 'L'; lng.val = 0L;

    setName(name);
    operator=(other);
}

cMessagePar::~cMessagePar()
{
    beforeChange();
    if (isRedirected())
        cancelRedirection();
    deleteOld();
}

void cMessagePar::deleteOld()
{
    if (typechar=='S' && !ls.sht)
    {
        delete [] ls.str;
    }
    else if (typechar=='T')
    {
        if (dtr.res->owner()==this)
            dropAndDelete(dtr.res);
    }
    else if (typechar=='C')
    {
        delete cexpr.expr;
    }
    else if (typechar=='P')
    {
        if (ptr.dupfunc || ptr.itemsize>0) // we're expected to do memory mgmt
        {
            if (ptr.delfunc)
                ptr.delfunc(ptr.ptr);
            else
                delete [] (char *)ptr.ptr;  // delete void* is no longer legal :-(
        }
    }
    else if (typechar=='O')
    {
        if (obj.obj->owner()==this)
            dropAndDelete(obj.obj);
    }
    else if (typechar=='X')
    {
        for (int i=0; i<expr.n; i++)
            if (expr.xelem[i].type=='R')  // should be ours
                dropAndDelete(expr.xelem[i].p);
        delete [] expr.xelem;
    }
    typechar = 'L';
}

//----------------------------------------------------------------------
// redefine virtual cObject funcs

std::string cMessagePar::info() const
{
    std::stringstream out;

    // redirection?
    if (isRedirected())
    {
        out << "--> " << ind.par->fullPath();
        return out.str();
    }

    // append useful info
    cMathFunction *ff;
    const char *s;
    switch (typechar) {
        case 'S': s = ls.sht ? ss.str:ls.str;
                  if (!s) s = "";
                  out << "\"" << s << "\" (S)";
                  break;
        case 'L': out << lng.val << " (L)"; break;
        case 'D': out << dbl.val << " (D)"; break;
        case 'C': out << "compiled expression"; break;
        case 'X': out << "reverse Polish expression"; break;
        case 'T': out << (dtr.res ? dtr.res->fullPath().c_str():"null") << " (T)"; break;
        case 'P': out << ptr.ptr << " (P)"; break;
        case 'O': out << (obj.obj ? obj.obj->fullPath().c_str():"null") << " (O)"; break;
        case 'F': ff = cMathFunction::findByPointer(func.f);
                  out << (ff ? ff->name() : "unknown") << "(";
                  switch(func.argc) {
                    case 0: out << ")"; break;
                    case 1: out << func.p1; break;
                    case 2: out << func.p1 << "," << func.p2; break;
                    case 3: out << func.p1 << "," << func.p2 << "," << func.p3; break;
                    case 4: out << func.p1 << "," << func.p2 << "," << func.p3 << "," << func.p4; break;
                    default:out << ") with " << func.argc << " args"; break;
                  };
                  out << " (F)";
                  break;
        case 'B': out << (lng.val?"true":"false") << " (B)"; break;
        case 'M': if (xmlp.node)
                      out << "<" << xmlp.node->getTagName() << "> from " << xmlp.node->getSourceLocation() << " (M)";
                  else
                      out << "null (M)";
                  break;
        default : out << "? (unknown type)"; break;
    }
    return out.str();
}

void cMessagePar::writeContents(ostream& os)
{
    if (isRedirected())
    {
        os << "  Type:   redirection ('I')\n";
        os << "  Target: " << ind.par->fullPath() << '\n';
    }
    else
    {
        os << "  Type:  " << typechar << '\n';
        os << "  Value: " << getAsText().c_str() << '\n';
    }
}

void cMessagePar::forEachChild(cVisitor *v)
{
    if (typechar=='T')
    {
        v->visit(dtr.res);
    }
    else if (typechar=='O')
    {
        if (obj.obj)
            v->visit(obj.obj);
    }
}

void cMessagePar::netPack(cCommBuffer *buffer)
{
#ifndef WITH_PARSIM
    throw new cRuntimeError(this,eNOPARSIM);
#else
    cObject::netPack(buffer);

    // For error checking & handling
    if (typechar != 'S' && typechar != 'C' && typechar != 'L' && typechar != 'D'
        && typechar != 'F' && typechar != 'T' && typechar != 'I' && typechar != 'X'
        && typechar != 'P' && typechar != 'O' && typechar != 'M')
    {
        throw new cRuntimeError(this,"netPack: unsupported type '%c'",typechar);
    }

    buffer->pack(typechar);
    buffer->pack(inputflag);
    buffer->pack(changedflag);
    buffer->pack(promptstr.buffer());

    cMathFunction *ff;
    switch (typechar)
    {
    case 'S':
        buffer->pack(ls.sht);
        if (ls.sht)
            buffer->pack(ls.str, sizeof(ls.str));
        else
            buffer->pack(ss.str);
        break;

    case 'L':
        buffer->pack(lng.val);
        break;

    case 'D':
        buffer->pack(dbl.val);
        break;

    case 'F':
        ff = cMathFunction::findByPointer(func.f);
        if (ff == NULL)
            throw new cRuntimeError(this,"netPack(): cannot transmit unregistered function");

        buffer->pack(ff->name());
        buffer->pack(func.argc);
        buffer->pack(func.p1);
        buffer->pack(func.p2);
        buffer->pack(func.p3);
        buffer->pack(func.p4);
        break;

    case 'T':
        if (dtr.res && dtr.res->owner() != this)
            throw new cRuntimeError(this,"netPack(): cannot transmit pointer to \"external\" object");
        if (buffer->packFlag(dtr.res!=NULL))
            buffer->packObject(dtr.res);
        break;

    case 'I':
        throw new cRuntimeError(this,"netPack(): transmitting indirect values (type 'I') not supported");

    case 'X':
        // not implemented, because there are functions and pointers in it.
        throw new cRuntimeError(this,"netPack(): transmitting expressions (type 'X') not supported");

    case 'P':
        throw new cRuntimeError(this,"netPack(): cannot transmit pointer to unknown data structure (type 'P')");

    case 'O':
        if (obj.obj && obj.obj->owner() != this)
            throw new cRuntimeError(this,"netPack(): cannot transmit pointer to \"external\" object");
        if (buffer->packFlag(obj.obj!=NULL))
            buffer->packObject(obj.obj);
        break;

    case 'M':
        throw new cRuntimeError(this,"netPack(): cannot transmit pointer to XML element (type 'M')");
    }
#endif
}

void cMessagePar::netUnpack(cCommBuffer *buffer)
{
#ifndef WITH_PARSIM
    throw new cRuntimeError(this,eNOPARSIM);
#else
    char *funcname;
    int argc;

    cObject::netUnpack(buffer);

    buffer->unpack(typechar);
    buffer->unpack(inputflag);
    buffer->unpack(changedflag);
    buffer->unpack(promptstr);

    cMathFunction *ff;
    switch (typechar)
    {
    case 'S':
        buffer->unpack(ls.sht);
        ss.sht = ls.sht;
        if (ls.sht)
            buffer->unpack(ss.str, sizeof(ls.str));
        else
            buffer->unpack(ls.str);
        break;

    case 'L':
        buffer->unpack(lng.val);
        break;

    case 'D':
        buffer->unpack(dbl.val);
        break;

    case 'F':
        buffer->unpack(funcname);
        buffer->unpack(argc);
        ff = findFunction(funcname,argc);
        if (ff == NULL)
        {
            delete [] funcname;
            throw new cRuntimeError(this,"netUnpack(): transmitted function `%s' with %d args not registered here",
                                    funcname, argc);
        }
        func.f = ff->mathFunc();
        func.argc = argc;
        buffer->unpack(func.p1);
        buffer->unpack(func.p2);
        buffer->unpack(func.p3);
        buffer->unpack(func.p4);
        delete [] funcname;
        break;

    case 'T':
        if (!buffer->checkFlag())
            dtr.res = NULL;
        else
            take(dtr.res = (cStatistic *) buffer->unpackObject());
        break;

    case 'I':
    case 'X':
    case 'P':
    case 'M':
        throw new cRuntimeError(this,"netUnpack(): unpacking types I, P, X, M not implemented");

    case 'O':
        if (!buffer->checkFlag())
            obj.obj = NULL;
        else
            take(obj.obj = buffer->unpackObject());
        break;
    }
#endif
}

//-------------------------------------------------------------------------
// set/get flags

char cMessagePar::type() const
{
     if (isRedirected())
         return ind.par->type();
     return typechar;
}

const char *cMessagePar::prompt()
{
     if (isRedirected())
         return ind.par->prompt();
     return promptstr.c_str();
}

bool cMessagePar::isInput() const
{
     if (isRedirected())
         return ind.par->isInput();
     return inputflag;
}

bool cMessagePar::changed()
{
     if (isRedirected())
         return ind.par->changed();
     bool ch = changedflag;
     changedflag=false;
     return ch;
}

void cMessagePar::setPrompt(const char *s)
{
     if (isRedirected())
         {ind.par->setPrompt(s);return;}
     promptstr = s;     // string's operator=() does delete+opp_strdup()
}

void cMessagePar::setInput(bool ip)
{
     if (isRedirected())
         {ind.par->setInput(ip);return;}
     inputflag = ip;
}

//-----------------------------------------------------------------------
// setXxxValue() funcs

cMessagePar& cMessagePar::setStringValue(const char *s)
{
     if (isRedirected())
         return ind.par->setStringValue(s);

     beforeChange();
     deleteOld();
     typechar = 'S';
     inputflag=false;
     if (!s)
         {ls.sht=true; *ss.str='\0';}
     else if ((ls.sht=(strlen(s)<=SHORTSTR))!=0)
         opp_strcpy(ss.str, s);
     else
         ls.str = opp_strdup(s);
     afterChange();
     return *this;
}

cMessagePar& cMessagePar::setBoolValue(bool b)
{
    if (isRedirected())
        return ind.par->setBoolValue(b);

    beforeChange();
    deleteOld();
    lng.val = b;
    typechar = 'B';
    inputflag=false;
    afterChange();
    return *this;
}

cMessagePar& cMessagePar::setLongValue(long l)
{
    if (isRedirected())
        return ind.par->setLongValue(l);

    beforeChange();
    deleteOld();
    lng.val = l;
    typechar = 'L';
    inputflag=false;
    afterChange();
    return *this;
}

cMessagePar& cMessagePar::setDoubleValue(double d)
{
    if (isRedirected())
        return ind.par->setDoubleValue(d);

    beforeChange();
    deleteOld();
    dbl.val = d;
    typechar = 'D';
    inputflag=false;
    afterChange();
    return *this;
}

cMessagePar& cMessagePar::setDoubleValue(MathFuncNoArg f)
{
    if (isRedirected())
        return ind.par->setDoubleValue(f);

    beforeChange();
    deleteOld();
    func.f = (MathFunc)f;
    func.argc=0;
    typechar = 'F';
    inputflag=false;
    afterChange();
    return *this;
}

cMessagePar& cMessagePar::setDoubleValue(MathFunc1Arg f, double p1)
{
    if (isRedirected())
        return ind.par->setDoubleValue(f,p1);

    beforeChange();
    deleteOld();
    func.f = (MathFunc)f;
    func.argc=1;
    func.p1 = p1;
    typechar = 'F';
    inputflag=false;
    afterChange();
    return *this;
}

cMessagePar& cMessagePar::setDoubleValue(MathFunc2Args f, double p1, double p2)
{
    if (isRedirected())
        return ind.par->setDoubleValue(f,p1,p2);

    beforeChange();
    deleteOld();
    func.f = (MathFunc)f;
    func.argc=2;
    func.p1 = p1;
    func.p2 = p2;
    typechar = 'F';
    inputflag=false;
    afterChange();
    return *this;
}

cMessagePar& cMessagePar::setDoubleValue(MathFunc3Args f, double p1, double p2, double p3)
{
    if (isRedirected())
        return ind.par->setDoubleValue(f,p1,p2,p3);

    beforeChange();
    deleteOld();
    func.f = (MathFunc)f;
    func.argc=3;
    func.p1 = p1;
    func.p2 = p2;
    func.p3 = p3;
    typechar = 'F';
    inputflag=false;
    afterChange();
    return *this;
}

cMessagePar& cMessagePar::setDoubleValue(MathFunc4Args f, double p1, double p2, double p3, double p4)
{
    if (isRedirected())
        return ind.par->setDoubleValue(f,p1,p2,p3,p4);

    beforeChange();
    deleteOld();
    func.f = (MathFunc)f;
    func.argc=4;
    func.p1 = p1;
    func.p2 = p2;
    func.p3 = p3;
    func.p4 = p4;
    typechar = 'F';
    inputflag=false;
    afterChange();
    return *this;
}

cMessagePar& cMessagePar::setDoubleValue(ExprElem *x, int n)
{
    if (isRedirected())
        return ind.par->setDoubleValue(x,n);

    if (!x)
        throw new cRuntimeError(this,eBADINIT,typeName('X'));

    beforeChange();
    deleteOld();
    expr.n = n;
    expr.xelem = x;
    typechar = 'X';
    inputflag=false;

    // ownership game: take objects given to us in the x[] array
    for(int i=0; i<expr.n; i++)
    {
       if (expr.xelem[i].type=='R')
       {
           //  ExprElem::op= has already dupped the pointed cMessagePar for us
           cMessagePar *p = expr.xelem[i].p;

           // if the pointed cMessagePar is not indirect and it is a constant,
           // there's really no need to keep the object
           if (!p->isRedirected() && (p->type()=='D' || p->type()=='L'))
           {
               expr.xelem[i] = (double)(*p);
               delete p;
           }
           else // otherwise, we'll become the owner
           {
               take(p);
           }
       }
    }
    afterChange();
    return *this;
}

cMessagePar& cMessagePar::setDoubleValue(cDoubleExpression *p)
{
    if (isRedirected())
        return ind.par->setDoubleValue(p);

    if (!p)
        throw new cRuntimeError(this,eBADINIT,typeName('P'));

    beforeChange();
    deleteOld();
    cexpr.expr = p;
    typechar = 'C';
    inputflag=false;
    afterChange();
    return *this;
}

cMessagePar& cMessagePar::setDoubleValue(cStatistic *res)
{
    if (isRedirected())
        return ind.par->setDoubleValue(res);

    if (!res)
        throw new cRuntimeError(this,eBADINIT,typeName('T'));

    beforeChange();
    deleteOld();
    dtr.res = res;
    if (takeOwnership())
       take(res);
    typechar = 'T';
    inputflag=false;
    afterChange();
    return *this;
}

cMessagePar& cMessagePar::setPointerValue(void *_ptr)
{
    if (isRedirected())
        return ind.par->setPointerValue(_ptr);

    beforeChange();
    // if it was a 'P' before, keep previous configuration
    if (typechar!='P')
    {
        deleteOld();
        ptr.delfunc=NULL;
        ptr.dupfunc=NULL;
        ptr.itemsize=0;
        typechar = 'P';
    }
    ptr.ptr = _ptr;
    inputflag=false;
    afterChange();
    return *this;
}

cMessagePar& cMessagePar::setObjectValue(cObject *_obj)
{
    if (isRedirected())
        return ind.par->setObjectValue(_obj);

    beforeChange();
    deleteOld();
    obj.obj = _obj;
    if (takeOwnership())
        take( _obj );
    typechar = 'O';
    inputflag=false;
    afterChange();
    return *this;
}

cMessagePar& cMessagePar::setXMLValue(cXMLElement *node)
{
    if (isRedirected())
        return ind.par->setXMLValue(node);

    beforeChange();
    deleteOld();
    xmlp.node = node;
    typechar = 'M';
    inputflag=false;
    afterChange();
    return *this;
}

cMessagePar& cMessagePar::setRedirection(cMessagePar *par)
{
    if (isRedirected())
        return ind.par->setRedirection(par);

    if (!par)
        throw new cRuntimeError(this,eBADINIT,typeName('I'));

    // check for circular references
    cMessagePar *p = par;
    while (p)
    {
        if (p==this)
            throw new cRuntimeError(this,eCIRCREF);
        p = p->isRedirected() ? p->ind.par : NULL;
    }

    // set redirection
    beforeChange();
    deleteOld();
    ind.par = par; // do NOT take ownership of passed cMessagePar object
    typechar = 'I';
    inputflag=false;
    afterChange();
    return *this;
}

void cMessagePar::configPointer( VoidDelFunc delfunc, VoidDupFunc dupfunc,
                      size_t itemsize)
{
    if (typechar!='P')
        throw new cRuntimeError(this,"configPointer(): type is '%c'; should be 'P'",typechar);
    ptr.delfunc = delfunc;
    ptr.dupfunc = dupfunc;
    ptr.itemsize = itemsize;
}

//------------------------------------------------------------------------
// functions returning the stored value

const char *cMessagePar::stringValue()
{
    if (isRedirected())
        return ind.par->stringValue();

    if (isInput())
        read();
    if (typechar!='S')
        throw new cRuntimeError(this,eBADCAST,typeName(typechar),typeName('S'));
    return ss.sht ? ss.str : ls.str;
}

//
// Note:
// boolValue(), longValue() and doubleValue() are rather liberal: they all
// allow conversion from all of B,L and the double types D,T,X,F.
// Aesthetically, this is nasty a feature (because it is against the strongly
// typed nature of C++; you can do narrowing conversions [double->bool]
// without a warning, etc.), but it is necessary because of the way parameter
// assignments in NED are handled. For example, bool and long expressions
// (e.g. "i==0 || i==n") are compiled into cMessagePar X expressions which will _need_
// to be converted to bool and long after evaluation.
//

bool cMessagePar::boolValue()
{
    if (isRedirected())
        return ind.par->boolValue();

    if (isInput()) read();

    if (typechar=='B' || typechar=='L')
        return lng.val!=0;
    else if (isNumeric())
        return doubleValue()!=0;
    else
        throw new cRuntimeError(this,eBADCAST,typeName(typechar),typeName('B'));
}

inline long _double_to_long(double d)
{
    // gcc 3.3 "feature": if double d=0xc0000000, (long)d yields 0x80000000!!!
    // This causes trouble if we in fact want to cast this long to unsigned long, see NED_expr_2.test.
    // Workaround follows. Note: even the ul variable is needed: when inlining it, gcc will do the wrong cast!
    long l = (long)d;
    unsigned long ul = (unsigned long)d;
    return d<0 ? l : ul;
}

long cMessagePar::longValue()
{
    if (isRedirected())
        return ind.par->longValue();

    if (isInput()) read();
    if (typechar=='L' || typechar=='B')
        return lng.val;
    else if (isNumeric())
        return _double_to_long(doubleValue());
    else
        throw new cRuntimeError(this,eBADCAST,typeName(typechar),typeName('L'));
}

double cMessagePar::doubleValue()
{
    if (isRedirected())
        return ind.par->doubleValue();

    if (isInput()) read();
    if (typechar=='B' || typechar=='L')
        return (double)lng.val;
    else if (typechar=='D')
        return dbl.val;
    else if (typechar=='T')
        return fromstat();
    else if (typechar=='X')
        return evaluate();
    else if (typechar=='C')
        return cexpr.expr->evaluate();
    else if (typechar=='F')
        return func.argc==0 ? ((MathFuncNoArg)func.f)() :
               func.argc==1 ? ((MathFunc1Arg) func.f)(func.p1) :
               func.argc==2 ? ((MathFunc2Args)func.f)(func.p1,func.p2) :
               func.argc==3 ? ((MathFunc3Args)func.f)(func.p1,func.p2,func.p3) :
                              ((MathFunc4Args)func.f)(func.p1,func.p2,func.p3,func.p4);
    else
        throw new cRuntimeError(this,eBADCAST,typeName(typechar),typeName('D'));
}

void *cMessagePar::pointerValue()
{
    if (isRedirected())
        return ind.par->pointerValue();

    if (isInput()) read();
    if (typechar=='P')
        return ptr.ptr;
    else
        throw new cRuntimeError(this,eBADCAST,typeName(typechar),typeName('P'));
}

cObject *cMessagePar::objectValue()
{
    if (isRedirected())
        return ind.par->objectValue();

    if (isInput()) read();
    if (typechar=='O')
        return obj.obj;
    else
        throw new cRuntimeError(this,eBADCAST,typeName(typechar),typeName('O'));
}

cXMLElement *cMessagePar::xmlValue()
{
    if (isRedirected())
        return ind.par->xmlValue();

    if (isInput()) read();
    if (typechar=='M')
        return xmlp.node;
    else
        throw new cRuntimeError(this,eBADCAST,typeName(typechar),typeName('M'));
}

bool cMessagePar::isNumeric() const
{
    // returns true if it is safe to call doubleValue()/longValue()/boolValue()
    if (isRedirected())
        return ind.par->isNumeric();

    return typechar=='B' ||
           typechar=='L' ||
           typechar=='D' ||
           typechar=='T' ||
           typechar=='X' ||
           typechar=='C' ||
           typechar=='F';
}

bool cMessagePar::isConstant() const
{
    return typechar=='S' ||
           typechar=='B' ||
           typechar=='L' ||
           typechar=='D';
}

bool cMessagePar::equalsTo(cMessagePar *par)
{
    if (typechar != par->typechar)
        return false;

    switch (typechar) {
        case 'S': return strcmp(stringValue(),par->stringValue())==0;
        case 'B': return lng.val == par->lng.val;
        case 'L': return lng.val == par->lng.val;
        case 'D': return dbl.val == par->dbl.val;
        case 'F': if (func.f!=par->func.f) return 0;
                  switch(func.argc) {
                      case 4: if (func.p4!=par->func.p4) return 0; // no break
                      case 3: if (func.p3!=par->func.p3) return 0; // no break
                      case 2: if (func.p2!=par->func.p2) return 0; // no break
                      case 1: if (func.p1!=par->func.p1) return 0; // no break
                  }
                  return 1;
        case 'T': return dtr.res == par->dtr.res;
        case 'P': return ptr.ptr == par->ptr.ptr;
        case 'O': return obj.obj == par->obj.obj;
        case 'M': return xmlp.node == par->xmlp.node;
        case 'X': throw new cRuntimeError(this, "equalsTo() with X type not implemented");
        case 'C': throw new cRuntimeError(this, "equalsTo() with C type not implemented");
        default: return 0;
    }
}

//------------------------------------------------------------------------
// misc funcs

void cMessagePar::beforeChange()
{
}

void cMessagePar::afterChange()
{
    changedflag=true;
}

cMessagePar *cMessagePar::redirection()
{
    if (isRedirected())
        return ind.par;
    else
        return NULL;
}

void cMessagePar::cancelRedirection()
{
    if (isRedirected())
    {
        // operator=( *ind.par ); -- the user may do this by hand
        typechar = 'L'; lng.val = 0L;
    }
}

string cMessagePar::getAsText() const
{
    if (isRedirected())
        return ind.par->getAsText();

    char bb[128];
    bb[0] = 0;
    cMathFunction *ff;
    const char *fn;
    const char *s;

    switch (typechar)
    {
       case 'S': s = ls.sht ? ss.str:ls.str;
                 return string("\"")+s+"\"";
       case 'B': return string(lng.val?"true":"false");
       case 'L': sprintf(bb,"%ld",lng.val);
                 return string(bb);
       case 'D': sprintf(bb,"%g",dbl.val);
                 return string(bb);
       case 'F': ff = cMathFunction::findByPointer(func.f);
                 fn = ff ? ff->name() : "unknown";
                 switch(func.argc) {
                 case 0: sprintf(bb,"()"); break;
                 case 1: sprintf(bb,"(%g)",func.p1); break;
                 case 2: sprintf(bb,"(%g,%g)",func.p1,func.p2); break;
                 case 3: sprintf(bb,"(%g,%g,%g)",func.p1,func.p2,func.p3); break;
                 case 4: sprintf(bb,"(%g,%g,%g,%g)",func.p1,func.p2,func.p3,func.p4); break;
                 default: sprintf(bb,"() with %d args",func.argc); break;
                 };
                 return string(fn)+bb;
       case 'T': return string("distribution ")+(dtr.res?dtr.res->fullPath().c_str():"NULL");
       case 'P': sprintf(bb,"pointer %p", ptr.ptr); return string(bb);
       case 'O': return string("object ")+(obj.obj?obj.obj->fullPath().c_str():"NULL");
       case 'C': return string("compiled expression ")+cexpr.expr->getAsText();
       case 'X': return string("reverse Polish expression");
       case 'M': if (xmlp.node)
                     return string("<")+xmlp.node->getTagName()+"> from "+xmlp.node->getSourceLocation();
                 else
                     return string("NULL");
                 break;
       default : return string("???");
    }
}

/*-------------------------------
 parse() - (for internal use only) Tries to interpret text as
 a 'tp' typed value. tp=='?' means that even type is to be determined.
 On success, cMessagePar is updated and true is returned, otherwise
 it returns false. No error message is ever generated.
----------------------------*/

static bool parseQuotedString(string& str, const char *&s)
{
    while (*s==' ' || *s=='\t') s++;
    if (*s!='"') return false;
    const char *beg = ++s;
    while (*s && (*s!='"' || *(s-1)=='\\'))
        s++;
    if (*s!='"') return false;
    str.assign(beg, s-beg);
    s++;
    return true;
}

bool cMessagePar::setFromText(const char *text, char tp)
{
    tp = (char) toupper(tp);

    // create a working copy and cut whitespaces (from both sides)
    if (!text) return false;  // error: no string
    while (*text==' ' || *text=='\t') text++;
    if (*text=='\0') return false; // error: empty string (or only whitespace)
    char *tmp = opp_strdup(text);
    char *s = tmp+strlen(tmp)-1;
    while (s>=tmp && (*s==' ' || *s=='\t')) *s--='\0';

    double d;

    if (strcmp(tmp,"true")==0 || strcmp(tmp,"TRUE")==0 || strcmp(tmp,"True")==0) // bool?
    {
        if (!strchr("?B",tp)) goto error;
        setBoolValue(true);
    }
    else if (strcmp(tmp,"false")==0 || strcmp(tmp,"FALSE")==0 || strcmp(tmp,"False")==0) // bool?
    {
        if (!strchr("?B",tp)) goto error;
        setBoolValue(false);
    }
    else if (strcmp(tmp,"1")==0 && tp=='B') // bool?
    {
        setBoolValue(true);
    }
    else if (strcmp(tmp,"0")==0 && tp=='B') // bool?
    {
        setBoolValue(false);
    }
    else if (tmp[0]=='\'' && tmp[1] && tmp[2]=='\''&& !tmp[3]) // char? (->long)
    {
        if (!strchr("?L",tp)) goto error;
        setLongValue((long)tmp[1]);
    }
    else if (text[0]=='\"') // string?
    {
        if (!strchr("?S",tp)) goto error;

        // check closing quote
        if (!tmp[1] || tmp[strlen(tmp)-1]!='\"') goto error;

        tmp[strlen(tmp)-1] = '\0'; // cut off closing quote
        setStringValue(tmp+1);
    }
    else if (strspn(tmp,"+-0123456789")==strlen(tmp)) // long?
    {
        long num;
        int len;
        if (0==sscanf(tmp,"%ld%n",&num,&len)) goto error;
        if (len < (int)strlen(tmp) || !strchr("?LD",tp)) goto error;
        if (tp=='?' || tp=='L')
           setLongValue(num);
        else
           setDoubleValue(num);
    }
    else if (strspn(tmp,"+-.eE0123456789")==strlen(tmp)) // double?
    {
        double num;
        int len;
        if (0==sscanf(tmp,"%lf%n",&num,&len)) goto error;
        if (len < (int)strlen(tmp) || !strchr("?D",tp)) goto error;
        setDoubleValue(num);
    }
    else if ((d=strToSimtime(tmp))>=0.0) // double given as time?
    {
        if (!strchr("?D",tp)) goto error;
        setDoubleValue(d);
    }
    else if (!strncmp(tmp,"xmldoc",6))
    {
        if (!strchr("?M",tp)) goto error;

        // parse xmldoc("filename") or xmldoc("filename", "pathexpr")
        const char *s=tmp;
        s+=6;  // skip "xmldoc"
        while (*s==' ' || *s=='\t') s++;
        if (*s!='(') goto error;  // no "("
        s++; // skip "("
        std::string fname, pathexpr;
        while (*s==' ' || *s=='\t') s++;
        if (!parseQuotedString(fname, s)) goto error;
        while (*s==' ' || *s=='\t') s++;
        if (*s!=',' && *s!=')') goto error;  // no ")" or ","
        if (*s==',')
        {
            s++;  // skip ","
            if (!parseQuotedString(pathexpr, s)) goto error;
            while (*s==' ' || *s=='\t') s++;
            if (*s!=')') goto error;  // no ")"
        }
        s++; // skip ")"
        while (*s==' ' || *s=='\t') s++;
        if (*s) goto error;  // trailing rubbish

        cXMLElement *node = ev.getXMLDocument(fname.c_str(), pathexpr.empty() ? NULL : pathexpr.c_str());
        if (!node)
            throw new cRuntimeError(this,"%s: element not found", tmp);
        setXMLValue(node);
    }
    else // maybe function; try to parse it
    {
        if (!strchr("?F",tp)) goto error;
        if (!setfunction(tmp)) goto error;
    }

    delete [] tmp;
    return true;

    error:
    delete [] tmp;
    return false;
}

bool cMessagePar::setfunction(char *text)
{
    // Note: this function *will* alter its input string

    // find '('
    char *d;
    for (d=text; *d!='(' && *d!='\0'; d++);
    if (*d!='(') return false;  // no opening paren
    char *args = d;

    // remove whitespaces in-place
    const char *s;
    for (s=d=args; *s; s++)
       if (!isspace(*s))
          *d++ = *s;
    *d = '\0';

    // determine argccount: number of commas+1, or zero
    int commas = 0;
    for (d=args; *d; d++)
        if (*d==',')
            commas++;
    int argc;
    if (commas==0 && args[1]==')')
        argc = 0;
    else
        argc = commas+1;

    // look up function name (temporarily overwriting '(' with a '\0')
    *args = '\0';
    cMathFunction *ff = cMathFunction::find(text, argc);
    *args = '(';
    if (ff==NULL) return false;


    // now `args' points to something like '(10,1.5E-3)', without spaces
    s = args;
    double p1,p2,p3,p4;
    switch(ff->numArgs())
    {
       case 0: if (strcmp(s,"()")!=0) return false;
               setDoubleValue(ff->mathFuncNoArg());
               return true;
       case 1: if (*s++!='(') return false;
               p1 = strToSimtime0(s);
               if (*s++!=')') return false;
               setDoubleValue(ff->mathFunc1Arg(), p1);
               return true;
       case 2: if (*s++!='(') return false;
               p1 = strToSimtime0(s);
               if (*s++!=',') return false;
               p2 = strToSimtime0(s);
               if (*s++!=')') return false;
               setDoubleValue(ff->mathFunc2Args(), p1,p2);
               return true;
       case 3: if (*s++!='(') return false;
               p1 = strToSimtime0(s);
               if (*s++!=',') return false;
               p2 = strToSimtime0(s);
               if (*s++!=',') return false;
               p3 = strToSimtime0(s);
               if (*s++!=')') return false;
               setDoubleValue(ff->mathFunc3Args(), p1,p2,p3);
               return true;
       case 4: if (*s++!='(') return false;
               p1 = strToSimtime0(s);
               if (*s++!=',') return false;
               p2 = strToSimtime0(s);
               if (*s++!=',') return false;
               p3 = strToSimtime0(s);
               if (*s++!=',') return false;
               p4 = strToSimtime0(s);
               if (*s++!=')') return false;
               setDoubleValue(ff->mathFunc4Args(), p1,p2,p3,p4);
               return true;
       default:
               return false; // invalid argcount
    }
}


cMessagePar& cMessagePar::read()
{
    // get it from ini file
    std::string str = ev.getParameter(simulation.runNumber(), fullPath().c_str());
    if (!str.empty())
    {
        bool success = setFromText(str.c_str(),'?');
        if (!success)
            throw new cRuntimeError("Wrong value `%s' for parameter `%s'", str.c_str(), fullPath().c_str());
        return *this;
    }

    // maybe we should use default value
    bool useDefault = ev.getParameterUseDefault(simulation.runNumber(), fullPath().c_str());
    if (useDefault)
    {
        setInput(false);
        return *this;
    }

    // otherwise, we have to ask the user
    bool success;
    do {
        string reply;
        if (!promptstr.empty())
            reply = ev.gets(promptstr.c_str(), getAsText().c_str());
        else
            reply = ev.gets((string("Enter parameter `")+fullPath()+"':").c_str(), getAsText().c_str());

        try {
            success = false;
            success = setFromText(reply.c_str(),'?');
            if (!success)
                throw new cRuntimeError("Syntax error, please try again.");
        } catch (cException *e) {
            ev.printfmsg("%s", e->message());
            delete e;
        }
    } while (!success);
    return *this;
}


double cMessagePar::evaluate()
{
    const int stksize = 20;

      /*---------------------------------
         Type X (expression) - interprets the expression given
            in the ExprElem vector as a reversed Polish one and
            evaluates it.
         --------------------------*/
    if (typechar!='X')
         return 0.0;

    double stk[stksize];

    int tos = -1;

    for(int i=0; i<expr.n; i++) {
       ExprElem& e = expr.xelem[i];
       switch( toupper(e.type) ) {
           case 'D':
             if(tos>=stksize - 1) throw new cRuntimeError(this,eBADEXP);
             stk[++tos] = e.d;
             break;
           case 'P':
           case 'R':
             if(!e.p || tos>=stksize - 1) throw new cRuntimeError(this,eBADEXP);
             stk[++tos] = (double)(*e.p);
             break;
           case '0':
             if(!e.f0) throw new cRuntimeError(this,eBADEXP);
             stk[++tos] = e.f0();
             break;
           case '1':
             if(!e.f1 || tos<0) throw new cRuntimeError(this,eBADEXP);
             stk[tos] = e.f1( stk[tos] );
             break;
           case '2':
             if(!e.f2 || tos<1) throw new cRuntimeError(this,eBADEXP);
             stk[tos-1] = e.f2( stk[tos-1], stk[tos] );
             tos--;
             break;
           case '3':
             if(!e.f3 || tos<2) throw new cRuntimeError(this,eBADEXP);
             stk[tos-2] = e.f3( stk[tos-2], stk[tos-1], stk[tos] );
             tos-=2;
             break;
           case '4':
             if(!e.f4 || tos<3) throw new cRuntimeError(this,eBADEXP);
             stk[tos-3] = e.f4( stk[tos-3], stk[tos-2], stk[tos-1], stk[tos] );
             tos-=3;
             break;
           case '@':
             if(tos<1) throw new cRuntimeError(this,eBADEXP);
             switch(e.op) {
                case '+':
                   stk[tos-1] = stk[tos-1] + stk[tos];
                   tos--;
                   break;
                case '-':
                   stk[tos-1] = stk[tos-1] - stk[tos];
                   tos--;
                   break;
                case '*':
                   stk[tos-1] = stk[tos-1] * stk[tos];
                   tos--;
                   break;
                case '/':
                   stk[tos-1] = stk[tos-1] / stk[tos];
                   tos--;
                   break;
                case '%':
                   stk[tos-1] = (long)stk[tos-1] % (long)stk[tos];
                   tos--;
                   break;
                case '^':
                   stk[tos-1] = pow( stk[tos-1], stk[tos] );
                   tos--;
                   break;
                case '=':
                   stk[tos-1] = stk[tos-1] == stk[tos];
                   tos--;
                   break;
                case '!':
                   stk[tos-1] = stk[tos-1] != stk[tos];
                   tos--;
                   break;
                case '<':
                   stk[tos-1] = stk[tos-1] < stk[tos];
                   tos--;
                   break;
                case '{':
                   stk[tos-1] = stk[tos-1] <= stk[tos];
                   tos--;
                   break;
                case '>':
                   stk[tos-1] = stk[tos-1] > stk[tos];
                   tos--;
                   break;
                case '}':
                   stk[tos-1] = stk[tos-1] >= stk[tos];
                   tos--;
                   break;
                case '?':
                   stk[tos-2] = stk[tos-2]!=0.0 ? stk[tos-1] : stk[tos];
                   tos-=2;
                   break;
                default:
                   throw new cRuntimeError(this,eBADEXP);
                   return 0.0;
             }
             break;
           default:
             throw new cRuntimeError(this,eBADEXP);
       }
    }
    if (tos!=0)
        throw new cRuntimeError(this,eBADEXP);
    return stk[tos];
}

double cMessagePar::fromstat()
{
    if (typechar!='T')
        throw new cRuntimeError(this,eBADCAST,typeName(typechar),typeName('T'));
    return  dtr.res->random();
}


cMessagePar& cMessagePar::operator=(const cMessagePar& val)
{
    //
    // NOTE (duplicate of Doxygen comment in cpar.h):
    //
    // This function copies the 'val' object (whether it is of type 'I' or not)
    // into this object, *or*, if this is redirected, into the object this object
    // refers to.
    //

    if (this==&val) return *this;

    if (isRedirected())
        return ind.par->operator=(val);

    beforeChange();
    deleteOld();

    cObject::operator=( val );
    typechar = val.typechar;
    changedflag = val.changedflag;
    inputflag = val.inputflag;
    promptstr = val.promptstr;

    // this line is supposed to copy the whole data area.
    memcpy( &ss, &val.ss, Max(sizeof(ss), sizeof(func)) );

    if (typechar=='S' && !ss.sht)
    {
         // make our copy of the string
         ls.str = opp_strdup(ls.str);
    }
    else if (typechar=='C')
    {
         // make our copy of expression
         cexpr.expr = (cDoubleExpression *) cexpr.expr->dup();
    }
    else if (typechar=='X')
    {
         // play with ownership stuff
         memcpy( expr.xelem = new ExprElem[expr.n], val.expr.xelem,
                 expr.n*sizeof(ExprElem) );
         for(int i=0; i<expr.n; i++)
         {
             if (expr.xelem[i].type=='R')
             {
                // create a copy for ourselves
                cMessagePar *&p = expr.xelem[i].p;
                take( p=(cMessagePar *)p->dup() );
             }
         }
    }
    else if (typechar=='T')
    {
         cStatistic *&p = dtr.res;
         if (p->owner()==const_cast<cMessagePar*>(&val))
            take( p=(cStatistic *)p->dup() );
    }
    else if (typechar=='P')
    {
         if (ptr.dupfunc)
            ptr.ptr = ptr.dupfunc( ptr.ptr );
         else if (ptr.itemsize>0)
            memcpy(ptr.ptr=new char[ptr.itemsize],val.ptr.ptr,ptr.itemsize);
         // if no dupfunc or itemsize, only the pointer is copied
    }
    else if (typechar=='O')
    {
         cObject *&p = obj.obj;
         if (p->owner()==const_cast<cMessagePar*>(&val))
            take( p = (cObject *)p->dup() );
    }
    // type 'I' does not use ownership so we can skip it.

    afterChange();
    return *this;
}


int cMessagePar::cmpbyvalue(cObject *one, cObject *other)
{
    double x = (double)(*(cMessagePar*)one)-(double)(*(cMessagePar*)other);
    return sgn(x);
}

void cMessagePar::convertToConst ()
{
    if (isRedirected())
       cancelRedirection();

    if (strchr("FXTIC", typechar)) // if type is any or F,X,T,I,C...
    {
       double d = doubleValue();
       setDoubleValue(d);
    }
}


