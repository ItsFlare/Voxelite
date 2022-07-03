package edu.kit.scc.git.ggd.voxelite.world.generator.noise;
/*
 * 2014 OpenSimplex Noise in Java.
 * by Kurt Spencer
 *
 * Updated Dec 2019 and Feb 2020:
 * - New lattice-symmetric gradient sets
 * - Optional alternate lattice orientation evaluators
 *
 * This implementation has been updated to slightly improve its output, but it is recommented to first
 * try the newer OpenSimplex2S or OpenSimplex2F noise. These are located in the OpenSimplex2 repo:
 * https://github.com/KdotJPG/OpenSimplex2
 *
 * In the event that the output of this OpenSimplex continues to better fit your project's needs than
 * either OpenSimplex2 variant, an updated backport of DigitalShadow's optimization is available here:
 * https://github.com/KdotJPG/OpenSimplex2/blob/master/java/legacy/OpenSimplex.java
 *
 * This is mostly kept here for reference. In particular, the 4D code is very slow.
 */

import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;

public class SimplexNoise implements Noise {

    private static final double STRETCH_CONSTANT_2D = -0.211324865405187;       // (1/Math.sqrt(2+1)-1)/2;
    private static final double SQUISH_CONSTANT_2D  = 0.366025403784439;        // (Math.sqrt(2+1)-1)/2;
    private static final double STRETCH_CONSTANT_3D = -1.0 / 6;                 // (1/Math.sqrt(3+1)-1)/3;
    private static final double SQUISH_CONSTANT_3D  = 1.0 / 3;                  // (Math.sqrt(3+1)-1)/3;

    private static final long DEFAULT_SEED = 0;

    private static final int PSIZE = 2048;
    private static final int PMASK = 2047;

    private final short[] perm;
    private final Grad2[] permGrad2;
    private final Grad3[] permGrad3;

    public SimplexNoise() {
        this(DEFAULT_SEED);
    }

    public SimplexNoise(short[] perm) {
        this.perm = perm;
        permGrad2 = new Grad2[PSIZE];
        permGrad3 = new Grad3[PSIZE];

        for (int i = 0; i < PSIZE; i++) {
            permGrad2[i] = GRADIENTS_2D[perm[i]];
            permGrad3[i] = GRADIENTS_3D[perm[i]];
        }
    }

