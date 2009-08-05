%module ScaveEngine

%include "loadlib.i"

%{
#include "scavedefs.h"
#include "enumtype.h"
#include "idlist.h"
#include "resultfilemanager.h"
#include "datasorter.h"
#include "stringutil.h"   // strdictcmp
#include "indexfile.h"
#include "indexedvectorfile.h"
#include "vectorfileindexer.h"
#include "vectorfilereader.h"
#include "scaveexception.h"
#include "export.h"
%}

%include "commondefs.i"

%exception {
    try {
        $action
    } catch (std::exception& e) {
        SWIG_JavaThrowException(jenv, SWIG_JavaRuntimeException, const_cast<char*>(e.what()));
        return $null;
    }
}

// hide export/import macros from swig
#define COMMON_API
#define OPP_DLLEXPORT
#define OPP_DLLIMPORT

%include "inttypes.h"
%include "scavedefs.h"

%typemap(jni) ID "jlong";

COMMON_ENGINE_BIGDECIMAL();

%include "std_common.i"
%include "std_string.i"
%include "std_set.i"     // our custom version
%include "std_list.i"    // our custom version
%include "std_vector.i"
%include "std_map.i"

namespace std {
   %typemap(javacode) vector<string> %{
       public String[] toArray() {
           int sz = (int) size();
           String[] array = new String[sz];
           for (int i=0; i<sz; i++)
               array[i] = get(i);
           return array;
       }
       public static StringVector fromArray(String[] array) {
           StringVector vector = new StringVector();
           for (int i=0; i<array.length; i++)
               vector.add(array[i]);
           return vector;
       }

   %}

   %typemap(javacode) vector<Run*> %{
        public Run[] toArray() {
            int sz = (int)size();
            Run[] array = new Run[sz];
            for (int i=0; i<sz; i++)
                array[i] = get(i);
            return array;
        }
   %}

   %typemap(javacode) vector<ResultFile*> %{
        public ResultFile[] toArray() {
            int sz = (int)size();
            ResultFile[] array = new ResultFile[sz];
            for (int i=0; i<sz; i++)
                array[i] = get(i);
            return array;
        }
   %}

   %typemap(javacode) vector<FileRun*> %{
        public FileRun[] toArray() {
            int sz = (int)size();
            FileRun[] array = new FileRun[sz];
            for (int i=0; i<sz; i++)
                array[i] = get(i);
            return array;
        }
   %}

   %typemap(javacode) vector<ID> %{
        public Long[] toArray() {
            int sz = (int)size();
            Long[] array = new Long[sz];
            for (int i=0; i<sz; i++)
                array[i] = Long.valueOf(get(i));
            return array;
        }
   %}

   %extend set<string> {
       std::vector<std::string> keys() {
           std::vector<std::string> vec;
           vec.reserve(self->size());
           for (std::set<std::string>::iterator it = self->begin(); it!=self->end(); it++)
               vec.push_back(*it);
           return vec;
       }
   }

   %extend map<string,string> {
       std::vector<std::string> keys() {
           std::vector<std::string> vec;
           vec.reserve(self->size());
           for (std::map<std::string,std::string>::iterator it = self->begin(); it!=self->end(); it++)
               vec.push_back(it->first);
           return vec;
       }
   }

   %template(StringSet) set<string>;
   %template(StringVector) vector<string>;

   //specialize_std_map_on_both(string,,,,string,,,);

   //%template(StringMap) map<string,string>;

   specialize_std_vector(ID);

   %template(IDVector) vector<ID>;
   %template(IDVectorVector) vector<vector<ID> >;

   specialize_std_vector(Run*);
   specialize_std_vector(ResultFile*);
   specialize_std_vector(FileRun*);

   %template(RunList) vector<Run*>;
   %template(ResultFileList) vector<ResultFile*>;
   %template(FileRunList) vector<FileRun*>;

   specialize_std_vector(const char *);

   %template(IntSet) set<int>;

   specialize_std_map_on_both(int,,,,int,,,);

   %template(IntIntMap) map<int,int>;

   %template(IntVector) vector<int>;

};

%ignore EnumType::insert;
%ignore EnumType::parseFromString;
%include "enumtype.h"


