//==========================================================================
//   SIMTIME.H  - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2006 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __SIMTIME_H
#define __SIMTIME_H

#include <string>
#include <iostream>
#include "simkerneldefs.h"
#include "platdep/inttypes.h"

class cPar;

/**
 * int64-based, fixed-point simulation time. Precision is determined by a scale
 * exponent, which is global (shared by all SimTime instances), and falls in
 * the range -18..0. For example, a scale exponent of -6 means microsecond
 * precision.
 *
 * Conversions and arithmetic operations are provided specifically for double
 * and cPar, and via template functions for integer types. Performance note:
 * conversion from double uses the scale stored as double, and conversion from
 * integer types uses the scale stored as int64; the former eliminates an
 * int64-to-double conversion, and the latter allows the compiler
 * to optimize expressions like <tt>if (time>0)</tt> to a trivial integer
 * operation, without floating-point or int64 multiplication.
 *
 * The underlying raw 64-bit integer is also made accessible.
 */
class SIM_API SimTime
{
  private:
    int64 t;

    static int scaleexp;     // scale exponent in the range -18..0
    static int64 dscale;     // 10^-scaleexp, that is 1 or 1000 or 1000000...
    static double fscale;    // 10^-scaleexp, that is 1 or 1000 or 1000000...
    static double invfscale; // 1/fscale; we store it because floating-point multiplication is faster than division

  public:
    static const int SCALEEXP_S  =  0;
    static const int SCALEEXP_MS = -3;
    static const int SCALEEXP_US = -6;
    static const int SCALEEXP_NS = -9;
    static const int SCALEEXP_PS = -12;
    static const int SCALEEXP_FS = -15;

    /**
     * Constructor initializes to zero.
     */
    SimTime() {t=0;}
    template<typename T> SimTime(T d) {operator=(d);}

    /** @name Arithmetic operations */
    //@{
    const SimTime& operator=(double d) {t = (int64)(fscale*d); return *this;}
    const SimTime& operator=(const cPar& d);
    const SimTime& operator=(const SimTime& x) {t=x.t; return *this;}
    template<typename T> const SimTime& operator=(T d) {t = (int64)(dscale*d); return *this;}

    const SimTime& operator+=(const SimTime& x) {t+=x.t; return *this;}
    const SimTime& operator-=(const SimTime& x) {t-=x.t; return *this;}

    const SimTime& operator*=(double d) {t=(int64)(t*d); return *this;} //XXX to be checked on Linux, see below
    const SimTime& operator/=(double d) {t=(int64)(t/d); return *this;} //XXX to be checked on Linux, see below
    const SimTime& operator*=(const cPar& p);
    const SimTime& operator/=(const cPar& p);
    template<typename T> const SimTime& operator*=(T d) {t*=d; return *this;}
    template<typename T> const SimTime& operator/=(T d) {t/=d; return *this;}

    bool operator==(const SimTime& x) const  {return t==x.t;}
    bool operator!=(const SimTime& x) const  {return t!=x.t;}
    bool operator< (const SimTime& x) const  {return t<x.t;}
    bool operator> (const SimTime& x) const  {return t>x.t;}
    bool operator<=(const SimTime& x) const  {return t<=x.t;}
    bool operator>=(const SimTime& x) const  {return t>=x.t;}

    friend const SimTime operator+(const SimTime& x, const SimTime& y);
    friend const SimTime operator-(const SimTime& x, const SimTime& y);

    friend const SimTime operator*(const SimTime& x, double d);
    friend const SimTime operator*(double d, const SimTime& x);
    friend const SimTime operator/(const SimTime& x, double d);
    friend double operator/(const SimTime& x, const SimTime& y);

    friend const SimTime operator*(const SimTime& x, const cPar& p);
    friend const SimTime operator*(const cPar& p, const SimTime& x);
    friend const SimTime operator/(const SimTime& x, const cPar& p);

    /*
    template<typename T> friend const SimTime operator*(const SimTime& x, T d);
    template<typename T> friend const SimTime operator*(T d, const SimTime& x);
    template<typename T> friend const SimTime operator/(const SimTime& x, T d);
    */
    //@}

    /**
     * Converts simulation time to double. Note that conversion to and from
     * double may lose precision. We do not provide implicit conversion to
     * double as it would conflict with other overloaded operators, and would
     * cause ambiguities during compilation.
     */
    double dbl() const  {return t*invfscale;}

    /**
     * Converts to string.
     */
    std::string str() const;

    /**
     * Converts to a string. Use this variant over str() where performance is
     * critical. The result is placed somewhere in the given buffer (pointer
     * is returned), but for performance reasons, not necessarily at the buffer's
     * beginning. Please read the documentation of ttoa() for the minimum
     * required buffer size.
     */
    char *str(char *buf) {char *endp; return SimTime::ttoa(buf, t, scaleExp(), endp);}

    /**
     * Returns the underlying 64-bit integer.
     */
    int64 raw() const  {return t;}

    /**
     * Directly sets the underlying 64-bit integer.
     */
    const SimTime& setRaw(int64 l) {t = l; return *this;}

    /**
     * Returns the largest simulation time that can be represented using the
     * present scale exponent.
     */
    static const SimTime maxTime() {return SimTime().setRaw((((int64)1) << 63) - 1);}

    /**
     * Returns the time resolution as the number of units per second,
     * e.g. for microsecond resolution it returns 1000000.
     */
    static int64 scale()  {return dscale;}

