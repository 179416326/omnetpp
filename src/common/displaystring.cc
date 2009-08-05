//==========================================================================
//   DISPLAYSTRING.CC  - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <string.h>
#include <stdio.h>
#include "displaystring.h"


//FIXME migrate (part of) sim/utils.cc here, and use these functions from there?

static char *opp_strdup(const char *s)
{
    if (!s || !s[0]) return NULL;
    char *p = new char[ strlen(s)+1 ];
    if (p) strcpy(p,s);
    return p;
}

static int opp_strcmp(const char *s1, const char *s2)
{
    // case-sensitive comparison
    if (s1 && s2)       return strcmp(s1,s2);
    else if (!s1 && s2) return -1;
    else if (s1 && !s2) return 1;
    else                return 0;
}

static int opp_strlen(const char *s)
{
    if (!s) return 0;
    return strlen(s);
}

inline const char *opp_nulltoempty(const char *s)
{
   return s ? s : "";
}

//---

DisplayString::DisplayString()
{
    dispstr = NULL;
    buffer = NULL;
    bufferend = NULL;
    tags = NULL;
    numtags = 0;
    needsassemble = false;
}


DisplayString::DisplayString(const char *displaystr)
{
    dispstr = opp_strdup(displaystr);
    buffer = NULL;
    bufferend = NULL;
    tags = NULL;
    numtags = 0;
    needsassemble = false;

    parse();
}

DisplayString::DisplayString(const DisplayString& ds)
{
    dispstr = NULL;
    buffer = NULL;
    bufferend = NULL;
    tags = NULL;
    numtags = 0;
    needsassemble = false;

    operator=(ds);
}

DisplayString::~DisplayString()
{
    delete [] dispstr;
    cleartags();
}

const char *DisplayString::toString() const
{
    if (needsassemble)
        assemble();
    return dispstr ? dispstr : "";
}


bool DisplayString::parse(const char *displaystr)
{
    // if it's the same, nothing to do
    if (needsassemble)
        assemble();
    if (!opp_strcmp(dispstr,displaystr))
        return true;

    // parse and store new string
    delete [] dispstr;
    cleartags();
    dispstr = opp_strdup(displaystr);
    bool fullyOK = parse();

    return fullyOK;
}


void DisplayString::updateWith(const DisplayString& ds)
{
    // elements in "ds" take precedence
    int n = ds.getNumTags();
    for (int i=0; i<n; i++)
    {
        int m = ds.getNumArgs(i);
        for (int j=0; j<m; j++)
        {
            const char *arg = ds.getTagArg(i,j);
            if (arg[0])
                setTagArg(ds.getTagName(i), j, arg);
        }
    }

    // optimize storage
    parse(toString());
}

void DisplayString::updateWith(const char *s)
{
    DisplayString ds(s);
    updateWith(ds);
}

bool DisplayString::existsTag(const char *tagname) const
{
    int t = gettagindex(tagname);
    return t!=-1;
}


int DisplayString::getNumArgs(const char *tagname) const
{
    return getNumArgs(gettagindex(tagname));
}


const char *DisplayString::getTagArg(const char *tagname, int index) const
{
    return getTagArg(gettagindex(tagname), index);
}


bool DisplayString::setTagArg(const char *tagname, int index, const char *value)
{
    int t = gettagindex(tagname);
    if (t==-1)
        t = insertTag(tagname);
    return setTagArg(t, index, value);
}

bool DisplayString::setTagArg(const char *tagname, int index, long value)
{
    char buf[32];
    sprintf(buf, "%ld", value);
    return setTagArg(tagname, index, buf);
}

bool DisplayString::removeTag(const char *tagname)
{
    return removeTag(gettagindex(tagname));
}


int DisplayString::getNumTags() const
{
    return numtags;
}


const char *DisplayString::getTagName(int tagindex) const
{
    if (tagindex<0 || tagindex>=numtags) return NULL;
    return tags[tagindex].name;
}


