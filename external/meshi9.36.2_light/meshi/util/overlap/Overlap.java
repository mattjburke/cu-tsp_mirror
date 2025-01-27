/*
 * Copyright (c) 2010. of Chen Keasar, BGU . For free use under LGPL
 */

package meshi.util.overlap;

import meshi.util.*;


/**
 * Kabsch algorithm for optimal superposition.
 * The following class implements the procedure for obtaining<BR>
 * the best rotation to relate two sets of vectors as described <BR>
 * in papers:<BR>
 * 1. "A solution for the best rotation to relate two sets of vectors". By Wolfgang Kabsch, Acta Cryst. (1976) A 32: 922-923<BR>
 * 2. "A discussion of the solution for the best rotation to relate two sets<BR>
 * of vectors". By Wolfgang Kabsch,  Acta Cryst. (1978). A34, 827-828 .
 */

public class Overlap {

    private double[][] coor;
    private double[][] coor2;
    private double[][] temp;
    private String comment;
    private String comment2;
    private int npt;
    private double[][] help;
    private double[] eigenv;
    private double[][] eigenVectors;
    private double[][] bvector;
    private double[][] Umatrix;
    private double eps = Math.pow(10, -10);
    private double rms;

    public Overlap(double[][] co, double[][] co2) {
        initiateFields(co, co2, co[0].length, "", "");
    }
    public Overlap(double[][] co, double[][] co2, int n, String com, String com2) {
        initiateFields(co, co2, n, com, com2);
        gravityCenter();
        double[] a = calcCharPol(createP());
        if (Double.isNaN(a[0]))
            throw new RuntimeException("This is weird");

        double[] b = findEigenval(a);
        if (Double.isNaN(b[0]))
            throw new RuntimeException("This is weird");
        checkEigenval(b);
        checkEigenVec();
        createUmatrix(calcBvectors());
        calculateRms();
    }

    public double[][] rotationMatrix() {
        return Umatrix;
    }

    public double rms() {
        return rms;
    }


    /**
     * A function that initializes all the class fields so that<BR> all the other functions can use them
     */

    private void initiateFields(double[][] co, double[][] co2, int n, String com, String com2) {
        coor = co;
        coor2 = co2;
        comment = com;
        comment2 = com2;
        npt = n;//align.length()


    }//initialFields


    /**
     * Uniting the gravity center of both proteins to (0,0,0)
     */

    public void gravityCenter() {

        double xcm1 = 0, ycm1 = 0, zcm1 = 0;
        double xcm2 = 0, ycm2 = 0, zcm2 = 0;


        /*calculating xc1m , xcm2, ycm1, ycm2, zcm1, zcm2*/
        for (int i = 0; i < npt; i++) {
            xcm1 = xcm1 + coor[0][i];
            xcm2 = xcm2 + coor2[0][i];
            ycm1 = ycm1 + coor[1][i];
            ycm2 = ycm2 + coor2[1][i];
            zcm1 = zcm1 + coor[2][i];
            zcm2 = zcm2 + coor2[2][i];
        }

        xcm1 = xcm1 / npt;
        xcm2 = xcm2 / npt;
        ycm1 = ycm1 / npt;
        ycm2 = ycm2 / npt;
        zcm1 = zcm1 / npt;
        zcm2 = zcm2 / npt;

        /*finished calculating center of mass*/
        /*now we have to move the two proteins to the calculated center*/
        for (int i = 0; i < npt; i++) {
            coor[0][i] = coor[0][i] - xcm1;
            coor2[0][i] = coor2[0][i] - xcm2;
            coor[1][i] = coor[1][i] - ycm1;
            coor2[1][i] = coor2[1][i] - ycm2;
            coor[2][i] = coor[2][i] - zcm1;
            coor2[2][i] = coor2[2][i] - zcm2;
        }


    }//gravity

    /**
     * Building the R matrix according to equation 7<BR>
     * at the Kabsch paper (1976)
     */

    public double[][] createR() {
        double[][] R = new double[3][3];
        double[] weight = new double[npt];//this array contain the atom weights, at this point all the weights equal to 1 and the weight array should become a global data member.
        for (int i = 0; i < npt; i++)
            weight[i] = 1;

        for (int i = 0; i < npt; i++) {
            R[0][0] = R[0][0] + coor2[0][i] * coor[0][i] * weight[i];
            R[0][1] = R[0][1] + coor2[0][i] * coor[1][i] * weight[i];
            R[0][2] = R[0][2] + coor2[0][i] * coor[2][i] * weight[i];
            R[1][0] = R[1][0] + coor2[1][i] * coor[0][i] * weight[i];
            R[1][1] = R[1][1] + coor2[1][i] * coor[1][i] * weight[i];
            R[1][2] = R[1][2] + coor2[1][i] * coor[2][i] * weight[i];
            R[2][0] = R[2][0] + coor2[2][i] * coor[0][i] * weight[i];
            R[2][1] = R[2][1] + coor2[2][i] * coor[1][i] * weight[i];
            R[2][2] = R[2][2] + coor2[2][i] * coor[2][i] * weight[i];
        }

        return R;
    }//createR