    /**
     * Returns the scale exponent, which is an integer in the range -18..0.
     * For example, for microsecond resolution it returns -6.
     */
    static int scaleExp() {return scaleexp;}

    /**
     * Sets the scale exponent, and thus the resolution of time. Accepted
     * values are -18..0; for example, setScaleExp(-6) selects microsecond
     * resolution. Normally, the scale exponent is set from the configuration
     * file (omnetpp.ini) on simulation startup.
     *
     * IMPORTANT: This function has a global effect, and therefore
     * should NEVER be called during simulation.
     */
    static void setScaleExp(int e);

    /**
     * Converts the given string to simulation time. Throws an error if
     * there is an error during conversion. Accepted format is:
     * &lt;number&gt; or (&lt;number&gt;&lt;unit&gt;)+.
     */
    static const SimTime parse(const char *s);

    /**
     * Converts a prefix of the given string to simulation time, up to the
     * first character that cannot occur in simulation time strings:
     * not (digit or letter or "." or "+" or "-" or whitespace).
     * Throws an error if there is an error during conversion of the prefix.
     * If the prefix is empty (whitespace only), then 0 is returned, and
     * endp is set equal to s; otherwise,  endp is set to point to the
     * first character that was not converted.
     */
    static const SimTime parse(const char *s, const char *&endp);

    /**
     * Utility function to convert a 64-bit fixed point number into a string
     * buffer. scaleexp must be in the -18..0 range, and the buffer must be
     * at least 64 bytes long. A pointer to the result string will be
     * returned. A pointer to the terminating '\0' will be returned in endp.
     *
     * ATTENTION: For performance reasons, the returned pointer will point
     * *somewhere* into the buffer, but NOT necessarily at the beginning.
     */
    static char *ttoa(char *buf, int64 t, int scaleexp, char *&endp);
};

/*XXX
 for *= and /=, SystemC uses the following code:
    // linux bug workaround; don't change next two lines
    volatile double tmp = uint64_to_double( m_value ) * d;
    m_value = static_cast<int64>( tmp );
    return *this;
*/

inline const SimTime operator+(const SimTime& x, const SimTime& y)
{
    return SimTime(x)+=y;
}

inline const SimTime operator-(const SimTime& x, const SimTime& y)
{
    return SimTime(x)-=y;
}

inline const SimTime operator*(const SimTime& x, double d)
{
    return SimTime(x)*=d;
}

inline const SimTime operator*(double d, const SimTime& x)
{
    return SimTime(x)*=d;
}

inline const SimTime operator/(const SimTime& x, double d)
{
    return SimTime(x)/=d;
}

inline double operator/(const SimTime& x, const SimTime& y)
{
    return (double)x.t / (double)y.t;
}

inline std::ostream& operator<<(std::ostream& os, const SimTime& x)
{
    char buf[64]; char *endp;
    return os << SimTime::ttoa(buf, x.raw(), SimTime::scaleExp(), endp);
}

/**
 * simtime_t version of floor(double) from math.h.
 */
inline const SimTime floor(const SimTime& x)
{
    int64 u = SimTime::scale();
    int64 t = x.raw();
    return SimTime().setRaw(t - t % u); //XXX test: also OK for negative t?
}

/**
 * Generalized version of floor(), accepting a unit and an offset:
 * floor(x,u,off) = floor((x-off)/u)*u + off.
 *
 * Examples: floor(2.1234, 0.1) = 2.1; floor(2.1234, 0.1, 0.007) = 2.107;
 * floor(2.1006, 0.1, 0.007) = 2.007.
 */
inline const SimTime floor(const SimTime& x, const SimTime& unit, const SimTime& offset = SimTime())
{
    int64 off = offset.raw();
    int64 u = unit.raw();
    int64 t = x.raw() - off;
    return SimTime().setRaw(t - t % u + off); //XXX test: also OK for negative t?
}

/**
 * simtime_t version of ceil(double) from math.h.
 */
inline const SimTime ceil(const SimTime& x)
{
    int64 u = SimTime::scale();
    int64 t = x.raw() + u-1;
    return SimTime().setRaw(t - t % u); //XXX test: also OK for negative t?
}

/**
 * Generalized version of ceil(), accepting a unit and an offset:
 * ceil(x,u,off) = ceil((x-off)/u)*u + off.
 */
inline const SimTime ceil(const SimTime& x, const SimTime& unit, const SimTime& offset = SimTime())
{
    int64 off = offset.raw();
    int64 u = unit.raw();
    int64 t = x.raw() - off + u-1;
    return SimTime().setRaw(t - t % u + off); //XXX test: also OK for negative t?
}

/**
 * simtime_t version of fabs(double) from math.h.
 */
inline const SimTime fabs(const SimTime& x)
{
    return x.raw()<0 ? SimTime().setRaw(-x.raw()) : x;
}

/**
 * simtime_t version of fmod(double,double) from math.h.
 */
inline const SimTime fmod(const SimTime& x, const SimTime& y)
{
    return SimTime().setRaw(x.raw() % y.raw()); //XXX test: also OK for negative x or y?
}

/**
 * Returns the greater of the two arguments.
 */
inline const SimTime max(const SimTime& x, const SimTime& y)
{
    return x > y ? x : y;
}

/**
 * Returns the smaller of the two arguments.
 */
inline const SimTime min(const SimTime& x, const SimTime& y)
{
    return x < y ? x : y;
}


#endif
