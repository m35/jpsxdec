package jpsxdec.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.annotation.Nonnull;

/*
  File: Fraction.java

  Originally written by Doug Lea and released into the public domain.
  This may be used for any purposes whatsoever without acknowledgment.
  Thanks for the assistance and support of Sun Microsystems Labs,
  and everyone contributing, testing, and using this code.

  History:
  Date       Who                What
  7Jul1998  dl               Create public version
  11Oct1999 dl               add hashCode
*/


/**
 * An immutable class representing fractions as pairs of longs.
 * Fractions are always maintained in reduced form.
 **/
public class Fraction implements Cloneable, Comparable<Fraction> {
  public static final Fraction ZERO = new Fraction(0, 1);

  protected final long numerator_;
  protected final long denominator_;

  /** Return the getNumerator **/
  public final long getNumerator() { return numerator_; }

  /** Return the getDenominator **/
  public final long getDenominator() { return denominator_; }

  public Fraction(long i) {
    this(i, 1);
  }

  /** Create a Fraction equal in value to num / den **/
  public Fraction(long num, long den) {
    // normalize while constructing
    boolean numNonnegative = (num >= 0);
    boolean denNonnegative = (den >= 0);
    long a = numNonnegative? num : -num;
    long b = denNonnegative? den : -den;
    long g = gcd(a, b);
    numerator_ = (numNonnegative == denNonnegative)? (a / g) : (-a / g);
    denominator_ = b / g;
  }

  /** Create a fraction with the same value as Fraction f **/
  public Fraction(@Nonnull Fraction f) {
    numerator_ = f.getNumerator();
    denominator_ = f.getDenominator();
  }

  @Override
  public String toString() {
    if (getDenominator() == 1)
      return String.valueOf(getNumerator());

    DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    df.setMaximumFractionDigits(4);
    return getNumerator() + "/" + getDenominator() + " (" + df.format(asDouble()) + ")";
  }

  public @Nonnull Fraction clone() { return new Fraction(this); }

  /** Return the value of the Fraction as a double **/
  public double asDouble() {
    return ((double)(getNumerator())) / ((double)(getDenominator()));
  }

  /** Return the value of the Fraction as a float **/
  public float asFloat() {
    return ((float)(getNumerator())) / ((float)(getDenominator()));
  }

    public int asInt() {
        return (int)asLong();
    }

    public long asLong() {
        return getNumerator() / getDenominator();
    }

  /**
   * Compute the nonnegative greatest common divisor of a and b.
   * (This is needed for normalizing Fractions, but can be
   * useful on its own.)
   **/
  public static long gcd(long a, long b) {
    long x;
    long y;

    if (a < 0) a = -a;
    if (b < 0) b = -b;

    if (a >= b) { x = a; y = b; }
    else        { x = b; y = a; }

    while (y != 0) {
      long t = x % y;
      x = y;
      y = t;
    }
    return x;
  }

  public static @Nonnull Fraction divide(long a, Fraction b) {
    long an = a;
    long ad = 1;
    long bn = b.getNumerator();
    long bd = b.getDenominator();
    return new Fraction(an*bd, ad*bn);
  }

  /** return a Fraction representing the negated value of this Fraction **/
  public @Nonnull Fraction negative() {
    long an = getNumerator();
    long ad = getDenominator();
    return new Fraction(-an, ad);
  }

  /** return a Fraction representing 1 / this Fraction **/
  public @Nonnull Fraction reciprocal() {
    long an = getNumerator();
    long ad = getDenominator();
    return new Fraction(ad, an);
  }


  /** return a Fraction representing this Fraction plus b **/
  public @Nonnull Fraction add(Fraction b) {
    long an = getNumerator();
    long ad = getDenominator();
    long bn = b.getNumerator();
    long bd = b.getDenominator();
    return new Fraction(an*bd+bn*ad, ad*bd);
  }

  /** return a Fraction representing this Fraction plus n **/
  public @Nonnull Fraction add(long n) {
    long an = getNumerator();
    long ad = getDenominator();
    long bn = n;
    long bd = 1;
    return new Fraction(an*bd+bn*ad, ad*bd);
  }

  /** return a Fraction representing this Fraction minus b **/
  public @Nonnull Fraction subtract(Fraction b) {
    long an = getNumerator();
    long ad = getDenominator();
    long bn = b.getNumerator();
    long bd = b.getDenominator();
    return new Fraction(an*bd-bn*ad, ad*bd);
  }

  /** return a Fraction representing this Fraction minus n **/
  public @Nonnull Fraction subtract(long n) {
    long an = getNumerator();
    long ad = getDenominator();
    long bn = n;
    long bd = 1;
    return new Fraction(an*bd-bn*ad, ad*bd);
  }


  /** return a Fraction representing this Fraction times b **/
  public @Nonnull Fraction multiply(Fraction b) {
    long an = getNumerator();
    long ad = getDenominator();
    long bn = b.getNumerator();
    long bd = b.getDenominator();
    return new Fraction(an*bn, ad*bd);
  }

  /** return a Fraction representing this Fraction times n **/
  public @Nonnull Fraction multiply(long n) {
    long an = getNumerator();
    long ad = getDenominator();
    long bn = n;
    long bd = 1;
    return new Fraction(an*bn, ad*bd);
  }

  public float multiply(float f) {
    return getNumerator() * f / getDenominator();
  }

  /** return a Fraction representing this Fraction divided by b **/
  public @Nonnull Fraction divide(@Nonnull Fraction b) {
    long an = getNumerator();
    long ad = getDenominator();
    long bn = b.getNumerator();
    long bd = b.getDenominator();
    return new Fraction(an*bd, ad*bn);
  }

  /** return a Fraction representing this Fraction divided by n **/
  public @Nonnull Fraction divide(long n) {
    long an = getNumerator();
    long ad = getDenominator();
    long bn = n;
    long bd = 1;
    return new Fraction(an*bd, ad*bn);
  }

  public @Nonnull Fraction abs() {
      return new Fraction(Math.abs(numerator_), denominator_);
  }

  /** return a number less, equal, or greater than zero
   * reflecting whether this Fraction is less, equal or greater than
   * the value of Fraction other.
   **/
  @Override
  public int compareTo(Fraction other) {
    long an = getNumerator();
    long ad = getDenominator();
    long bn = other.getNumerator();
    long bd = other.getDenominator();
    long l = an*bd;
    long r = bn*ad;
    return (l < r)? -1 : ((l == r)? 0: 1);
  }

  /** return a number less, equal, or greater than zero
   * reflecting whether this Fraction is less, equal or greater than n.
   **/

  public int compareTo(long n) {
    long an = getNumerator();
    long ad = getDenominator();
    long bn = n;
    long bd = 1;
    long l = an*bd;
    long r = bn*ad;
    return (l < r)? -1 : ((l == r)? 0: 1);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Fraction))
        return false;
    return compareTo((Fraction)other) == 0;
  }

  public boolean equals(long n) {
    return compareTo(n) == 0;
  }

  @Override
  public int hashCode() {
    return (int) (numerator_ ^ denominator_);
  }

}
