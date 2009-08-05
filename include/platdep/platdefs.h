//==========================================================================
//  PLATDEFS.H - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//
//  Compiler and platform related checks and defines
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __PLATDEFS_H
#define __PLATDEFS_H

#include <stddef.h>

#ifndef __WIN32__
#  if defined(_WIN32) || defined(WIN32)
#    define __WIN32__
#  endif
#endif

// OPP_DLLIMPORT/EXPORT are empty if not needed
#if defined(__WIN32__)
#  define OPP_DLLEXPORT  __declspec(dllexport)
#  if defined(WIN32_DLL)
#    define OPP_DLLIMPORT  __declspec(dllimport)
#  else
#    define OPP_DLLIMPORT
#  endif
#else
#  define OPP_DLLIMPORT
#  define OPP_DLLEXPORT
#endif

// check VC++ version (6.0 is no longer accepted)
#ifdef _MSC_VER
#if _MSC_VER<1300
#error "OMNEST/OMNeT++ cannot be compiled with Visual C++ 6.0 or earlier, at least version 7.0 required"
#endif
#endif

#ifdef _MSC_VER
#pragma warning(disable:4800)  // disable "forcing int to bool"
#pragma warning(disable:4996)  // disable VC8.0 warnings on Unix syscalls
#endif

// gcc 2.9x.x had broken exception handling
#ifdef __GNUC__
#  if  __GNUC__<3
#    error gcc 3.x or later required -- please upgrade
#  endif
#endif

#if defined __GNUC__ && __GNUC__>=4
#define _MAYBEUNUSED   __attribute((__unused__))
#define _OPPDEPRECATED __attribute((__deprecated__))
#else
#define _MAYBEUNUSED
#define _OPPDEPRECATED
#endif

#endif