int DisplayString::getNumArgs(int tagindex) const
{
    if (tagindex<0 || tagindex>=numtags) return -1;
    return tags[tagindex].numargs;
}


const char *DisplayString::getTagArg(int tagindex, int index) const
{
    if (tagindex<0 || tagindex>=numtags) return "";
    if (index<0 || index>=tags[tagindex].numargs) return "";
    return opp_nulltoempty(tags[tagindex].args[index]);
}


bool DisplayString::setTagArg(int tagindex, int index, const char *value)
{
    // check indices
    if (tagindex<0 || tagindex>=numtags) return false;
    if (index<0 || index>=MAXARGS) return false;
    Tag& tag = tags[tagindex];

    // adjust numargs if necessary
    if (index>=tag.numargs)
        tag.numargs = index+1;

    // if it's the same, nothing to do
    char *&slot = tag.args[index];
    if (!opp_strcmp(slot,value))
        return true;

    // set value
    if (slot && !isinbuffer(slot))
        delete [] slot;
    slot = opp_strdup(value);

    // get rid of possible empty trailing args
    while (tag.numargs>0 && tag.args[tag.numargs-1]==NULL)
        tag.numargs--;

    return true;
}


int DisplayString::insertTag(const char *tagname, int atindex)
{
    // check uniqueness
    int t = gettagindex(tagname);
    if (t!=-1)
        return t;

    // check index
    if (atindex<0) atindex=0;
    if (atindex>numtags) atindex=numtags;

    // create new tags[] array with hole at atindex
    Tag *newtags = new Tag[numtags+1];
    for (int s=0,d=0; s<numtags; s++,d++)
    {
       if (d==atindex) d++; // make room for new item
       newtags[d] = tags[s];
    }
    delete [] tags;
    tags = newtags;
    numtags++;

    // fill in new tag
    tags[atindex].name = opp_strdup(tagname);
    tags[atindex].numargs = 0;
    for (int i=0; i<MAXARGS; i++) tags[atindex].args[i] = NULL;

    // success
    return atindex;
}


bool DisplayString::removeTag(int tagindex)
{
    if (tagindex<0 || tagindex>=numtags) return false;

    // dealloc strings in tag
    if (!isinbuffer(tags[tagindex].name))
        delete [] tags[tagindex].name;
    for (int i=0; i<tags[tagindex].numargs; i++)
        if (!isinbuffer(tags[tagindex].args[i]))
            delete [] tags[tagindex].args[i];

    // eliminate hole in tags[] array
    for (int t=tagindex; t<numtags-1; t++)
        tags[t] = tags[t+1];
    numtags--;

    // success
    return true;
}


int DisplayString::gettagindex(const char *tagname) const
{
    for (int t=0; t<numtags; t++)
        if (!strcmp(tagname,tags[t].name))
            return t;
    return -1;
}

void DisplayString::cleartags()
{
    // delete tags array. string pointers that do not point inside the
    // buffer were allocated individually via new char[] and have to be
    // deleted.
    for (int t=0; t<numtags; t++)
    {
        if (!isinbuffer(tags[t].name))
            delete [] tags[t].name;
        for (int i=0; i<tags[t].numargs; i++)
            if (!isinbuffer(tags[t].args[i]))
                delete [] tags[t].args[i];
    }
    delete [] tags;
    tags = NULL;
    numtags = 0;

    // must be done after deleting tags[] because of isinbuffer()
    delete [] buffer;
    buffer = bufferend = NULL;
}

inline int h2d(char c)
{
    if (c>='0' && c<='9') return c-'0';
    if (c>='A' && c<='F') return c-'A'+10;
    if (c>='a' && c<='f') return c-'a'+10;
    return -1;
}

inline int h2d(char *&s)
{
    int a = h2d(*s);
    if (a<0) return 0;
    s++;
    int b = h2d(*s);
    if (b<0) return a;
    s++;
    return a*16+b;
}