    public SimplexNoise(long seed) {
        perm = new short[PSIZE];
        permGrad2 = new Grad2[PSIZE];
        permGrad3 = new Grad3[PSIZE];
        short[] source = new short[PSIZE];
        for (short i = 0; i < PSIZE; i++)
            source[i] = i;
        for (int i = PSIZE - 1; i >= 0; i--) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int r = (int) ((seed + 31) % (i + 1));
            if (r < 0)
                r += (i + 1);
            perm[i] = source[r];
            permGrad2[i] = GRADIENTS_2D[perm[i]];
            permGrad3[i] = GRADIENTS_3D[perm[i]];
            source[r] = source[i];
        }
    }

    // 2D OpenSimplex Noise.
    public float sample(Vec2f position) {
        double x = position.x();
        double y = position.y();

        // Place input coordinates onto grid.
        double stretchOffset = (x + y) * STRETCH_CONSTANT_2D;
        double xs = x + stretchOffset;
        double ys = y + stretchOffset;

        // Floor to get grid coordinates of rhombus (stretched square) super-cell origin.
        int xsb = fastFloor(xs);
        int ysb = fastFloor(ys);

        // Compute grid coordinates relative to rhombus origin.
        double xins = xs - xsb;
        double yins = ys - ysb;

        // Sum those together to get a value that determines which region we're in.
        double inSum = xins + yins;

        // Positions relative to origin point.
        double squishOffsetIns = inSum * SQUISH_CONSTANT_2D;
        double dx0 = xins + squishOffsetIns;
        double dy0 = yins + squishOffsetIns;

        // We'll be defining these inside the next block and using them afterwards.
        double dx_ext, dy_ext;
        int xsv_ext, ysv_ext;

        double value = 0;

        // Contribution (1,0)
        double dx1 = dx0 - 1 - SQUISH_CONSTANT_2D;
        double dy1 = dy0 - 0 - SQUISH_CONSTANT_2D;
        double attn1 = 2 - dx1 * dx1 - dy1 * dy1;
        if (attn1 > 0) {
            attn1 *= attn1;
            value += attn1 * attn1 * extrapolate(xsb + 1, ysb, dx1, dy1);
        }

        // Contribution (0,1)
        double dx2 = dx0 - 0 - SQUISH_CONSTANT_2D;
        double dy2 = dy0 - 1 - SQUISH_CONSTANT_2D;
        double attn2 = 2 - dx2 * dx2 - dy2 * dy2;
        if (attn2 > 0) {
            attn2 *= attn2;
            value += attn2 * attn2 * extrapolate(xsb, ysb + 1, dx2, dy2);
        }

        if (inSum <= 1) { // We're inside the triangle (2-Simplex) at (0,0)
            double zins = 1 - inSum;
            if (zins > xins || zins > yins) { // (0,0) is one of the closest two triangular vertices
                if (xins > yins) {
                    xsv_ext = xsb + 1;
                    ysv_ext = ysb - 1;
                    dx_ext = dx0 - 1;
                    dy_ext = dy0 + 1;
                } else {
                    xsv_ext = xsb - 1;
                    ysv_ext = ysb + 1;
                    dx_ext = dx0 + 1;
                    dy_ext = dy0 - 1;
                }
            } else { // (1,0) and (0,1) are the closest two vertices.
                xsv_ext = xsb + 1;
                ysv_ext = ysb + 1;
                dx_ext = dx0 - 1 - 2 * SQUISH_CONSTANT_2D;
                dy_ext = dy0 - 1 - 2 * SQUISH_CONSTANT_2D;
            }
        } else { // We're inside the triangle (2-Simplex) at (1,1)
            double zins = 2 - inSum;
            if (zins < xins || zins < yins) { // (0,0) is one of the closest two triangular vertices
                if (xins > yins) {
                    xsv_ext = xsb + 2;
                    ysv_ext = ysb;
                    dx_ext = dx0 - 2 - 2 * SQUISH_CONSTANT_2D;
                    dy_ext = dy0 + 0 - 2 * SQUISH_CONSTANT_2D;
                } else {
                    xsv_ext = xsb;
                    ysv_ext = ysb + 2;
                    dx_ext = dx0 + 0 - 2 * SQUISH_CONSTANT_2D;
                    dy_ext = dy0 - 2 - 2 * SQUISH_CONSTANT_2D;
                }
            } else { // (1,0) and (0,1) are the closest two vertices.
                dx_ext = dx0;
                dy_ext = dy0;
                xsv_ext = xsb;
                ysv_ext = ysb;
            }
            xsb += 1;
            ysb += 1;
            dx0 = dx0 - 1 - 2 * SQUISH_CONSTANT_2D;
            dy0 = dy0 - 1 - 2 * SQUISH_CONSTANT_2D;
        }

        // Contribution (0,0) or (1,1)
        double attn0 = 2 - dx0 * dx0 - dy0 * dy0;
        if (attn0 > 0) {
            attn0 *= attn0;
            value += attn0 * attn0 * extrapolate(xsb, ysb, dx0, dy0);
        }

        // Extra Vertex
        double attn_ext = 2 - dx_ext * dx_ext - dy_ext * dy_ext;
        if (attn_ext > 0) {
            attn_ext *= attn_ext;
            value += attn_ext * attn_ext * extrapolate(xsv_ext, ysv_ext, dx_ext, dy_ext);
        }

        return (float) value;
    }

    // 3D OpenSimplex Noise.
    public float sample(Vec3f position) {
        double x = position.x();
        double y = position.y();
        double z = position.z();

        // Place input coordinates on simplectic honeycomb.
        double stretchOffset = (x + y + z) * STRETCH_CONSTANT_3D;
        double xs = x + stretchOffset;
        double ys = y + stretchOffset;
        double zs = z + stretchOffset;

        return (float) eval3_Base(xs, ys, zs);
    }

    // 3D OpenSimplex Noise (base which takes skewed coordinates directly).
    private double eval3_Base(double xs, double ys, double zs) {

        // Floor to get simplectic honeycomb coordinates of rhombohedron (stretched cube) super-cell origin.
        int xsb = fastFloor(xs);
        int ysb = fastFloor(ys);
        int zsb = fastFloor(zs);

        // Compute simplectic honeycomb coordinates relative to rhombohedral origin.
        double xins = xs - xsb;
        double yins = ys - ysb;
        double zins = zs - zsb;

        // Sum those together to get a value that determines which region we're in.
        double inSum = xins + yins + zins;

        // Positions relative to origin point.
        double squishOffsetIns = inSum * SQUISH_CONSTANT_3D;
        double dx0 = xins + squishOffsetIns;
        double dy0 = yins + squishOffsetIns;
        double dz0 = zins + squishOffsetIns;

        // We'll be defining these inside the next block and using them afterwards.
        double dx_ext0, dy_ext0, dz_ext0;
        double dx_ext1, dy_ext1, dz_ext1;
        int xsv_ext0, ysv_ext0, zsv_ext0;
        int xsv_ext1, ysv_ext1, zsv_ext1;

        double value = 0;
        if (inSum <= 1) { // We're inside the tetrahedron (3-Simplex) at (0,0,0)

            // Determine which two of (0,0,1), (0,1,0), (1,0,0) are closest.
            byte aPoint = 0x01;
            double aScore = xins;
            byte bPoint = 0x02;
            double bScore = yins;
            if (aScore >= bScore && zins > bScore) {
                bScore = zins;
                bPoint = 0x04;
            } else if (aScore < bScore && zins > aScore) {
                aScore = zins;
                aPoint = 0x04;
            }

            // Now we determine the two lattice points not part of the tetrahedron that may contribute.
            // This depends on the closest two tetrahedral vertices, including (0,0,0)
            double wins = 1 - inSum;
            if (wins > aScore || wins > bScore) { // (0,0,0) is one of the closest two tetrahedral vertices.
                byte c = (bScore > aScore ? bPoint : aPoint); // Our other closest vertex is the closest out of a and b.

                if ((c & 0x01) == 0) {
                    xsv_ext0 = xsb - 1;
                    xsv_ext1 = xsb;
                    dx_ext0 = dx0 + 1;
                    dx_ext1 = dx0;
                } else {
                    xsv_ext0 = xsv_ext1 = xsb + 1;
                    dx_ext0 = dx_ext1 = dx0 - 1;
                }

                if ((c & 0x02) == 0) {
                    ysv_ext0 = ysv_ext1 = ysb;
                    dy_ext0 = dy_ext1 = dy0;
                    if ((c & 0x01) == 0) {
                        ysv_ext1 -= 1;
                        dy_ext1 += 1;
                    } else {
                        ysv_ext0 -= 1;
                        dy_ext0 += 1;
                    }
                } else {
                    ysv_ext0 = ysv_ext1 = ysb + 1;
                    dy_ext0 = dy_ext1 = dy0 - 1;
                }

                if ((c & 0x04) == 0) {
                    zsv_ext0 = zsb;
                    zsv_ext1 = zsb - 1;
                    dz_ext0 = dz0;
                    dz_ext1 = dz0 + 1;
                } else {
                    zsv_ext0 = zsv_ext1 = zsb + 1;
                    dz_ext0 = dz_ext1 = dz0 - 1;
                }
            } else { // (0,0,0) is not one of the closest two tetrahedral vertices.
                byte c = (byte) (aPoint | bPoint); // Our two extra vertices are determined by the closest two.

                if ((c & 0x01) == 0) {
                    xsv_ext0 = xsb;
                    xsv_ext1 = xsb - 1;
                    dx_ext0 = dx0 - 2 * SQUISH_CONSTANT_3D;
                    dx_ext1 = dx0 + 1 - SQUISH_CONSTANT_3D;
                } else {
                    xsv_ext0 = xsv_ext1 = xsb + 1;
                    dx_ext0 = dx0 - 1 - 2 * SQUISH_CONSTANT_3D;
                    dx_ext1 = dx0 - 1 - SQUISH_CONSTANT_3D;
                }

                if ((c & 0x02) == 0) {
                    ysv_ext0 = ysb;
                    ysv_ext1 = ysb - 1;
                    dy_ext0 = dy0 - 2 * SQUISH_CONSTANT_3D;
                    dy_ext1 = dy0 + 1 - SQUISH_CONSTANT_3D;
                } else {
                    ysv_ext0 = ysv_ext1 = ysb + 1;
                    dy_ext0 = dy0 - 1 - 2 * SQUISH_CONSTANT_3D;
                    dy_ext1 = dy0 - 1 - SQUISH_CONSTANT_3D;
                }

                if ((c & 0x04) == 0) {
                    zsv_ext0 = zsb;
                    zsv_ext1 = zsb - 1;
                    dz_ext0 = dz0 - 2 * SQUISH_CONSTANT_3D;
                    dz_ext1 = dz0 + 1 - SQUISH_CONSTANT_3D;
                } else {
                    zsv_ext0 = zsv_ext1 = zsb + 1;
                    dz_ext0 = dz0 - 1 - 2 * SQUISH_CONSTANT_3D;
                    dz_ext1 = dz0 - 1 - SQUISH_CONSTANT_3D;
                }
            }

            // Contribution (0,0,0)
            double attn0 = 2 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0;
            if (attn0 > 0) {
                attn0 *= attn0;
                value += attn0 * attn0 * extrapolate(xsb, ysb, zsb, dx0, dy0, dz0);
            }

            // Contribution (1,0,0)
            double dx1 = dx0 - 1 - SQUISH_CONSTANT_3D;
            double dy1 = dy0 - 0 - SQUISH_CONSTANT_3D;
            double dz1 = dz0 - 0 - SQUISH_CONSTANT_3D;
            double attn1 = 2 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1;
            if (attn1 > 0) {
                attn1 *= attn1;
                value += attn1 * attn1 * extrapolate(xsb + 1, ysb, zsb, dx1, dy1, dz1);
            }

            // Contribution (0,1,0)
            double dx2 = dx0 - 0 - SQUISH_CONSTANT_3D;
            double dy2 = dy0 - 1 - SQUISH_CONSTANT_3D;
            double attn2 = 2 - dx2 * dx2 - dy2 * dy2 - dz1 * dz1;
            if (attn2 > 0) {
                attn2 *= attn2;
                value += attn2 * attn2 * extrapolate(xsb, ysb + 1, zsb, dx2, dy2, dz1);
            }

            // Contribution (0,0,1)
            double dz3 = dz0 - 1 - SQUISH_CONSTANT_3D;
            double attn3 = 2 - dx2 * dx2 - dy1 * dy1 - dz3 * dz3;
            if (attn3 > 0) {
                attn3 *= attn3;
                value += attn3 * attn3 * extrapolate(xsb, ysb, zsb + 1, dx2, dy1, dz3);
            }
        } else if (inSum >= 2) { // We're inside the tetrahedron (3-Simplex) at (1,1,1)

            // Determine which two tetrahedral vertices are the closest, out of (1,1,0), (1,0,1), (0,1,1) but not (1,1,1).
            byte aPoint = 0x06;
            double aScore = xins;
            byte bPoint = 0x05;
            double bScore = yins;
            if (aScore <= bScore && zins < bScore) {
                bScore = zins;
                bPoint = 0x03;
            } else if (aScore > bScore && zins < aScore) {
                aScore = zins;
                aPoint = 0x03;
            }

            // Now we determine the two lattice points not part of the tetrahedron that may contribute.
            // This depends on the closest two tetrahedral vertices, including (1,1,1)
            double wins = 3 - inSum;
            if (wins < aScore || wins < bScore) { // (1,1,1) is one of the closest two tetrahedral vertices.
                byte c = (bScore < aScore ? bPoint : aPoint); // Our other closest vertex is the closest out of a and b.

                if ((c & 0x01) != 0) {
                    xsv_ext0 = xsb + 2;
                    xsv_ext1 = xsb + 1;
                    dx_ext0 = dx0 - 2 - 3 * SQUISH_CONSTANT_3D;
                    dx_ext1 = dx0 - 1 - 3 * SQUISH_CONSTANT_3D;
                } else {
                    xsv_ext0 = xsv_ext1 = xsb;
                    dx_ext0 = dx_ext1 = dx0 - 3 * SQUISH_CONSTANT_3D;
                }

                if ((c & 0x02) != 0) {
                    ysv_ext0 = ysv_ext1 = ysb + 1;
                    dy_ext0 = dy_ext1 = dy0 - 1 - 3 * SQUISH_CONSTANT_3D;
                    if ((c & 0x01) != 0) {
                        ysv_ext1 += 1;
                        dy_ext1 -= 1;
                    } else {
                        ysv_ext0 += 1;
                        dy_ext0 -= 1;
                    }
                } else {
                    ysv_ext0 = ysv_ext1 = ysb;
                    dy_ext0 = dy_ext1 = dy0 - 3 * SQUISH_CONSTANT_3D;
                }

                if ((c & 0x04) != 0) {
                    zsv_ext0 = zsb + 1;
                    zsv_ext1 = zsb + 2;
                    dz_ext0 = dz0 - 1 - 3 * SQUISH_CONSTANT_3D;
                    dz_ext1 = dz0 - 2 - 3 * SQUISH_CONSTANT_3D;
                } else {
                    zsv_ext0 = zsv_ext1 = zsb;
                    dz_ext0 = dz_ext1 = dz0 - 3 * SQUISH_CONSTANT_3D;
                }
            } else { // (1,1,1) is not one of the closest two tetrahedral vertices.
                byte c = (byte) (aPoint & bPoint); // Our two extra vertices are determined by the closest two.

                if ((c & 0x01) != 0) {
                    xsv_ext0 = xsb + 1;
                    xsv_ext1 = xsb + 2;
                    dx_ext0 = dx0 - 1 - SQUISH_CONSTANT_3D;
                    dx_ext1 = dx0 - 2 - 2 * SQUISH_CONSTANT_3D;
                } else {
                    xsv_ext0 = xsv_ext1 = xsb;
                    dx_ext0 = dx0 - SQUISH_CONSTANT_3D;
                    dx_ext1 = dx0 - 2 * SQUISH_CONSTANT_3D;
                }

                if ((c & 0x02) != 0) {
                    ysv_ext0 = ysb + 1;
                    ysv_ext1 = ysb + 2;
                    dy_ext0 = dy0 - 1 - SQUISH_CONSTANT_3D;
                    dy_ext1 = dy0 - 2 - 2 * SQUISH_CONSTANT_3D;
                } else {
                    ysv_ext0 = ysv_ext1 = ysb;
                    dy_ext0 = dy0 - SQUISH_CONSTANT_3D;
                    dy_ext1 = dy0 - 2 * SQUISH_CONSTANT_3D;
                }

                if ((c & 0x04) != 0) {
                    zsv_ext0 = zsb + 1;
                    zsv_ext1 = zsb + 2;
                    dz_ext0 = dz0 - 1 - SQUISH_CONSTANT_3D;
                    dz_ext1 = dz0 - 2 - 2 * SQUISH_CONSTANT_3D;
                } else {
                    zsv_ext0 = zsv_ext1 = zsb;
                    dz_ext0 = dz0 - SQUISH_CONSTANT_3D;
                    dz_ext1 = dz0 - 2 * SQUISH_CONSTANT_3D;
                }
            }

            // Contribution (1,1,0)
            double dx3 = dx0 - 1 - 2 * SQUISH_CONSTANT_3D;
            double dy3 = dy0 - 1 - 2 * SQUISH_CONSTANT_3D;
            double dz3 = dz0 - 0 - 2 * SQUISH_CONSTANT_3D;
            double attn3 = 2 - dx3 * dx3 - dy3 * dy3 - dz3 * dz3;
            if (attn3 > 0) {
                attn3 *= attn3;
                value += attn3 * attn3 * extrapolate(xsb + 1, ysb + 1, zsb, dx3, dy3, dz3);
            }

            // Contribution (1,0,1)
            double dy2 = dy0 - 0 - 2 * SQUISH_CONSTANT_3D;
            double dz2 = dz0 - 1 - 2 * SQUISH_CONSTANT_3D;
            double attn2 = 2 - dx3 * dx3 - dy2 * dy2 - dz2 * dz2;
            if (attn2 > 0) {
                attn2 *= attn2;
                value += attn2 * attn2 * extrapolate(xsb + 1, ysb, zsb + 1, dx3, dy2, dz2);
            }

            // Contribution (0,1,1)
            double dx1 = dx0 - 0 - 2 * SQUISH_CONSTANT_3D;
            double attn1 = 2 - dx1 * dx1 - dy3 * dy3 - dz2 * dz2;
            if (attn1 > 0) {
                attn1 *= attn1;
                value += attn1 * attn1 * extrapolate(xsb, ysb + 1, zsb + 1, dx1, dy3, dz2);
            }

            // Contribution (1,1,1)
            dx0 = dx0 - 1 - 3 * SQUISH_CONSTANT_3D;
            dy0 = dy0 - 1 - 3 * SQUISH_CONSTANT_3D;
            dz0 = dz0 - 1 - 3 * SQUISH_CONSTANT_3D;
            double attn0 = 2 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0;
            if (attn0 > 0) {
                attn0 *= attn0;
                value += attn0 * attn0 * extrapolate(xsb + 1, ysb + 1, zsb + 1, dx0, dy0, dz0);
            }
        } else { // We're inside the octahedron (Rectified 3-Simplex) in between.
            double aScore;
            byte aPoint;
            boolean aIsFurtherSide;
            double bScore;
            byte bPoint;
            boolean bIsFurtherSide;

            // Decide between point (0,0,1) and (1,1,0) as closest
            double p1 = xins + yins;
            if (p1 > 1) {
                aScore = p1 - 1;
                aPoint = 0x03;
                aIsFurtherSide = true;
            } else {
                aScore = 1 - p1;
                aPoint = 0x04;
                aIsFurtherSide = false;
            }

            // Decide between point (0,1,0) and (1,0,1) as closest
            double p2 = xins + zins;
            if (p2 > 1) {
                bScore = p2 - 1;
                bPoint = 0x05;
                bIsFurtherSide = true;
            } else {
                bScore = 1 - p2;
                bPoint = 0x02;
                bIsFurtherSide = false;
            }

            // The closest out of the two (1,0,0) and (0,1,1) will replace the furthest out of the two decided above, if closer.
            double p3 = yins + zins;
            if (p3 > 1) {
                double score = p3 - 1;
                if (aScore <= bScore && aScore < score) {
                    aPoint = 0x06;
                    aIsFurtherSide = true;
                } else if (aScore > bScore && bScore < score) {
                    bPoint = 0x06;
                    bIsFurtherSide = true;
                }
            } else {
                double score = 1 - p3;
                if (aScore <= bScore && aScore < score) {
                    aPoint = 0x01;
                    aIsFurtherSide = false;
                } else if (aScore > bScore && bScore < score) {
                    bPoint = 0x01;
                    bIsFurtherSide = false;
                }
            }

            // Where each of the two closest points are determines how the extra two vertices are calculated.
            if (aIsFurtherSide == bIsFurtherSide) {
                if (aIsFurtherSide) { // Both closest points on (1,1,1) side

                    // One of the two extra points is (1,1,1)
                    dx_ext0 = dx0 - 1 - 3 * SQUISH_CONSTANT_3D;
                    dy_ext0 = dy0 - 1 - 3 * SQUISH_CONSTANT_3D;
                    dz_ext0 = dz0 - 1 - 3 * SQUISH_CONSTANT_3D;
                    xsv_ext0 = xsb + 1;
                    ysv_ext0 = ysb + 1;
                    zsv_ext0 = zsb + 1;

                    // Other extra point is based on the shared axis.
                    byte c = (byte) (aPoint & bPoint);
                    if ((c & 0x01) != 0) {
                        dx_ext1 = dx0 - 2 - 2 * SQUISH_CONSTANT_3D;
                        dy_ext1 = dy0 - 2 * SQUISH_CONSTANT_3D;
                        dz_ext1 = dz0 - 2 * SQUISH_CONSTANT_3D;
                        xsv_ext1 = xsb + 2;
                        ysv_ext1 = ysb;
                        zsv_ext1 = zsb;
                    } else if ((c & 0x02) != 0) {
                        dx_ext1 = dx0 - 2 * SQUISH_CONSTANT_3D;
                        dy_ext1 = dy0 - 2 - 2 * SQUISH_CONSTANT_3D;
                        dz_ext1 = dz0 - 2 * SQUISH_CONSTANT_3D;
                        xsv_ext1 = xsb;
                        ysv_ext1 = ysb + 2;
                        zsv_ext1 = zsb;
                    } else {
                        dx_ext1 = dx0 - 2 * SQUISH_CONSTANT_3D;
                        dy_ext1 = dy0 - 2 * SQUISH_CONSTANT_3D;
                        dz_ext1 = dz0 - 2 - 2 * SQUISH_CONSTANT_3D;
                        xsv_ext1 = xsb;
                        ysv_ext1 = ysb;
                        zsv_ext1 = zsb + 2;
                    }
                } else {// Both closest points on (0,0,0) side

                    // One of the two extra points is (0,0,0)
                    dx_ext0 = dx0;
                    dy_ext0 = dy0;
                    dz_ext0 = dz0;
                    xsv_ext0 = xsb;
                    ysv_ext0 = ysb;
                    zsv_ext0 = zsb;

                    // Other extra point is based on the omitted axis.
                    byte c = (byte) (aPoint | bPoint);
                    if ((c & 0x01) == 0) {
                        dx_ext1 = dx0 + 1 - SQUISH_CONSTANT_3D;
                        dy_ext1 = dy0 - 1 - SQUISH_CONSTANT_3D;
                        dz_ext1 = dz0 - 1 - SQUISH_CONSTANT_3D;
                        xsv_ext1 = xsb - 1;
                        ysv_ext1 = ysb + 1;
                        zsv_ext1 = zsb + 1;
                    } else if ((c & 0x02) == 0) {
                        dx_ext1 = dx0 - 1 - SQUISH_CONSTANT_3D;
                        dy_ext1 = dy0 + 1 - SQUISH_CONSTANT_3D;
                        dz_ext1 = dz0 - 1 - SQUISH_CONSTANT_3D;
                        xsv_ext1 = xsb + 1;
                        ysv_ext1 = ysb - 1;
                        zsv_ext1 = zsb + 1;
                    } else {
                        dx_ext1 = dx0 - 1 - SQUISH_CONSTANT_3D;
                        dy_ext1 = dy0 - 1 - SQUISH_CONSTANT_3D;
                        dz_ext1 = dz0 + 1 - SQUISH_CONSTANT_3D;
                        xsv_ext1 = xsb + 1;
                        ysv_ext1 = ysb + 1;
                        zsv_ext1 = zsb - 1;
                    }
                }
            } else { // One point on (0,0,0) side, one point on (1,1,1) side
                byte c1, c2;
                if (aIsFurtherSide) {
                    c1 = aPoint;
                    c2 = bPoint;
                } else {
                    c1 = bPoint;
                    c2 = aPoint;
                }

                // One contribution is a permutation of (1,1,-1)
                if ((c1 & 0x01) == 0) {
                    dx_ext0 = dx0 + 1 - SQUISH_CONSTANT_3D;
                    dy_ext0 = dy0 - 1 - SQUISH_CONSTANT_3D;
                    dz_ext0 = dz0 - 1 - SQUISH_CONSTANT_3D;
                    xsv_ext0 = xsb - 1;
                    ysv_ext0 = ysb + 1;
                    zsv_ext0 = zsb + 1;
                } else if ((c1 & 0x02) == 0) {
                    dx_ext0 = dx0 - 1 - SQUISH_CONSTANT_3D;
                    dy_ext0 = dy0 + 1 - SQUISH_CONSTANT_3D;
                    dz_ext0 = dz0 - 1 - SQUISH_CONSTANT_3D;
                    xsv_ext0 = xsb + 1;
                    ysv_ext0 = ysb - 1;
                    zsv_ext0 = zsb + 1;
                } else {
                    dx_ext0 = dx0 - 1 - SQUISH_CONSTANT_3D;
                    dy_ext0 = dy0 - 1 - SQUISH_CONSTANT_3D;
                    dz_ext0 = dz0 + 1 - SQUISH_CONSTANT_3D;
                    xsv_ext0 = xsb + 1;
                    ysv_ext0 = ysb + 1;
                    zsv_ext0 = zsb - 1;
                }

                // One contribution is a permutation of (0,0,2)
                dx_ext1 = dx0 - 2 * SQUISH_CONSTANT_3D;
                dy_ext1 = dy0 - 2 * SQUISH_CONSTANT_3D;
                dz_ext1 = dz0 - 2 * SQUISH_CONSTANT_3D;
                xsv_ext1 = xsb;
                ysv_ext1 = ysb;
                zsv_ext1 = zsb;
                if ((c2 & 0x01) != 0) {
                    dx_ext1 -= 2;
                    xsv_ext1 += 2;
                } else if ((c2 & 0x02) != 0) {
                    dy_ext1 -= 2;
                    ysv_ext1 += 2;
                } else {
                    dz_ext1 -= 2;
                    zsv_ext1 += 2;
                }
            }

            // Contribution (1,0,0)
            double dx1 = dx0 - 1 - SQUISH_CONSTANT_3D;
            double dy1 = dy0 - 0 - SQUISH_CONSTANT_3D;
            double dz1 = dz0 - 0 - SQUISH_CONSTANT_3D;
            double attn1 = 2 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1;
            if (attn1 > 0) {
                attn1 *= attn1;
                value += attn1 * attn1 * extrapolate(xsb + 1, ysb, zsb, dx1, dy1, dz1);
            }

            // Contribution (0,1,0)
            double dx2 = dx0 - 0 - SQUISH_CONSTANT_3D;
            double dy2 = dy0 - 1 - SQUISH_CONSTANT_3D;
            double attn2 = 2 - dx2 * dx2 - dy2 * dy2 - dz1 * dz1;
            if (attn2 > 0) {
                attn2 *= attn2;
                value += attn2 * attn2 * extrapolate(xsb, ysb + 1, zsb, dx2, dy2, dz1);
            }

            // Contribution (0,0,1)
            double dz3 = dz0 - 1 - SQUISH_CONSTANT_3D;
            double attn3 = 2 - dx2 * dx2 - dy1 * dy1 - dz3 * dz3;
            if (attn3 > 0) {
                attn3 *= attn3;
                value += attn3 * attn3 * extrapolate(xsb, ysb, zsb + 1, dx2, dy1, dz3);
            }

            // Contribution (1,1,0)
            double dx4 = dx0 - 1 - 2 * SQUISH_CONSTANT_3D;
            double dy4 = dy0 - 1 - 2 * SQUISH_CONSTANT_3D;
            double dz4 = dz0 - 0 - 2 * SQUISH_CONSTANT_3D;
            double attn4 = 2 - dx4 * dx4 - dy4 * dy4 - dz4 * dz4;
            if (attn4 > 0) {
                attn4 *= attn4;
                value += attn4 * attn4 * extrapolate(xsb + 1, ysb + 1, zsb, dx4, dy4, dz4);
            }

            // Contribution (1,0,1)
            double dy5 = dy0 - 0 - 2 * SQUISH_CONSTANT_3D;
            double dz5 = dz0 - 1 - 2 * SQUISH_CONSTANT_3D;
            double attn5 = 2 - dx4 * dx4 - dy5 * dy5 - dz5 * dz5;
            if (attn5 > 0) {
                attn5 *= attn5;
                value += attn5 * attn5 * extrapolate(xsb + 1, ysb, zsb + 1, dx4, dy5, dz5);
            }

            // Contribution (0,1,1)
            double dx6 = dx0 - 0 - 2 * SQUISH_CONSTANT_3D;
            double attn6 = 2 - dx6 * dx6 - dy4 * dy4 - dz5 * dz5;
            if (attn6 > 0) {
                attn6 *= attn6;
                value += attn6 * attn6 * extrapolate(xsb, ysb + 1, zsb + 1, dx6, dy4, dz5);
            }
        }

        // First extra vertex
        double attn_ext0 = 2 - dx_ext0 * dx_ext0 - dy_ext0 * dy_ext0 - dz_ext0 * dz_ext0;
        if (attn_ext0 > 0) {
            attn_ext0 *= attn_ext0;
            value += attn_ext0 * attn_ext0 * extrapolate(xsv_ext0, ysv_ext0, zsv_ext0, dx_ext0, dy_ext0, dz_ext0);
        }

        // Second extra vertex
        double attn_ext1 = 2 - dx_ext1 * dx_ext1 - dy_ext1 * dy_ext1 - dz_ext1 * dz_ext1;
        if (attn_ext1 > 0) {
            attn_ext1 *= attn_ext1;
            value += attn_ext1 * attn_ext1 * extrapolate(xsv_ext1, ysv_ext1, zsv_ext1, dx_ext1, dy_ext1, dz_ext1);
        }

        return value;
    }

    private double extrapolate(int xsb, int ysb, double dx, double dy) {
        Grad2 grad = permGrad2[perm[xsb & PMASK] ^ (ysb & PMASK)];
        return grad.dx * dx + grad.dy * dy;
    }

    private double extrapolate(int xsb, int ysb, int zsb, double dx, double dy, double dz) {
        Grad3 grad = permGrad3[perm[perm[xsb & PMASK] ^ (ysb & PMASK)] ^ (zsb & PMASK)];
        return grad.dx * dx + grad.dy * dy + grad.dz * dz;
    }

    private static int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }

    public static class Grad2 {
        double dx, dy;

        public Grad2(double dx, double dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    public static class Grad3 {
        double dx, dy, dz;

        public Grad3(double dx, double dy, double dz) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }
    }

    private static final double N2 = 7.69084574549313;
    private static final double N3 = 26.92263139946168;

    private static final Grad2[] GRADIENTS_2D = new Grad2[PSIZE];
    private static final Grad3[] GRADIENTS_3D = new Grad3[PSIZE];

    static {
        Grad2[] grad2 = {
                new Grad2(0.130526192220052, 0.99144486137381),
                new Grad2(0.38268343236509, 0.923879532511287),
                new Grad2(0.608761429008721, 0.793353340291235),
                new Grad2(0.793353340291235, 0.608761429008721),
                new Grad2(0.923879532511287, 0.38268343236509),
                new Grad2(0.99144486137381, 0.130526192220051),
                new Grad2(0.99144486137381, -0.130526192220051),
                new Grad2(0.923879532511287, -0.38268343236509),
                new Grad2(0.793353340291235, -0.60876142900872),
                new Grad2(0.608761429008721, -0.793353340291235),
                new Grad2(0.38268343236509, -0.923879532511287),
                new Grad2(0.130526192220052, -0.99144486137381),
                new Grad2(-0.130526192220052, -0.99144486137381),
                new Grad2(-0.38268343236509, -0.923879532511287),
                new Grad2(-0.608761429008721, -0.793353340291235),
                new Grad2(-0.793353340291235, -0.608761429008721),
                new Grad2(-0.923879532511287, -0.38268343236509),
                new Grad2(-0.99144486137381, -0.130526192220052),
                new Grad2(-0.99144486137381, 0.130526192220051),
                new Grad2(-0.923879532511287, 0.38268343236509),
                new Grad2(-0.793353340291235, 0.608761429008721),
                new Grad2(-0.608761429008721, 0.793353340291235),
                new Grad2(-0.38268343236509, 0.923879532511287),
                new Grad2(-0.130526192220052, 0.99144486137381)
        };
        for (Grad2 item : grad2) {
            item.dx /= N2;
            item.dy /= N2;
        }
        for (int i = 0; i < PSIZE; i++) {
            GRADIENTS_2D[i] = grad2[i % grad2.length];
        }

        Grad3[] grad3 = {
                new Grad3(-1.4082482904633333, -1.4082482904633333, -2.6329931618533333),
                new Grad3(-0.07491495712999985, -0.07491495712999985, -3.29965982852),
                new Grad3(0.24732126143473554, -1.6667938651159684, -2.838945207362466),
                new Grad3(-1.6667938651159684, 0.24732126143473554, -2.838945207362466),
                new Grad3(-1.4082482904633333, -2.6329931618533333, -1.4082482904633333),
                new Grad3(-0.07491495712999985, -3.29965982852, -0.07491495712999985),
                new Grad3(-1.6667938651159684, -2.838945207362466, 0.24732126143473554),
                new Grad3(0.24732126143473554, -2.838945207362466, -1.6667938651159684),
                new Grad3(1.5580782047233335, 0.33333333333333337, -2.8914115380566665),
                new Grad3(2.8914115380566665, -0.33333333333333337, -1.5580782047233335),
                new Grad3(1.8101897177633992, -1.2760767510338025, -2.4482280932803),
                new Grad3(2.4482280932803, 1.2760767510338025, -1.8101897177633992),
                new Grad3(1.5580782047233335, -2.8914115380566665, 0.33333333333333337),
                new Grad3(2.8914115380566665, -1.5580782047233335, -0.33333333333333337),
                new Grad3(2.4482280932803, -1.8101897177633992, 1.2760767510338025),
                new Grad3(1.8101897177633992, -2.4482280932803, -1.2760767510338025),
                new Grad3(-2.6329931618533333, -1.4082482904633333, -1.4082482904633333),
                new Grad3(-3.29965982852, -0.07491495712999985, -0.07491495712999985),
                new Grad3(-2.838945207362466, 0.24732126143473554, -1.6667938651159684),
                new Grad3(-2.838945207362466, -1.6667938651159684, 0.24732126143473554),
                new Grad3(0.33333333333333337, 1.5580782047233335, -2.8914115380566665),
                new Grad3(-0.33333333333333337, 2.8914115380566665, -1.5580782047233335),
                new Grad3(1.2760767510338025, 2.4482280932803, -1.8101897177633992),
                new Grad3(-1.2760767510338025, 1.8101897177633992, -2.4482280932803),
                new Grad3(0.33333333333333337, -2.8914115380566665, 1.5580782047233335),
                new Grad3(-0.33333333333333337, -1.5580782047233335, 2.8914115380566665),
                new Grad3(-1.2760767510338025, -2.4482280932803, 1.8101897177633992),
                new Grad3(1.2760767510338025, -1.8101897177633992, 2.4482280932803),
                new Grad3(3.29965982852, 0.07491495712999985, 0.07491495712999985),
                new Grad3(2.6329931618533333, 1.4082482904633333, 1.4082482904633333),
                new Grad3(2.838945207362466, -0.24732126143473554, 1.6667938651159684),
                new Grad3(2.838945207362466, 1.6667938651159684, -0.24732126143473554),
                new Grad3(-2.8914115380566665, 1.5580782047233335, 0.33333333333333337),
                new Grad3(-1.5580782047233335, 2.8914115380566665, -0.33333333333333337),
                new Grad3(-2.4482280932803, 1.8101897177633992, -1.2760767510338025),
                new Grad3(-1.8101897177633992, 2.4482280932803, 1.2760767510338025),
                new Grad3(-2.8914115380566665, 0.33333333333333337, 1.5580782047233335),
                new Grad3(-1.5580782047233335, -0.33333333333333337, 2.8914115380566665),
                new Grad3(-1.8101897177633992, 1.2760767510338025, 2.4482280932803),
                new Grad3(-2.4482280932803, -1.2760767510338025, 1.8101897177633992),
                new Grad3(0.07491495712999985, 3.29965982852, 0.07491495712999985),
                new Grad3(1.4082482904633333, 2.6329931618533333, 1.4082482904633333),
                new Grad3(1.6667938651159684, 2.838945207362466, -0.24732126143473554),
                new Grad3(-0.24732126143473554, 2.838945207362466, 1.6667938651159684),
                new Grad3(0.07491495712999985, 0.07491495712999985, 3.29965982852),
                new Grad3(1.4082482904633333, 1.4082482904633333, 2.6329931618533333),
                new Grad3(-0.24732126143473554, 1.6667938651159684, 2.838945207362466),
                new Grad3(1.6667938651159684, -0.24732126143473554, 2.838945207362466)
        };

        for (Grad3 value : grad3) {
            value.dx /= N3;
            value.dy /= N3;
            value.dz /= N3;
        }

        for (int i = 0; i < PSIZE; i++) {
            GRADIENTS_3D[i] = grad3[i % grad3.length];
        }
    }
}