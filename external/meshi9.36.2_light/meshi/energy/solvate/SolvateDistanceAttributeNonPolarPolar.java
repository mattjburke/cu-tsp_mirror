/*
 * Copyright (c) 2010. of Chen Keasar, BGU . For free use under LGPL
 */

package meshi.energy.solvate;

import meshi.geometry.Distance;
import meshi.util.mathTools.Sigma;


public class SolvateDistanceAttributeNonPolarPolar extends SolvateDistanceAttribute {
    public final Distance distance;
    public final Sigma sigma = new Sigma();
    public final SolvateDistanceType type() {
        return SolvateDistanceType.HYDROPHOBIC_POLAR;
    }
    //public final int atom1number, atom2number;

    public final SigmaParameters sigmaParameters_21;


    // Carbon sigmoids and related derivatives

    public double sigmCa2;     /*1*/
    public double dsigmCa2dx;/*2*/
    public double dsigmCa2dy;/*2*/
    public double dsigmCa2dz;/*2*/


    public SolvateDistanceAttributeNonPolarPolar(Distance dis,
                                                 SigmaParameters sigmaParameters_21) {

        this.distance = dis;

        this.sigmaParameters_21 = sigmaParameters_21;
        resetSigmVals();
    }

    public final void update() {
        double sigmas_tag;

        // Handling Carbons
        // ----------------
        // Is atom1 non-polar? If so it should contribute to atom2's CNC
        sigma.sigma(distance.distance(), sigmaParameters_21.end,
                sigmaParameters_21.p1,
                sigmaParameters_21.p2,
                sigmaParameters_21.valAtp1,
                sigmaParameters_21.valAtp2);
        sigmCa2 = sigma.s();
        sigmas_tag = sigma.s_tag();
        dsigmCa2dx = sigmas_tag * distance.dDistanceDx();
        dsigmCa2dy = sigmas_tag * distance.dDistanceDy();
        dsigmCa2dz = sigmas_tag * distance.dDistanceDz();
    } // of update


    public final void resetSigmVals() {
        sigmCa2 = 0.0;
        dsigmCa2dx = 0.0;
        dsigmCa2dy = 0.0;
        dsigmCa2dz = 0.0;
    }
}