bool DisplayString::parse()
{
    cleartags();
    if (dispstr==NULL)
        return true;

    bool fully_ok = true;

    buffer = new char[opp_strlen(dispstr)+1];
    bufferend = buffer + opp_strlen(dispstr);

    // count tags (#(';')+1) & allocate tags[] array
    int n = 1;
    for (char *s1 = dispstr; *s1; s1++)
        if (*s1==';')
            n++;
    tags = new Tag[n];

    // parse string into tags[]. To avoid several small allocations,
    // pointers in tags[] will point into the buffer.
    numtags = 1;
    tags[0].name = buffer;
    tags[0].numargs = 0;
    for (int i=0; i<MAXARGS; i++)
        tags[0].args[i] = NULL;

    char *s, *d;
    for (s=dispstr,d=buffer; *s; s++,d++)
    {
        if (*s=='\\')
        {
            // allow backslash as quote character, also interpret backslash sequences
            s++;
            switch(*s)
            {
                case 'b': *d = '\b'; break;
                case 'f': *d = '\f'; break;
                case 'n': *d = '\n'; break;
                case 'r': *d = '\r'; break;
                case 't': *d = '\t'; break;
                case 'x': s++; *d = h2d(s); s--; break;
                case '\0': *d--; s--; break; // dispstr terminates in stray backslash
                default: *d = *s;
            }
        }
        else if (*s==';')
        {
            // new tag begins
            *d = '\0';
            numtags++;
            tags[numtags-1].name = d+1;
            tags[numtags-1].numargs = 0;
            for (int i=0; i<MAXARGS; i++) tags[numtags-1].args[i] = NULL;
        }
        else if (*s=='=')
        {
            // first argument of new tag begins
            *d = '\0';
            tags[numtags-1].numargs = 1;
            tags[numtags-1].args[0] = d+1;
        }
        else if (*s==',')
        {
            // new argument of current tag begins
            *d = '\0';
            if (tags[numtags-1].numargs>=MAXARGS)
            {
                fully_ok = false; // extra args ignored (there were too many)
            }
            else
            {
                tags[numtags-1].numargs++;
                tags[numtags-1].args[ tags[numtags-1].numargs-1 ] = d+1;
            }
        }
        else
        {
            *d = *s;
        }
    }
    *d = '\0';

    return fully_ok;
}

void DisplayString::assemble() const
{
    // calculate length of display string
    int size = 0;
    for (int t0=0; t0<numtags; t0++)
    {
        size += opp_strlen(tags[t0].name)+2;
        for (int i=0; i<tags[t0].numargs; i++)
            size += opp_strlen(tags[t0].args[i])+1;
    }
    size = 2*size+1;  // 2* for worst case if every char has to be escaped

    // allocate display string
    delete [] dispstr;
    dispstr = new char[size];
    dispstr[0] = '\0';

    // assemble string
    for (int t=0; t<numtags; t++)
    {
        if (t!=0)
            strcat(dispstr, ";");
        strcatescaped(dispstr, tags[t].name);
        if (tags[t].numargs>0)
            strcat(dispstr, "=");
        for (int i=0; i<tags[t].numargs; i++)
        {
            if (i!=0) strcat(dispstr, ",");
            strcatescaped(dispstr, tags[t].args[i]);
        }
    }
    needsassemble = false;
}

void DisplayString::strcatescaped(char *d, const char *s)
{
    if (!s) return;

    d += strlen(d);
    while (*s)
    {
        if (*s=='\b')      {*d++='\\'; *d++='b'; s++;}
        else if (*s=='\f') {*d++='\\'; *d++='f'; s++;}
        else if (*s=='\n') {*d++='\\'; *d++='n'; s++;}
        else if (*s=='\r') {*d++='\\'; *d++='r'; s++;}
        else if (*s=='\t') {*d++='\\'; *d++='t'; s++;}
        else if (*s<32)    {*d++='\\'; *d++='x'; sprintf(d,"%2.2X",*s++); d+=2;}
        else if (*s==';' || *s==',' || *s=='=' || *s=='\\') {*d++ = '\\'; *d++=*s++;}
        else *d++ = *s++;
    }
    *d = '\0';
}


