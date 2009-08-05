//=========================================================================
//  OCTAVEEXPORT.CC - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2006 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifdef _MSC_VER
#pragma warning(disable:4786)
#endif

#include "octaveexport.h"

#ifdef CHECK
#undef CHECK
#endif
#define CHECK(fprintf)    if (fprintf<0) throw new Exception("Cannot write file `%s'", fileName.c_str())


OctaveExport::OctaveExport(const char *fileName)
{
    f = NULL;
    this->prec = DEFAULT_PRECISION;
    this->fileName = fileName;
}

OctaveExport::~OctaveExport()
{
    close();
}

void OctaveExport::openFileIfNeeded()
{
    if (!f)
    {
        f = fopen(fileName.c_str(), "w");
        if (!f)
            throw new Exception("cannot open `%s' for write", fileName.c_str());

        // print file header
        CHECK(fprintf(f,"# Created by OMNeT++/OMNEST scavetool\n"));
    }
}

void OctaveExport::close()
{
    if (f)
    {
        // close the file
        fclose(f);
        f = NULL;
    }
}

std::string OctaveExport::makeUniqueName(const char *name)
{
    // check if it's already unique
    std::set<std::string>::const_iterator it = savedVars.find(name);
    if (it == savedVars.end())
        return name;

    // try appending "_1", "_2", etc until it becomes unique
    for (int i=1; i>0; i++)
    {
        char buf[32];
        sprintf(buf,"_%d", i);
        std::string newName = name;
        newName += buf;

        std::set<std::string>::const_iterator it = savedVars.find(newName);
        if (it == savedVars.end())
            return newName;
    }
    throw new Exception("banged head against the sky");
}

void OctaveExport::writeMatrixHeader(const char *name, int rows, int columns)
{
    savedVars.insert(name);

    CHECK(fprintf(f,"# name: %s\n"
                    "# type: matrix\n"
                    "# rows: %d\n"
                    "# columns: %d\n",
                    name, rows, columns));
}

void OctaveExport::writeString(const char *name, const char *value)
{
    CHECK(fprintf(f,"# name: %s\n"
                    "# type: string\n"
                    "# elements: 1\n"
                    "# length: %d\n",
                    name, strlen(value)));
    CHECK(fprintf(f,"%s\n", value));
}

void OctaveExport::writeDescription(const char *name, const char *description)
{
    writeString((std::string(name)+"_descr").c_str(), description);
}

void OctaveExport::saveVector(const char *name, const char *description,
                              const XYArray *vec, int startIndex, int endIndex)
{
    // write header
    openFileIfNeeded();
    if (description)
        writeDescription(name, description);
    if (endIndex==-1)
        endIndex = vec->length();
    writeMatrixHeader(name, endIndex-startIndex, 2);

    // write data
    for (int i=startIndex; i<endIndex; i++)
        CHECK(fprintf(f," %.*g %.*g\n", prec, vec->getX(i), prec, vec->getY(i)));
}

void OctaveExport::saveVectorX(const char *name, const char *description,
                               const XYArray *vec, int startIndex, int endIndex)
{
    // write header
    openFileIfNeeded();
    if (description)
        writeDescription(name, description);
    if (endIndex==-1)
        endIndex = vec->length();
    writeMatrixHeader(name, endIndex-startIndex, 1);

    // write data
    for (int i=startIndex; i<endIndex; i++)
        CHECK(fprintf(f," %.*g\n", prec, vec->getX(i)));
}

void OctaveExport::saveVectorY(const char *name, const char *description,
                               const XYArray *vec, int startIndex, int endIndex)
{
    // write header
    openFileIfNeeded();
    if (description)
        writeDescription(name, description);
    if (endIndex==-1)
        endIndex = vec->length();
    writeMatrixHeader(name, endIndex-startIndex, 1);

    // write data
    for (int i=startIndex; i<endIndex; i++)
        CHECK(fprintf(f," %.*g\n", prec, vec->getY(i)));
}