%typemap(javacode) IDList %{
    public static final IDList EMPTY = new IDList();

    public void swigDisown() {
        swigCMemOwn = false;
    }
    public Long[] toArray() {
        int sz = (int) size();
        Long[] array = new Long[sz];
        for (int i=0; i<sz; i++)
            array[i] = Long.valueOf(get(i));
        return array;
    }
    public static IDList fromArray(Long[] array) {
        IDList list = new IDList();
        for (int i=0; i<array.length; i++)
            list.add(array[i].longValue());
        return list;
    }
%}

//
// Add polymorphic return type to ResultFileManager::getItem(),
// because plain ResultItem does not contain the type (VECTOR, SCALAR, etc).
//
%rename(_getItem) ResultFileManager::getItem;
%javamethodmodifiers ResultFileManager::_getItem "protected";
%typemap(javacode) ResultFileManager %{
    public ResultItem getItem(long id) {
        int type = getTypeOf(id);
        if (type==SCALAR)
            return getScalar(id);
        else if (type==VECTOR)
            return getVector(id);
        else if (type==HISTOGRAM)
            return getHistogram(id);
        else
            throw new RuntimeException("unknown ID type");
    }
%}

//
// The following code is for IDList::getSubsetByIndices():
//
%typemap(in) (int *array, int n) {
    $2 = jenv->GetArrayLength($input);
    jint *a = jenv->GetIntArrayElements($input, 0);
    $1 = new int[$2];
    for (int i=0; i<$2; i++)  $1[i] = a[i];
    jenv->ReleaseIntArrayElements($input, a, 0);
}

%typemap(freearg) (int *array, int n) {
    delete [] $1;
}

%typemap(jni)    (int *array, int n) "jintArray"
%typemap(jtype)  (int *array, int n) "int[]"
%typemap(jstype) (int *array, int n) "int[]"
%typemap(javain) (int *array, int n) "$javainput"

//
// The following code is for IDList::toByteArray()/fromByteArray()
//
%typemap(in) (char *array, int n) {
    $2 = jenv->GetArrayLength($input);
    $1 = (char *) jenv->GetByteArrayElements($input, 0);
}

%typemap(freearg) (char *array, int n) {
    jenv->ReleaseByteArrayElements($input, (jbyte *)$1, 0);
}

%typemap(jni)    (char *array, int n) "jbyteArray"
%typemap(jtype)  (char *array, int n) "byte[]"
%typemap(jstype) (char *array, int n) "byte[]"
%typemap(javain) (char *array, int n) "$javainput"


// FIXME add %newobject where needed!

%ignore ResultFileManager::dump;

//
// By default, string members get wrapped with get/set using *pointers* to strings.
// This is not too nice, so override it by defining proper getter/setter methods,
// and hiding (ignoring) the class member
//
%define FIX_STRING_MEMBER(STRUCT,MEMBER,CAPITALIZEDMEMBER)
%ignore STRUCT::MEMBER;
%extend STRUCT {      
   std::string get ## CAPITALIZEDMEMBER() {return self->MEMBER;}
   void set ## CAPITALIZEDMEMBER(std::string __a) {self->MEMBER = __a;}
}
%enddef

%define FIX_CHARPTR_MEMBER(STRUCT,MEMBER,CAPITALIZEDMEMBER)
%ignore STRUCT::MEMBER;
%extend STRUCT {
   const char * get ## CAPITALIZEDMEMBER() {return self->MEMBER;}
}
%enddef

FIX_STRING_MEMBER(ResultFile, filePath, FilePath);
FIX_STRING_MEMBER(ResultFile, directory, Directory);
FIX_STRING_MEMBER(ResultFile, fileName, FileName);
FIX_STRING_MEMBER(ResultFile, fileSystemFilePath, FileSystemFilePath);

//FIX_STRING_MEMBER(Run, networkName, NetworkName);
//FIX_STRING_MEMBER(Run, date, Date);
FIX_STRING_MEMBER(Run, runName, RunName);
//FIX_STRING_MEMBER(Run, fileAndRunName, FileAndRunName);
//FIX_STRING_MEMBER(Run, experimentName, ExperimentName);
//FIX_STRING_MEMBER(Run, measurementName, MeasurementName);
//FIX_STRING_MEMBER(Run, replicationName, ReplicationName);
FIX_STRING_MEMBER(VectorResult, columns, Columns);

