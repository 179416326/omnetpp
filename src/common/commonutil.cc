//=========================================================================
//  COMMONUTIL.CC - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//  Author: Andras Varga
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <stdio.h>
#include <stdarg.h>
#include <stdlib.h>
#include <locale.h>
#include "platmisc.h"
#include "commonutil.h"
#ifndef _WIN32
#include <unistd.h>
#endif

USING_NAMESPACE


static double zero = 0.0;
double const NaN = zero / zero;
double const POSITIVE_INFINITY = 1.0 / zero;
double const NEGATIVE_INFINITY = -1.0 / zero;

//----

void setPosixLocale()
{
    setlocale(LC_ALL, "C");
}

//----

const char *opp_gethostname()
{
#ifdef _WIN32
    // Windows also has gethostname(), but we don't want to pull in winsock2 just for that
    return getenv("COMPUTERNAME");
#else
    static char buf[128];
    if (gethostname(buf, sizeof(buf)) == 0)
        return buf;
    else if (getenv("HOST") != NULL)
        return getenv("HOST");
    else
        return getenv("HOSTNAME");
#endif
}

//----

int CallTracer::depth;

CallTracer::CallTracer(const char *fmt,...)
{
    char buf[1024];
    VSNPRINTF(buf, 1024, fmt);
    funcname = buf;
    std::cout << std::setw(depth++*2) << "" << " ++ " << funcname << std::endl;
}

CallTracer::~CallTracer()
{
    std::cout << std::setw(--depth*2) << "" << " -- " << funcname;
    if (!result.empty())
        std::cout << ", result: " << result;
    std::cout << std::endl;
}

void CallTracer::printf(const char *fmt, ...)
{
    char buf[1024];
    VSNPRINTF(buf, 1024, fmt);
    std::cout << std::setw(depth*2) << "" << "    " << buf << std::endl;
}

//----

#ifdef _MSC_VER
// source: http://en.wikipedia.org/wiki/RDTSC
__declspec(naked)
uint64 __cdecl readCPUTimeStampCounter()
{
   __asm
   {
      rdtsc
      ret       ; return value at EDX:EAX
   }
}
#else
uint64 readCPUTimeStampCounter()
{
    return 0;
}
#endif


