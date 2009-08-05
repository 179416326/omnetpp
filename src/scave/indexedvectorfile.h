//=========================================================================
//  INDEXEDVECTORFILE.H - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef _INDEXEDVECTORFILE_H_
#define _INDEXEDVECTORFILE_H_

#include <assert.h>
#include <float.h>
#include <vector>
#include <stdarg.h>
#include "filereader.h"
#include "indexfile.h"
#include "node.h"
#include "nodetype.h"
#include "resultfilemanager.h"


struct OutputVectorEntry {
    long serial;
    long eventNumber;
    double simtime;
    double value;

    OutputVectorEntry()
        : eventNumber(-1) {}
    OutputVectorEntry(long serial, long eventNumber, double simtime, double value)
        : serial(serial), eventNumber(eventNumber), simtime(simtime), value(value) {}
};

typedef std::vector<OutputVectorEntry> Entries;

/**
 * Vector file reader with random access.
 * Each instance reads one vector from a vector file.
 */
class IndexedVectorFileReader
{
    std::string fname;  // file name of the vector file
    FileReader *reader; // reader of the vector file
    
    VectorFileIndex *index; // index of the vector file, loaded fully into the memory
    VectorData *vector;     // index data of the read vector, points into index
    Block *currentBlock;    // last loaded block, points into index
    OutputVectorEntry *currentEntries; // entries of the loaded block

    public:
        explicit IndexedVectorFileReader(const char* filename, long vectorId);
        ~IndexedVectorFileReader();
    protected:
        /** reads a block from the vector file */
        void loadBlock(Block &block);
    public:
        /**
         * Returns the number of entries in the vector.
         */
        int getNumberOfEntries() const { return vector->count(); };
        /**
         * Returns the entry with the specified serial,
         * or NULL if the serial is out of range.
         * The pointer will be valid until the next call to getEntryBySerial().
         */
        OutputVectorEntry *getEntryBySerial(long serial);
        /**
         * Adds output vector entries in the [startTime,endTime] interval to
         * the specified vector. Returns the number of entries added.
         */
        long collectEntriesInSimtimeInterval(double startTime, double endTime, Entries &out);
        /**
         * Adds output vector entries in the [startTime,endTime] interval to
         * the specified vector. Returns the number of entries added.
         */
        long collectEntriesInEventnumInterval(long startEventNum, long endEventNum, Entries &out);
};

/**
 * Consumer node which writes an indexed output vector file.
 */
class IndexedVectorFileWriterNode : public Node
{
    protected:
        class VectorInputPort : public Port {
            public:
            VectorData vector;
            char *buffer;     // buffer holding recorded data
            int bufferSize;  // size of the allocated buffer
            char *bufferPtr; // pointer to the current position in the buffer
            int bufferNumOfRecords; //

            VectorInputPort(int id, std::string moduleName, std::string name, std::string columns, int bufferSize, Node *owner)
                : Port(owner), vector(id, moduleName, name, columns, bufferSize) 
                { this->allocateBuffer(bufferSize); vector.blocks.push_back(Block()); }
            ~VectorInputPort() { if (buffer) delete[] buffer; }

            void allocateBuffer(int size) { buffer=new char[size]; bufferSize=size; clearBuffer(); }
            void clearBuffer() { bufferPtr = buffer; bufferNumOfRecords=0; if (buffer) buffer[0] = '\0'; }
            bool hasBufferedData() const { return bufferPtr!=buffer; }
        };

        typedef std::vector<VectorInputPort*> PortVector;

    private:
        PortVector ports;
        std::string fileHeader;
        std::string fileName;
        std::string indexFileName;
        int blockSize;
        FILE *f;
        IndexFileWriter *indexWriter;
        int prec;

    public:
        IndexedVectorFileWriterNode(const char *fileName, const char* indexFileName, int blockSize, const char *fileHeader=NULL);
        virtual ~IndexedVectorFileWriterNode();

        Port *addVector(const VectorResult &vector);
        void setPrecision(int prec) {this->prec = prec;}
        void setHeader(const std::string &header) { fileHeader = header; }
        std::string filename() const {return fileName;}

        virtual bool isReady() const;
        virtual void process();
        virtual bool finished() const;
    protected:
        void writeRecordsToBuffer(VectorInputPort *port);
        void writeBufferToFile(VectorInputPort *port);
        void writeIndex(VectorInputPort *port);
    private:
        void bufferPrintf(VectorInputPort *port, const char *format...);
};

class IndexedVectorFileWriterNodeType : public NodeType
{
    public:
        virtual const char *name() const {return "indexedvectorfilewriter";}
        virtual const char *category() const {return "multi-port sink";}
        virtual const char *description() const;
        virtual bool isHidden() const {return true;}
        virtual void getAttributes(StringMap& attrs) const;
        virtual Node *create(DataflowManager *mgr, StringMap& attrs) const;
        virtual Port *getPort(Node *node, const char *portname) const;
};

#endif
