//=========================================================================
//  FILEWRITER.H - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef _FILEWRITER_H_
#define _FILEWRITER_H_

#include <string>
#include "commonnodes.h"


/**
 * Processing node which writes data into a file. Every line contains two
 * real numbers (time and value) separated by a tab.
 */
class SCAVE_API FileWriterNode : public SingleSinkNode
{
    private:
        std::string fileName;
        std::string banner;
        FILE *f;
        int prec;
    public:
        FileWriterNode(const char *filename, const char *banner=NULL);
        virtual ~FileWriterNode();
        void setPrecision(int prec) {this->prec = prec;}
        virtual bool isReady() const;
        virtual void process();
        virtual bool finished() const;
};

class SCAVE_API FileWriterNodeType : public SingleSinkNodeType
{
    public:
        virtual const char *name() const {return "filewriter";}
        virtual const char *description() const;
        virtual void getAttributes(StringMap& attrs) const;
        virtual Node *create(DataflowManager *mgr, StringMap& attrs) const;
};


#endif


