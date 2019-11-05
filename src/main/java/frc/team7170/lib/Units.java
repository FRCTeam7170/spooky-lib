package frc.team7170.lib;

/**
 * <p>
 * Unit conversions for the most commonly used units in FRC. Simple conversions, such as between metric types or between
 * units of time, are not provided.
 * </p>
 * <p>
 * Note that, for example, one could use the conversion for metres to feet to convert from metres per second to feet per
 * second, even though conversions between velocity types are not explicitly provided.
 * </p>
 *
 * @author Robert Russell
 */
public final class Units {

    /**
     * The number of feet in a metre.
     */
    public static final double FT_PER_M = 3.28083989501312;

    /**
     * The number of inches in a foot.
     */
    public static final double IN_PER_FT = 12.0;

    /**
     * The number of pounds in a kilogram.
     */
    public static final double LB_PER_KG = 2.2046226218488;

    /**
     * Gravitational acceleration in metres per second squared.
     */
    public static final double GRAVITY_M_S2 = 9.81;

    /* LENGTH */

    /**
     * Convert metres to feet.
     *
     * @param m the value in metres.
     * @return the value in feet.
     */
    public static double m2ft(double m) {
        return m * FT_PER_M;
    }

    /**
     * Convert feet to metres.
     *
     * @param ft the value in feet
     * @return the value in metres
     */
    public static double ft2m(double ft) {
        return ft / FT_PER_M;
    }

    /**
     * Convert feet to inches.
     *
     * @param ft the value in feet.
     * @return the value in inches.
     */
    public static double ft2in(double ft) {
        return ft * IN_PER_FT;
    }

    /**
     * Convert inches to feet.
     *
     * @param in the value in inches.
     * @return the value in feet.
     */
    public static double in2ft(double in) {
        return in / IN_PER_FT;
    }

    /**
     * Convert metres to inches.
     *
     * @param m the value in metres.
     * @return the value in inches.
     */
    public static double m2in(double m) {
        return ft2in(m2ft(m));
    }

    /**
     * Convert inches to metres.
     *
     * @param in the value in inches.
     * @return the value in metres.
     */
    public static double in2m(double in) {
        return ft2m(in2ft(in));
    }

    /* Mass */

    /**
     * Convert kilograms to pounds.
     *
     * @param kg the value in kilograms.
     * @return the value in pounds.
     */
    public static double kg2lb(double kg) {
        return kg * LB_PER_KG;
    }

    /**
     * Convert pounds to kilograms.
     *
     * @param lb the value in pounds.
     * @return the value in kilograms.
     */
    public static double lb2kg(double lb) {
        return lb / LB_PER_KG;
    }

    /* Rotation */

    /**
     * Convert radians per second to RPM (revolutions per minute).
     *
     * @param rad_s the value in radians per second.
     * @return the value in RPM.
     */
    public static double rad_s2rpm(double rad_s) {
        // 1/(2pi) revolutions per radian; 60 seconds per minute.
        return rad_s / (2 * Math.PI) * 60.0;
    }

    /**
     * Convert RPM (revolutions per minute) to radians per second.
     *
     * @param rpm the value in RPM.
     * @return the value in radians per second.
     */
    public static double rpm2rad_s(double rpm) {
        // 2pi radians per revolution; 1/60 minutes per second.
        return rpm * (2 * Math.PI) / 60.0;
    }

    /* Angles */

    /**
     * Convert radians to degrees.
     *
     * @param rad the value in radians.
     * @return the value in degrees.
     */
    public static double rad2deg(double rad) {
        return Math.toDegrees(rad);
    }

    /**
     * Convert degrees to radians.
     * @param deg the value in degrees.
     * @return the value in radians.
     */
    public static double deg2rad(double deg) {
        return Math.toRadians(deg);
    }

    /**
     * Convert radians to revolutions.
     *
     * @param rad the value in radians.
     * @return the value in revolutions.
     */
    public static double rad2rev(double rad) {
        return rad / (2 * Math.PI);
    }

    /**
     * Convert revolutions to radians.
     *
     * @param rev the value in revolutions.
     * @return the value in radians.
     */
    public static double rev2rad(double rev) {
        return rev * (2 * Math.PI);
    }

    /**
     * Convert degrees to revolutions.
     *
     * @param deg the value in degrees.
     * @return the value in revolutions.
     */
    public static double deg2rev(double deg) {
        return deg / 360.0;
    }

    /**
     * Convert revolutions to degrees.
     *
     * @param rev the value in revolutions.
     * @return the value in degrees.
     */
    public static double rev2deg(double rev) {
        return rev * 360.0;
    }
}
