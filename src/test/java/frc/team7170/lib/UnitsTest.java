package frc.team7170.lib;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;

public class UnitsTest {

    private static final double EPSILON = 1E-6;

    @Test
    void m2ft() {
        assertAll(
                () -> assertThat(Units.m2ft(0.0), closeTo(0.0, EPSILON)),
                () -> assertThat(Units.m2ft(5.0), closeTo(16.404_199, EPSILON)),
                () -> assertThat(Units.m2ft(-5.0), closeTo(-16.404_199, EPSILON))
        );
    }

    @Test
    void ft2m() {
        assertAll(
                () -> assertThat(Units.ft2m(0.0), closeTo(0.0, EPSILON)),
                () -> assertThat(Units.ft2m(5.0), closeTo(1.524_000, EPSILON)),
                () -> assertThat(Units.ft2m(-5.0), closeTo(-1.524_000, EPSILON))
        );
    }

    @Test
    void ft2in() {
        assertAll(
                () -> assertThat(Units.ft2in(0.0), closeTo(0.0, EPSILON)),
                () -> assertThat(Units.ft2in(5.0), closeTo(60.0, EPSILON)),
                () -> assertThat(Units.ft2in(-5.0), closeTo(-60.0, EPSILON))
        );
    }

    @Test
    void in2ft() {
        assertAll(
                () -> assertThat(Units.in2ft(0.0), closeTo(0.0, EPSILON)),
                () -> assertThat(Units.in2ft(5.0), closeTo(0.416_667, EPSILON)),
                () -> assertThat(Units.in2ft(-5.0), closeTo(-0.416_667, EPSILON))
        );
    }

    @Test
    void m2in() {
        assertAll(
                () -> assertThat(Units.m2in(0.0), closeTo(0.0, EPSILON)),
                () -> assertThat(Units.m2in(5.0), closeTo(196.850_394, EPSILON)),
                () -> assertThat(Units.m2in(-5.0), closeTo(-196.850_394, EPSILON))
        );
    }

    @Test
    void in2m() {
        assertAll(
                () -> assertThat(Units.in2m(0.0), closeTo(0.0, EPSILON)),
                () -> assertThat(Units.in2m(5.0), closeTo(0.127_000, EPSILON)),
                () -> assertThat(Units.in2m(-5.0), closeTo(-0.127_000, EPSILON))
        );
    }

    @Test
    void kg2lb() {
        assertAll(
                () -> assertThat(Units.kg2lb(0.0), closeTo(0.0, EPSILON)),
                () -> assertThat(Units.kg2lb(5.0), closeTo(11.023_113, EPSILON)),
                () -> assertThat(Units.kg2lb(-5.0), closeTo(-11.023_113, EPSILON))
        );
    }

    @Test
    void lb2kg() {
        assertAll(
                () -> assertThat(Units.lb2kg(0.0), closeTo(0.0, EPSILON)),
                () -> assertThat(Units.lb2kg(5.0), closeTo(2.267_962, EPSILON)),
                () -> assertThat(Units.lb2kg(-5.0), closeTo(-2.267_962, EPSILON))
        );
    }

    @Test
    void rad_s2rpm() {
        assertAll(
                () -> assertThat(Units.rad_s2rpm(0.0), closeTo(0.0, EPSILON)),
                () -> assertThat(Units.rad_s2rpm(5.0), closeTo(47.746_483, EPSILON)),
                () -> assertThat(Units.rad_s2rpm(-5.0), closeTo(-47.746_483, EPSILON))
        );
    }

    @Test
    void rpm2rad_s() {
        assertAll(
                () -> assertThat(Units.rpm2rad_s(0.0), closeTo(0.0, EPSILON)),
                () -> assertThat(Units.rpm2rad_s(5.0), closeTo(0.523_599, EPSILON)),
                () -> assertThat(Units.rpm2rad_s(-5.0), closeTo(-0.523_599, EPSILON))
        );
    }

    @Test
    void rad2deg() {
        assertAll(
                () -> assertThat(Units.rad2deg(0.0), closeTo(0.0, EPSILON)),
                () -> assertThat(Units.rad2deg(5.0), closeTo(286.478_898, EPSILON)),
                () -> assertThat(Units.rad2deg(-5.0), closeTo(-286.478_898, EPSILON))
        );
    }

    @Test
    void deg2rad() {
        assertAll(
                () -> assertThat(Units.deg2rad(0.0), closeTo(0.0, EPSILON)),
                () -> assertThat(Units.deg2rad(5.0), closeTo(0.087_267, EPSILON)),
                () -> assertThat(Units.deg2rad(-5.0), closeTo(-0.087_267, EPSILON))
        );
    }

    @Test
    void rad2rev() {
        assertAll(
                () -> assertThat(Units.rad2rev(0.0), closeTo(0.0, EPSILON)),
                () -> assertThat(Units.rad2rev(5.0), closeTo(0.795_775, EPSILON)),
                () -> assertThat(Units.rad2rev(-5.0), closeTo(-0.795_775, EPSILON))
        );
    }

    @Test
    void rev2rad() {
        assertAll(
                () -> assertThat(Units.rev2rad(0.0), closeTo(0.0, EPSILON)),
                () -> assertThat(Units.rev2rad(5.0), closeTo(31.415_927, EPSILON)),
                () -> assertThat(Units.rev2rad(-5.0), closeTo(-31.415_927, EPSILON))
        );
    }

    @Test
    void deg2rev() {
        assertAll(
                () -> assertThat(Units.deg2rev(0.0), closeTo(0.0, EPSILON)),
                () -> assertThat(Units.deg2rev(5.0), closeTo(0.013_889, EPSILON)),
                () -> assertThat(Units.deg2rev(-5.0), closeTo(-0.013_889, EPSILON))
        );
    }

    @Test
    void rev2deg() {
        assertAll(
                () -> assertThat(Units.rev2deg(0.0), closeTo(0.0, EPSILON)),
                () -> assertThat(Units.rev2deg(5.0), closeTo(1800.0, EPSILON)),
                () -> assertThat(Units.rev2deg(-5.0), closeTo(-1800.0, EPSILON))
        );
    }
}