    /**
     * Building a P  matrix according to equation 10<BR>
     * which is  R*Rt(1976)
     */

    public double[][] createP() {

        double[][] P = new double[3][3];
        double[][] temp = createR();
        double[][] Rt = new double[3][3];//calculating the transposed matrix
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++)
                Rt[i][j] = temp[j][i];
        }


        P[0][0] = (Rt[0][0] * temp[0][0] + Rt[0][1] * temp[1][0] + Rt[0][2] * temp[2][0]);
        P[0][1] = (Rt[0][0] * temp[0][1] + Rt[0][1] * temp[1][1] + Rt[0][2] * temp[2][1]);
        P[0][2] = (Rt[0][0] * temp[0][2] + Rt[0][1] * temp[1][2] + Rt[0][2] * temp[2][2]);
        P[1][0] = (Rt[1][0] * temp[0][0] + Rt[1][1] * temp[1][0] + Rt[1][2] * temp[2][0]);
        P[1][1] = (Rt[1][0] * temp[0][1] + Rt[1][1] * temp[1][1] + Rt[1][2] * temp[2][1]);
        P[1][2] = (Rt[1][0] * temp[0][2] + Rt[1][1] * temp[1][2] + Rt[1][2] * temp[2][2]);
        P[2][0] = (Rt[2][0] * temp[0][0] + Rt[2][1] * temp[1][0] + Rt[2][2] * temp[2][0]);
        P[2][1] = (Rt[2][0] * temp[0][1] + Rt[2][1] * temp[1][1] + Rt[2][2] * temp[2][1]);
        P[2][2] = (Rt[2][0] * temp[0][2] + Rt[2][1] * temp[1][2] + Rt[2][2] * temp[2][2]);


        help = P;

        return P;

    }

    /**
     * this function will calculate the characteristic polynom
     * after caululating the determinant manually
     */

    public double[] calcCharPol(double[][] mat) {
        double[] charPol = new double[4];
        charPol[0] = -1;//the coefficient of x^3
        charPol[1] = mat[0][0] + mat[1][1] + mat[2][2];//the coefficient of x^2
        charPol[2] = mat[0][1] * mat[1][0] + mat[2][0] * mat[0][2] + mat[1][2] * mat[2][1] - mat[0][0] * mat[1][1] - mat[0][0] * mat[2][2] - mat[1][1] * mat[2][2];
        charPol[3] = mat[0][0] * mat[1][1] * mat[2][2] - mat[0][0] * mat[1][2] * mat[2][1] - mat[0][1] * mat[1][0] * mat[2][2] + mat[0][1] * mat[2][0] * mat[1][2] + mat[0][2] * mat[1][0] * mat[2][1] - mat[0][2] * mat[2][0] * mat[1][1];


        return charPol;
    }


    /**
     * In order to find the 3 eigenValues of P, which is <BR>
     * a positive definite matrix, we are solving a cubic equation
     */

    public double[] findEigenval(double[] charpol) {
        double[] eigenval = new double[3];//this is an array that will contain the eigenvalues

        /*solving a cubic equation*/
        double a = charpol[0];
        double b = charpol[1];
        double c = charpol[2];
        double d = charpol[3];
        double f = ((3 * c) / a - (b * b) / (a * a)) / 3;
        double g = (((2 * b * b * b) / (a * a * a)) - (9 * b * c / (a * a)) + 27 * d / a) / 27;
        double h = (g * g / 4) + (f * f * f / 27);


        if (h > eps)
            throw new MeshiException("this is a positive definite matrix, all eigenvalues must be real numbers");

        if (h < eps) {

            double i = Math.sqrt((g * g) / 4 - h);
            double t = (double) 1 / 3;
            double j = Math.pow(i, t);
            double k;
            if (-(g / (2 * i)) > 1)
                k = 0;
            else if (-(g / (2 * i)) < -1)
                k = Math.PI;
            else
                k = Math.acos(-(g / (2 * i)));
            double L = j * (-1);
            double M = Math.cos(k / 3);
            double q = (double) 3;
            double N = Math.sqrt(q) * Math.sin(k / 3);
            double P = (b / 3 * a) * (-1);

            eigenval[0] = 2 * j * Math.cos(k / 3) - (b / 3 * a);
            eigenval[1] = L * (M + N) + P;
            eigenval[2] = L * (M - N) + P;
            if (Double.isNaN(eigenval[0]))
                throw new RuntimeException("This is weird");

        }

        eigenval = sortEigenval(eigenval);

        eigenv = eigenval;
        if (Double.isNaN(eigenv[0]))
                throw new RuntimeException("This is weird");


        return eigenval;


    }//findEigenVal

    /**
     * this function sorts the eigenvalues in descending
     * order, according to Kabsch(1978) so we obtain the
     * proper rotation matrix
     */


    private double[] sortEigenval(double[] eigenval) {

        double[] sortedVals = new double[3];
        sortedVals[0] = findMax(eigenval[0], findMax(eigenval[1], eigenval[2]));//finding the largest eigval
        sortedVals[2] = findMin(eigenval[0], findMin(eigenval[1], eigenval[2]));//finding the smallest eigcval
        for (int i = 0; i < 3; i++) {
            if (eigenval[i] != sortedVals[0] && eigenval[i] != sortedVals[2])
                sortedVals[1] = eigenval[i];
        }

        return sortedVals;
    }

    /**
     * We now have to find the corresponding eigenvectors
     * so we check if all eigenvalues are different or not.
     * Then sending the eigenvalues to a function that calculates
     * the vectors
     */


    public void checkEigenval(double[] eigenval) {
        if (Math.abs(eigenval[0] - eigenval[1]) > eps && Math.abs(eigenval[1] - eigenval[2]) > eps && Math.abs(eigenval[0] - eigenval[2]) > eps) {
            findEigenvec1(eigenval);
        } else {
            findEigenvec2(eigenval);
        }

    }//checkEigenval

    /*****************************FIND EIGEN VEC 1*********************************************/
    /**
     * Incase all 3 eigenvalues are different (most cases)
     * in order to derive the eigenvectors we use the
     * Gauss elimination procedure with scaled pivoting
     */


    public double[][] findEigenvec1(double[] eigenval) {
        double[][] eigenvec = new double[3][3];//each column in this matrix will represent one eigenvector
        double[][] temp = new double[3][3];//this matrix is used each time for a different eigenval.

        /*THIS BIG LOOP WILL BE USED FOR ALL 3 EIGENVALUES, incase that all 3 are different*/


        for (int i = 0; i < 3; i++) {//each iteration will find one eigenvec
            temp = copyMat(help);
            for (int j = 0; j < 3; j++)
                temp[j][j] = temp[j][j] - eigenval[i];//assigning the eigen val into |A-lambda*I|


            double max1 = findMaxAbs(temp[0][0], findMaxAbs(temp[0][1], temp[0][2]));//finds the max of the first row
            double max2 = findMaxAbs(temp[1][0], findMaxAbs(temp[1][1], temp[1][2]));//finds the max of the second
            double max3 = findMaxAbs(temp[2][0], findMaxAbs(temp[2][1], temp[2][2]));//finds the max of the third


            double checkMax1 = temp[0][0] / max1;
            double checkMax2 = temp[1][0] / max2;
            double checkMax3 = temp[2][0] / max3;
            int maxIndex1 = 0;
            double maxVal = findMaxAbs(checkMax1, findMaxAbs(checkMax2, checkMax3));
            if (maxVal == Math.abs(checkMax2))
                maxIndex1 = 1;
            if (maxVal == Math.abs(checkMax3))
                maxIndex1 = 2;


            /*nullification of the first argument in each row but the one with maxIndex*/

            for (int k = 0; k < 3; k++) {
                if (k != maxIndex1) {
                    double l = temp[k][0] / temp[maxIndex1][0];
                    for (int m = 0; m < 3; m++)
                        temp[k][m] = temp[maxIndex1][m] * l - temp[k][m];


                }
            }
            //dividing the second argumnet in each row (but the max Index row)  in the max of the row
            checkMax1 = temp[0][1] / max1;
            checkMax2 = temp[1][1] / max2;
            checkMax3 = temp[2][1] / max3;

            //finding the second row that has to stay in place
            int maxIndex2 = 0;

            if (maxIndex1 == 0) {
                if (Math.abs(checkMax2) > Math.abs(checkMax3))
                    maxIndex2 = 1;
                else
                    maxIndex2 = 2;
            }
            if (maxIndex1 == 1) {
                if (Math.abs(checkMax1) > Math.abs(checkMax3))
                    maxIndex2 = 0;
                else
                    maxIndex2 = 2;
            }
            if (maxIndex1 == 2) {
                if (Math.abs(checkMax1) > Math.abs(checkMax2))
                    maxIndex2 = 0;
                else
                    maxIndex2 = 1;
            }

            for (int p = 0; p < 3; p++) { //replace all values that are less than eps with zero
                for (int q = 0; q < 3; q++) {
                    if (Math.abs(temp[p][q]) < eps)
                        temp[p][q] = 0;
                }
            }


            if (temp[maxIndex2][1] == 0 && temp[maxIndex1][0] != 0) {
                eigenvec[2][i] = 0;
                eigenvec[1][i] = 1;
                eigenvec[0][i] = -temp[maxIndex1][1] / temp[maxIndex1][0];
            }
            if (temp[maxIndex2][1] != 0 && temp[maxIndex1][0] != 0) {
                eigenvec[2][i] = 1;
                eigenvec[1][i] = -temp[maxIndex2][2] / temp[maxIndex2][1];
                eigenvec[0][i] = (((temp[maxIndex2][2] * temp[maxIndex1][1]) / temp[maxIndex2][1]) - temp[maxIndex1][2]) / temp[maxIndex1][0];


            }

            if (temp[maxIndex2][1] == 0 && temp[maxIndex1][0] == 0) {
                eigenvec[2][i] = 0;
                eigenvec[1][i] = 0;
                eigenvec[0][i] = 1;
            }

            if (temp[maxIndex2][1] != 0 && temp[maxIndex1][0] == 0) {
                eigenvec[2][i] = 0;
                eigenvec[1][i] = 0;
                eigenvec[0][i] = 1;
            }

            double n = Math.sqrt(eigenvec[0][i] * eigenvec[0][i] + eigenvec[1][i] * eigenvec[1][i] + eigenvec[2][i] * eigenvec[2][i]);
            for (int r = 0; r < 3; r++)
                eigenvec[r][i] = eigenvec[r][i] / n;

            eigenVectors = eigenvec;


        }//BIG FOR


        eigenVectors = eigenvec;
        return eigenvec;
    }//findEigenvec
    /******************************************************************************************/


    /**
     * **************************FIND EIGEN VEC 2********************************************
     */


    public double[][] findEigenvec2(double[] eigenval) {


        double[][] eigenvec = new double[3][3];
        double[][] temp2 = new double[3][3];
        double[] helpArray = new double[3];
        temp2 = copyMat(help);

        /*first we have to find the two equal eigen values*/
        for (int k = 0; k < 3; k++)
            helpArray[k] = eigenval[k];


        double same = helpArray[0];
        if (Math.abs(eigenval[0] - eigenval[1]) < eps)
            helpArray[1] = same;


        if (Math.abs(eigenval[1] - eigenval[2]) < eps) {
            same = eigenval[1];
            helpArray[2] = same;
        } else
            helpArray[2] = same;

        /*now we will find two eigenvectors for the double eigen value that we have*/
        for (int i = 0; i < 3; i++)
            temp2[i][i] = temp2[i][i] - same;


        for (int p = 0; p < 3; p++) { //replace all values that are less than eps with zero
            for (int q = 0; q < 3; q++) {
                if (Math.abs(temp2[p][q]) < eps)
                    temp2[p][q] = 0;
            }
        }

        if (temp2[0][0] != 0) {
            eigenvec[0][0] = -temp2[0][2] / temp2[0][0];
            eigenvec[1][0] = 0;
            eigenvec[2][0] = 1;
            eigenvec[0][1] = -temp2[0][1] / temp2[0][0];
            eigenvec[1][1] = 1;
            eigenvec[2][1] = 0;
        }

        if (temp2[0][0] == 0) {

            eigenvec[0][0] = 1;
            eigenvec[1][0] = -temp2[0][2] / temp2[0][1];
            eigenvec[2][0] = 1;
            eigenvec[0][1] = 0;
            eigenvec[1][1] = -temp2[0][2] / temp2[0][1];
            eigenvec[2][1] = 1;

        }
        double n1 = Math.sqrt(eigenvec[0][0] * eigenvec[0][0] + eigenvec[1][0] * eigenvec[1][0] + eigenvec[2][0] * eigenvec[2][0]);


        for (int i = 0; i < 3; i++)
            eigenvec[i][0] = eigenvec[i][0] / n1;
        /*I would like the two vectors of the same eigenval to be orthonormal and there for I
    will use the graham-shmidt prosidure*/
        double v1v2 = (eigenvec[0][0] * eigenvec[0][1]) + (eigenvec[1][0] * eigenvec[1][1]) + (eigenvec[2][0] * eigenvec[2][1]);
        for (int j = 0; j < 3; j++)
            eigenvec[j][1] = eigenvec[j][1] - eigenvec[j][0] * v1v2;

        double n2 = Math.sqrt(eigenvec[0][1] * eigenvec[0][1] + eigenvec[1][1] * eigenvec[1][1] + eigenvec[2][1] * eigenvec[2][1]);

        for (int k = 0; k < 3; k++)
            eigenvec[k][1] = eigenvec[k][1] / n2;

        //finding the eigen value that is different

        for (int i = 0; i < 3; i++) {
            if (eigenval[i] != same)
                helpArray[2] = eigenval[i];

        }


        double[][] temp3 = new double[3][3];
        temp3 = copyMat(help);
        for (int j = 0; j < 3; j++)
            temp3[j][j] = temp3[j][j] - helpArray[2];//assigning the eigen value

        for (int i = 0; i < 3; i++) {
            System.out.println("\n");
            for (int j = 0; j < 3; j++)
                System.out.print(temp3[i][j] + "\t");
            System.out.println(" ");
        }

        double max1 = findMaxAbs(temp3[0][0], findMaxAbs(temp3[0][1], temp3[0][2]));//finds the max of the 1`st row
        double max2 = findMaxAbs(temp3[1][0], findMaxAbs(temp3[1][1], temp3[1][2]));//finds the max of the second
        double max3 = findMaxAbs(temp3[2][0], findMaxAbs(temp3[2][1], temp3[2][2]));//finds the max of the third

        double checkMax1 = temp3[0][0] / max1;
        double checkMax2 = temp3[1][0] / max2;
        double checkMax3 = temp3[2][0] / max3;
        int maxIndex1 = 0;//this index will indicate which line doesn't change
        double maxVal = findMaxAbs(checkMax1, findMaxAbs(checkMax2, checkMax3));
        if (maxVal == Math.abs(checkMax2))
            maxIndex1 = 1;
        if (maxVal == Math.abs(checkMax3))
            maxIndex1 = 2;


        for (int k = 0; k < 3; k++) {
            if (k != maxIndex1) {
                double l = temp3[k][0] / temp3[maxIndex1][0];
                for (int m = 0; m < 3; m++)
                    temp3[k][m] = temp3[maxIndex1][m] * l - temp3[k][m];
            }
        }


        //dividing the second argumnet in each row (but the max Index row in the max of the row)

        //finding the second row that has to stay in place
        checkMax1 = temp3[0][1] / max1;
        checkMax2 = temp3[1][1] / max2;
        checkMax3 = temp3[2][1] / max3;
        int maxIndex2 = 0;
        if (maxIndex1 == 0) {
            if (Math.abs(checkMax2) > Math.abs(checkMax3))
                maxIndex2 = 1;
            else
                maxIndex2 = 2;
        }
        if (maxIndex1 == 1) {
            if (Math.abs(checkMax1) > Math.abs(checkMax3))
                maxIndex2 = 0;
            else
                maxIndex2 = 2;
        }
        if (maxIndex1 == 2) {
            if (Math.abs(checkMax1) > Math.abs(checkMax2))
                maxIndex2 = 0;
            else
                maxIndex2 = 1;
        }


        for (int p = 0; p < 3; p++) { //replace all values that are less than eps with zero
            for (int q = 0; q < 3; q++) {
                if (Math.abs(temp3[p][q]) < eps)
                    temp3[p][q] = 0;
            }
        }

        if (temp3[maxIndex2][1] == 0 && temp3[maxIndex1][0] != 0) {
            eigenvec[2][2] = 0;
            eigenvec[1][2] = 1;
            eigenvec[0][2] = -temp3[maxIndex1][1] / temp3[maxIndex1][0];
        }
        if (temp3[maxIndex2][1] != 0 && temp3[maxIndex1][0] != 0) {
            eigenvec[2][2] = 1;
            eigenvec[1][2] = -temp3[maxIndex2][2] / temp3[maxIndex2][1];
            eigenvec[0][2] = (((temp3[maxIndex2][2] * temp3[maxIndex1][1]) / temp3[maxIndex2][1]) - temp3[maxIndex1][2]) / temp3[maxIndex1][0];

        }
        if (temp3[maxIndex2][1] == 0 && temp3[maxIndex1][0] == 0) {
            eigenvec[2][2] = 0;
            eigenvec[1][2] = 0;
            eigenvec[0][2] = 1;
        }

        if (temp3[maxIndex2][1] != 0 && temp3[maxIndex1][0] == 0) {
            eigenvec[2][2] = 0;
            eigenvec[1][2] = 0;
            eigenvec[0][2] = 1;
        }
        double n3 = Math.sqrt(eigenvec[0][2] * eigenvec[0][2] + eigenvec[1][2] * eigenvec[1][2] + eigenvec[2][2] * eigenvec[2][2]);

        for (int i = 0; i < 3; i++)
            eigenvec[i][2] = eigenvec[i][2] / n3;


        eigenVectors = eigenvec;

        for (int i = 0; i < 3; i++) {
            System.out.println("\n");
            for (int j = 0; j < 3; j++) {
                System.out.print(eigenvec[i][j] + "\t");
            }
            System.out.println(" ");
        }


        return eigenvec;


    }//findEigenvec2


    /*****************************************************************************************/
    /**
     * In order to get the proper rotation matrix (kabsch 1978)
     * when a1,a2,a3 are out eigenvectors
     * we make sure that a1*a2 = a3
     */

    public void checkEigenVec() {

        double[] vecMul = new double[3];
        vecMul[0] = (eigenVectors[1][0] * eigenVectors[2][1]) - (eigenVectors[1][1] * eigenVectors[2][0]);
        vecMul[1] = (eigenVectors[0][1] * eigenVectors[2][0]) - (eigenVectors[0][0] * eigenVectors[2][1]);
        vecMul[2] = (eigenVectors[0][0] * eigenVectors[1][1]) - (eigenVectors[0][1] * eigenVectors[1][0]);


        double norm = Math.sqrt((vecMul[0] - eigenVectors[0][2]) * (vecMul[0] - eigenVectors[0][2]) + (vecMul[1] - eigenVectors[1][2]) * (vecMul[1] - eigenVectors[1][2]) + (vecMul[2] - eigenVectors[2][2]) * (vecMul[2] - eigenVectors[2][2]));
        eigenVectors[0][2] = vecMul[0];
        eigenVectors[1][2] = vecMul[1];
        eigenVectors[2][2] = vecMul[2];

    }

    /**
     * Calculating the B vectors (equation 12 at Kabsch-1976)
     */


    public double[][] calcBvectors() {

        double[][] bVectors = new double[3][3];
        double[][] r = createR();

        if (Double.isNaN(eigenv[0]) |(eigenv[0] <= 0))
            throw new RuntimeException("This is weird");

        for (int i = 0; i < 3; i++) {
            bVectors[0][i] = (r[0][0] * eigenVectors[0][i] + r[0][1] * eigenVectors[1][i] + r[0][2] * eigenVectors[2][i]) / Math.sqrt(eigenv[i]);
            bVectors[1][i] = (r[1][0] * eigenVectors[0][i] + r[1][1] * eigenVectors[1][i] + r[1][2] * eigenVectors[2][i]) / Math.sqrt(eigenv[i]);
            bVectors[2][i] = (r[2][0] * eigenVectors[0][i] + r[2][1] * eigenVectors[1][i] + r[2][2] * eigenVectors[2][i]) / Math.sqrt(eigenv[i]);
        }
        if (Double.isNaN(bVectors[0][0]))
            throw new RuntimeException("This is weird");
        double n1 = Math.sqrt((bVectors[0][0] * bVectors[0][0]) + (bVectors[1][0] * bVectors[1][0]) + (bVectors[2][0] * bVectors[2][0]));
        double n2 = Math.sqrt((bVectors[0][1] * bVectors[0][1]) + (bVectors[1][1] * bVectors[1][1]) + (bVectors[2][1] * bVectors[2][1]));
        if ((n1 == 0) | (n2 == 0) | Double.isNaN(n1) | Double.isNaN(n2) | Double.isInfinite(n1) | Double.isInfinite(n2))
            throw new RuntimeException("This is weird");

        for (int i = 0; i < 3; i++)
            bVectors[i][0] = bVectors[i][0] / n1;
        for (int i = 0; i < 3; i++)
            bVectors[i][1] = bVectors[i][1] / n2;

        bVectors[0][2] = (bVectors[1][0] * bVectors[2][1]) - (bVectors[1][1] * bVectors[2][0]);
        bVectors[1][2] = (bVectors[0][1] * bVectors[2][0]) - (bVectors[0][0] * bVectors[2][1]);
        bVectors[2][2] = (bVectors[0][0] * bVectors[1][1]) - (bVectors[0][1] * bVectors[1][0]);

if (Double.isNaN(bVectors[0][1]))
    throw new RuntimeException("This is weird.");
        return bVectors;
    }

    /**
     * Creating the U matrix, the rotation matrix.
     * Please note that since we switched the X and the Y atoms
     * when creating the R matrix, we don't return U but the transposed
     * of U
     */

    public void createUmatrix(double[][] bVectors) {
        double[][] u = new double[3][3];
        u[0][0] = bVectors[0][0] * eigenVectors[0][0] + bVectors[0][1] * eigenVectors[0][1] + bVectors[0][2] * eigenVectors[0][2];
        u[0][1] = bVectors[0][0] * eigenVectors[1][0] + bVectors[0][1] * eigenVectors[1][1] + bVectors[0][2] * eigenVectors[1][2];
        u[0][2] = bVectors[0][0] * eigenVectors[2][0] + bVectors[0][1] * eigenVectors[2][1] + bVectors[0][2] * eigenVectors[2][2];
        u[1][0] = bVectors[1][0] * eigenVectors[0][0] + bVectors[1][1] * eigenVectors[0][1] + bVectors[1][2] * eigenVectors[0][2];
        u[1][1] = bVectors[1][0] * eigenVectors[1][0] + bVectors[1][1] * eigenVectors[1][1] + bVectors[1][2] * eigenVectors[1][2];
        u[1][2] = bVectors[1][0] * eigenVectors[2][0] + bVectors[1][1] * eigenVectors[2][1] + bVectors[1][2] * eigenVectors[2][2];
        u[2][0] = bVectors[2][0] * eigenVectors[0][0] + bVectors[2][1] * eigenVectors[0][1] + bVectors[2][2] * eigenVectors[0][2];
        u[2][1] = bVectors[2][0] * eigenVectors[1][0] + bVectors[2][1] * eigenVectors[1][1] + bVectors[2][2] * eigenVectors[1][2];
        u[2][2] = bVectors[2][0] * eigenVectors[2][0] + bVectors[2][1] * eigenVectors[2][1] + bVectors[2][2] * eigenVectors[2][2];
        if (Double.isNaN(u[0][0]))
            throw new RuntimeException("This is weird");

        double[][] tmp = new double[3][3];
        for (int t = 0; t < 3; t++) {
            for (int k = 0; k < 3; k++)
                tmp[t][k] = u[k][t];
        }


        Umatrix = tmp;
    }//createUmatrix


    public void calculateRms() {
        temp = new double[3][npt];


        /*we multiply the second protein in U and then caculate the rms */
        // double rms;
        for (int i = 0; i < npt; i++) {
            temp[0][i] = (Umatrix[0][0] * coor2[0][i]) + (Umatrix[0][1] * coor2[1][i]) + (Umatrix[0][2] * coor2[2][i]);
            temp[1][i] = (Umatrix[1][0] * coor2[0][i]) + (Umatrix[1][1] * coor2[1][i]) + (Umatrix[1][2] * coor2[2][i]);
            temp[2][i] = (Umatrix[2][0] * coor2[0][i]) + (Umatrix[2][1] * coor2[1][i]) + (Umatrix[2][2] * coor2[2][i]);
        }
        if (Double.isNaN(Umatrix[0][0]))
            throw new RuntimeException("This is weird");

        coor2 = temp;

        double d = 0;
        for (int j = 0; j < npt; j++) {
            d = d + ((coor2[0][j] - coor[0][j]) * (coor2[0][j] - coor[0][j]) + (coor2[1][j] - coor[1][j]) * (coor2[1][j] - coor[1][j]) + (coor2[2][j] - coor[2][j]) * (coor2[2][j] - coor[2][j]));

        }
        rms = Math.sqrt(d / npt);
    }//rms()


    /**
     * *************some help functions******************************************************
     */


    public double findMaxAbs(double a, double b) {//a simple function that helps finding the max of 3 nums
        if (Math.abs(a) >= Math.abs(b))
            return Math.abs(a);
        else
            return Math.abs(b);
    }

    public static double findMin(double a, double b) {
        if (a <= b)
            return a;
        else
            return b;
    }

    public double findMax(double a, double b) {
        if (a >= b)
            return a;
        else
            return b;

    }


    public double[][] copyMat(double[][] mat) {//a simple function that copies values from one mat to another.
        double[][] mat2 = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++)
                mat2[i][j] = mat[i][j];

        }
        return mat2;

    }//copyMat

    /**
     * Finds the RMS according to a small subset of the two proteins. The subset is an array of
     * indexes to setResidue of atoms in the proteins (starting from 0 for the first atom).The second protein
     * is then rotated so that the subset is in best rms fit woth the first protein. the first protein does
     * not move. The output RMS number is the RMS of the entire two proteins.
     */

    public static double rmsPartial(double[][] co, double[][] co2, int[] partList) {
        Overlap overlap = new Overlap(co,co2);
        return  overlap.rmsPartial(partList);
    }

    private double rmsPartial(int[] partList) {
        int i, j;
        int partListLength = partList.length;
        double[][] C1 = new double[3][partListLength];
        double[][] C2 = new double[3][partListLength];
        double[][] saveCoor = coor;
        double[][] saveCoor2 = coor2;
        int saveNpt = npt;
        double[] CenterOfMass1 = {0.0, 0.0, 0.0};
        double[] CenterOfMass2 = {0.0, 0.0, 0.0};

        for (i = 0; i < partListLength; i++) {
            j = partList[i];
            CenterOfMass1[0] += C1[0][i] = coor[0][j];
            CenterOfMass1[1] += C1[1][i] = coor[1][j];
            CenterOfMass1[2] += C1[2][i] = coor[2][j];
            CenterOfMass2[0] += C2[0][i] = coor2[0][j];
            CenterOfMass2[1] += C2[1][i] = coor2[1][j];
            CenterOfMass2[2] += C2[2][i] = coor2[2][j];
        }
        CenterOfMass1[0] /= partListLength;
        CenterOfMass1[1] /= partListLength;
        CenterOfMass1[2] /= partListLength;
        CenterOfMass2[0] /= partListLength;
        CenterOfMass2[1] /= partListLength;
        CenterOfMass2[2] /= partListLength;
        initiateFields(C1, C2, C1[0].length, null, null);
        gravityCenter();
        double[] a = calcCharPol(createP());
        double[] b = findEigenval(a);
        checkEigenval(b);
        checkEigenVec();
        createUmatrix(calcBvectors());

        coor = saveCoor;
        coor2 = saveCoor2;
        npt = saveNpt;
        j = coor[0].length;
        for (i = 0; i < j; i++) {
            coor2[0][i] = coor2[0][i] - CenterOfMass2[0];
            coor2[1][i] = coor2[1][i] - CenterOfMass2[1];
            coor2[2][i] = coor2[2][i] - CenterOfMass2[2];
        }

        temp = new double[3][j];
        for (i = 0; i < j; i++) {
            temp[0][i] = (Umatrix[0][0] * coor2[0][i]) + (Umatrix[0][1] * coor2[1][i]) + (Umatrix[0][2] * coor2[2][i]);
            temp[1][i] = (Umatrix[1][0] * coor2[0][i]) + (Umatrix[1][1] * coor2[1][i]) + (Umatrix[1][2] * coor2[2][i]);
            temp[2][i] = (Umatrix[2][0] * coor2[0][i]) + (Umatrix[2][1] * coor2[1][i]) + (Umatrix[2][2] * coor2[2][i]);
        }

        for (i = 0; i < j; i++) {
            coor2[0][i] = temp[0][i];
            coor2[1][i] = temp[1][i];
            coor2[2][i] = temp[2][i];
        }

        for (i = 0; i < j; i++) {
            coor2[0][i] = coor2[0][i] + CenterOfMass1[0];
            coor2[1][i] = coor2[1][i] + CenterOfMass1[1];
            coor2[2][i] = coor2[2][i] + CenterOfMass1[2];
        }

        double d = 0;
        for (i = 0; i < coor[0].length; i++) {
            d = d + ((coor2[0][i] - coor[0][i]) * (coor2[0][i] - coor[0][i]) +
                    (coor2[1][i] - coor[1][i]) * (coor2[1][i] - coor[1][i]) +
                    (coor2[2][i] - coor[2][i]) * (coor2[2][i] - coor[2][i]));
        }
        return Math.sqrt(d / (double) j);
    }

    /**
     * This is exactly the same as 'rmsPartial' except that the rms is calculated isOn the subset only.
     */
    public static double rmsPartialAltRMS(double[][] co, double[][] co2, int[] partList) {
        Overlap overlap = new Overlap(co,co2);
        return overlap.rmsPartialAltRMS(partList);
    }
    private double rmsPartialAltRMS(int[] partList) {
        int i, j;
        int partListLength = partList.length;
        double[][] C1 = new double[3][partListLength];
        double[][] C2 = new double[3][partListLength];
        double[][] saveCoor = coor;
        double[][] saveCoor2 = coor2;
        double[] CenterOfMass1 = {0.0, 0.0, 0.0};
        double[] CenterOfMass2 = {0.0, 0.0, 0.0};
        for (i = 0; i < partListLength; i++) {
            j = partList[i];
            CenterOfMass1[0] += C1[0][i] = coor[0][j];
            CenterOfMass1[1] += C1[1][i] = coor[1][j];
            CenterOfMass1[2] += C1[2][i] = coor[2][j];
            CenterOfMass2[0] += C2[0][i] = coor2[0][j];
            CenterOfMass2[1] += C2[1][i] = coor2[1][j];
            CenterOfMass2[2] += C2[2][i] = coor2[2][j];
        }
        CenterOfMass1[0] /= partListLength;
        CenterOfMass1[1] /= partListLength;
        CenterOfMass1[2] /= partListLength;
        CenterOfMass2[0] /= partListLength;
        CenterOfMass2[1] /= partListLength;
        CenterOfMass2[2] /= partListLength;
        initiateFields(C1, C2, C1[0].length, null, null);
        gravityCenter();
        double[] a = calcCharPol(createP());
        double[] b = findEigenval(a);
        checkEigenval(b);
        checkEigenVec();
        createUmatrix(calcBvectors());

        coor = saveCoor;
        coor2 = saveCoor2;
        j = coor[0].length;
        for (i = 0; i < j; i++) {
            coor2[0][i] = coor2[0][i] - CenterOfMass2[0];
            coor2[1][i] = coor2[1][i] - CenterOfMass2[1];
            coor2[2][i] = coor2[2][i] - CenterOfMass2[2];
        }

        temp = new double[3][j];
        for (i = 0; i < j; i++) {
            temp[0][i] = (Umatrix[0][0] * coor2[0][i]) + (Umatrix[0][1] * coor2[1][i]) + (Umatrix[0][2] * coor2[2][i]);
            temp[1][i] = (Umatrix[1][0] * coor2[0][i]) + (Umatrix[1][1] * coor2[1][i]) + (Umatrix[1][2] * coor2[2][i]);
            temp[2][i] = (Umatrix[2][0] * coor2[0][i]) + (Umatrix[2][1] * coor2[1][i]) + (Umatrix[2][2] * coor2[2][i]);
        }

        for (i = 0; i < j; i++) {
            coor2[0][i] = temp[0][i];
            coor2[1][i] = temp[1][i];
            coor2[2][i] = temp[2][i];
        }

        for (i = 0; i < j; i++) {
            coor2[0][i] = coor2[0][i] + CenterOfMass1[0];
            coor2[1][i] = coor2[1][i] + CenterOfMass1[1];
            coor2[2][i] = coor2[2][i] + CenterOfMass1[2];
        }

        double d = 0;
        for (i = 0; i < partListLength; i++) {
            int newInd = partList[i];
            d = d + ((coor2[0][newInd] - coor[0][newInd]) * (coor2[0][newInd] - coor[0][newInd]) +
                    (coor2[1][newInd] - coor[1][newInd]) * (coor2[1][newInd] - coor[1][newInd]) +
                    (coor2[2][newInd] - coor[2][newInd]) * (coor2[2][newInd] - coor[2][newInd]));
        }
        return Math.sqrt(d / (double) partListLength);
    }
}// Overlap


