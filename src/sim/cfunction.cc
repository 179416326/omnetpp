//=========================================================================
//  CFUNCTION.CC - part of
//
//                    OMNeT++/OMNEST
//             Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <math.h>
#include <string.h>
#include <stdarg.h>
#include "cfunction.h"
#include "globals.h"
#include "cenvir.h"
#include "cexception.h"
#include "onstartup.h"
#include "carray.h"

#ifdef WITH_PARSIM
#include "ccommbuffer.h"
#include "parsim/cplaceholdermod.h"
#endif

//=== Functions  (register them for findFunction())
Define_Function( acos,  1 )
Define_Function( asin,  1 )
Define_Function( atan,  1 )
Define_Function( atan2, 2 )

Define_Function( sin,  1 )
Define_Function( cos,  1 )
Define_Function( tan,  1 )

Define_Function( ceil,  1 )
Define_Function( floor, 1 )

Define_Function( exp,  1 )
Define_Function( pow,  2 )
Define_Function( sqrt, 1 )

Define_Function( fabs, 1 )
Define_Function( fmod, 2 )

Define_Function( hypot, 2 )

Define_Function( log,   1 )
Define_Function( log10, 1 )


cMathFunction::cMathFunction(const char *name, MathFuncNoArg f, int ac) : cNoncopyableObject(name)
{
    this->f = (MathFunc)f;
    argc = 0;
    if (ac!=-1 && ac!=0)
        throw new cRuntimeError("Register_Function() or cMathFunction: "
                                "attempt to register function \"%s\" with wrong "
                                "number of arguments %d, should be 0", name, ac);
}

cMathFunction::cMathFunction(const char *name, MathFunc1Arg f, int ac) : cNoncopyableObject(name)
{
    this->f = (MathFunc)f;
    argc = 1;
    if (ac!=-1 && ac!=1)
        throw new cRuntimeError("Register_Function() or cMathFunction: "
                                "attempt to register function \"%s\" with wrong "
                                "number of arguments %d, should be 1", name, ac);
}

cMathFunction::cMathFunction(const char *name, MathFunc2Args f, int ac) : cNoncopyableObject(name)
{
    this->f = (MathFunc)f;
    argc = 2;
    if (ac!=-1 && ac!=2)
        throw new cRuntimeError("Register_Function() or cMathFunction: "
                                "attempt to register function \"%s\" with wrong "
                                "number of arguments %d, should be 2", name, ac);
}

cMathFunction::cMathFunction(const char *name, MathFunc3Args f, int ac) : cNoncopyableObject(name)
{
    this->f = (MathFunc)f;
    argc = 3;
    if (ac!=-1 && ac!=3)
        throw new cRuntimeError("Register_Function() or cMathFunction: "
                                "attempt to register function \"%s\" with wrong "
                                "number of arguments %d, should be 3", name, ac);
}

cMathFunction::cMathFunction(const char *name, MathFunc4Args f, int ac) : cNoncopyableObject(name)
{
    this->f = (MathFunc)f;
    argc = 4;
    if (ac!=-1 && ac!=4)
        throw new cRuntimeError("Register_Function() or cMathFunction: "
                                "attempt to register function \"%s\" with wrong "
                                "number of arguments %d, should be 4", name, ac);
}

std::string cMathFunction::info() const
{
    std::stringstream out;
    out << "(";
    for (int i=0; i<argc; i++)
        out << (i?",":"") << "double";
    out << ") -> double";
    return out.str();
}

MathFuncNoArg cMathFunction::mathFuncNoArg()
{
    if (argc!=0)
        throw new cRuntimeError(this,"mathFuncNoArg(): arg count mismatch (argc=%d)",argc);
    return (MathFuncNoArg)f;
}

MathFunc1Arg cMathFunction::mathFunc1Arg()
{
    if (argc!=1)
        throw new cRuntimeError(this,"mathFunc1Arg(): arg count mismatch (argc=%d)",argc);
    return (MathFunc1Arg)f;
}

MathFunc2Args cMathFunction::mathFunc2Args()
{
    if (argc!=2)
        throw new cRuntimeError(this,"mathFunc2Args(): arg count mismatch (argc=%d)",argc);
    return (MathFunc2Args)f;
}

MathFunc3Args cMathFunction::mathFunc3Args()
{
    if (argc!=3)
        throw new cRuntimeError(this,"mathFunc3Args(): arg count mismatch (argc=%d)",argc);
    return (MathFunc3Args)f;
}

MathFunc4Args cMathFunction::mathFunc4Args()
{
    if (argc!=4)
        throw new cRuntimeError(this,"mathFunc4Args(): arg count mismatch (argc=%d)",argc);
    return (MathFunc4Args)f;
}

cMathFunction *cMathFunction::find(const char *name, int argcount)
{
    cArray *a = nedFunctions.instance();
    for (int i=0; i<a->items(); i++)
    {
        cMathFunction *f = dynamic_cast<cMathFunction *>(a->get(i));
        if (f && f->isName(name) && f->numArgs()==argcount)
            return f;
    }
    return NULL;
}

cMathFunction *cMathFunction::findByPointer(MathFunc f)
{
    cArray *a = nedFunctions.instance();
    for (int i=0; i<a->items(); i++)
    {
        cMathFunction *ff = dynamic_cast<cMathFunction *>(a->get(i));
        if (ff && ff->mathFunc() == f)
            return ff;
    }
    return NULL;
}


