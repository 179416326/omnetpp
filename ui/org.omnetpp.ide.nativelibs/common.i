%module Common

// covariant return type warning disabled
#pragma SWIG nowarn=822

%{
#include "stringutil.h"
#include "patternmatcher.h"
#include "unitconversion.h"
#include "bigdecimal.h"
#include "rwlock.h"
#include "expression.h"
%}

%include "commondefs.i"
%include "loadlib.i"
%include "std_string.i"
%include "std_vector.i"

namespace std {
   %template(StringVector) vector<string>;
   %template(PStringVector) vector<const char *>;
}

#define THREADED

// hide export/import macros from swig
#define COMMON_API
#define OPP_DLLEXPORT
#define OPP_DLLIMPORT

#define NAMESPACE_BEGIN
#define NAMESPACE_END
#define USING_NAMESPACE

%rename(parseQuotedString)   ::opp_parsequotedstr;
%rename(quoteString)         ::opp_quotestr;
%rename(needsQuotes)         ::opp_needsquotes;
%rename(quoteStringIfNeeded) ::opp_quotestr_ifneeded;

std::string opp_parsequotedstr(const char *txt);
std::string opp_quotestr(const char *txt);
bool opp_needsquotes(const char *txt);
std::string opp_quotestr_ifneeded(const char *txt);
int strdictcmp(const char *s1, const char *s2);

%ignore UnitConversion::parseQuantity(const char *, std::string&);

%include "patternmatcher.h"
%include "unitconversion.h"


/* -------------------- BigDecimal -------------------------- */


%ignore _I64_MAX_DIGITS;
%ignore BigDecimal::BigDecimal();
%ignore BigDecimal::str(char*);
%ignore BigDecimal::parse(const char*,const char*&);
%ignore BigDecimal::ttoa;
%ignore BigDecimal::Nil;
%ignore BigDecimal::isNil;
%ignore BigDecimal::operator=;
%ignore BigDecimal::operator+=;
%ignore BigDecimal::operator-=;
%ignore BigDecimal::operator*=;
%ignore BigDecimal::operator/=;
%ignore BigDecimal::operator!=;
%ignore operator+;
%ignore operator-;
%ignore operator*;
%ignore operator/;
%ignore operator<<;
%immutable BigDecimal::Zero;
%immutable BigDecimal::NaN;
%immutable BigDecimal::PositiveInfinity;
%immutable BigDecimal::NegativeInfinity;
%rename(equals) BigDecimal::operator==;
%rename(less) BigDecimal::operator<;
%rename(greater) BigDecimal::operator>;
%rename(lessOrEqual) BigDecimal::operator<=;
%rename(greaterOrEqual) BigDecimal::operator>=;
%rename(toString) BigDecimal::str;
%rename(doubleValue) BigDecimal::dbl;

%extend BigDecimal {
   const BigDecimal add(const BigDecimal& x) {return *self + x;}
   const BigDecimal subtract(const BigDecimal& x) {return *self - x;}
}

SWIG_JAVABODY_METHODS(public, public, BigDecimal)

%typemap(javacode) BigDecimal %{
    public boolean equals(Object other) {
       return other instanceof BigDecimal && equals((BigDecimal)other);
    }

    public int hashCode() {
       return (int)getIntValue();
    }

    public java.math.BigDecimal toBigDecimal() {
       long intVal = getIntValue();
       int scale = getScale();
       java.math.BigDecimal d = new java.math.BigDecimal(intVal);
       return (scale == 0 ? d : d.movePointRight(scale));
    }
%}

%include "bigdecimal.h"


/* -------------------- rwlock.h -------------------------- */
%ignore ReaderMutex;
%ignore WriterMutex;
SWIG_JAVABODY_METHODS(public, public, ILock)

%include "rwlock.h"

// copied over from Expression.h, make sure that this is in sync with that!
struct Value
{
    enum {UNDEF=0, BOOL='B', DBL='D', STR='S'} type;
    bool bl;
    double dbl;
    const char *dblunit; // stringpooled, may be NULL
    std::string s;
};

%ignore MathFunction;
%include "expression.h"

%{
typedef Expression::Value Value;
%}