%rename FileRun::fileRef file;
%rename FileRun::runRef run;
%rename ResultItem::fileRunRef fileRun;

%ignore ResultItem::moduleNameRef;
%ignore ResultItem::nameRef;
%extend ResultItem {
   std::string getModuleName() {return *self->moduleNameRef;}
   std::string getName() {return *self->nameRef;}
}

%ignore ResultFile::id;
%ignore ResultFile::scalarResults;
%ignore ResultFile::vectorResults;

%typemap(javacode) ResultFile %{
    public boolean equals(Object obj) {
        return (obj instanceof ResultFile) && getCPtr(this)==getCPtr((ResultFile)obj);
    }
    public int hashCode() {
        return (int)getCPtr(this);
    }
%}

%typemap(javacode) Run %{
    public boolean equals(Object obj) {
        return (obj instanceof Run) && getCPtr(this)==getCPtr((Run)obj);
    }
    public int hashCode() {
        return (int)getCPtr(this);
    }
%}

%typemap(javacode) FileRun %{
    public boolean equals(Object obj) {
        return (obj instanceof FileRun) && getCPtr(this)==getCPtr((FileRun)obj);
    }
    public int hashCode() {
        return (int)getCPtr(this);
    }
%}

%typemap(javacode) OutputVectorEntry %{
   public boolean equals(Object obj) {
      return (obj instanceof OutputVectorEntry) && getSerial() == ((OutputVectorEntry)obj).getSerial();
   }

   public int hashCode() {
      return (int)getCPtr(this);
   }
%}

// Java doesn't appear to have dictionary sort, export it
int strdictcmp(const char *s1, const char *s2);

/* Let's just grab the original header file here */
%include "idlist.h"
%include "resultfilemanager.h"
%include "datasorter.h"

// wrap the data-flow engine as well
%include scave-plove.i

/* ------------- indexedvectorfile.h  ----------------- */
// %include "indexfile.h"
class IndexFile
{
    public:
        static bool isIndexFile(const char *indexFileName);
        static bool isVectorFile(const char *vectorFileName);
        static std::string getIndexFileName(const char *vectorFileName);
        static std::string getVectorFileName(const char *indexFileName);
        static bool isIndexFileUpToDate(const char *fileName);
};

/* ------------- vectorfileindexer.h  ----------------- */

%exception VectorFileIndexer::generateIndex {
    try {
        $action
    } catch (ResultFileFormatException& e) {
	jclass clazz = jenv->FindClass("org/omnetpp/scave/engineext/ResultFileFormatException");
	jmethodID methodId = jenv->GetMethodID(clazz, "<init>", "(Ljava/lang/String;Ljava/lang/String;I)V");
	jthrowable exception = (jthrowable)(jenv->NewObject(clazz, methodId, jenv->NewStringUTF(e.what()), jenv->NewStringUTF(e.getFileName()), e.getLine()));
        jenv->Throw(exception);
        return $null;
    } catch (std::exception& e) {
        SWIG_JavaThrowException(jenv, SWIG_JavaRuntimeException, const_cast<char*>(e.what()));
        return $null;
    }
}

%include "vectorfileindexer.h"


/* ------------- indexedvectorfile.h  ----------------- */

namespace std {
  specialize_std_vector(OutputVectorEntry);
  %template(EntryVector) vector<OutputVectorEntry>;
};
%ignore IndexedVectorFileWriterNode;
%ignore IndexedVectorFileWriterNodeType;
%include "indexedvectorfile.h"

%extend VectorFileReaderNode {
	static VectorFileReaderNode *cast(Node* node) { return dynamic_cast<VectorFileReaderNode*>(node); }
};

%ignore VectorFileReaderNodeType;
%include "vectorfilereader.h"

/* ------------------ datasorter.h --------------------- */
%include "datasorter.h"

/* ------------------ export.h ----------------------- */
%ignore DataTable;
%ignore XYDataTable;
%ignore ScalarDataTable;
%ignore MatlabStructExport;
%ignore MatlabScriptExport;
%ignore OctaveTextExport;
%rename(EOL)	CsvExport::eol;

%include "export.h"
