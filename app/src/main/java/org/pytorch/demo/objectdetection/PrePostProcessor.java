// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package org.pytorch.demo.objectdetection;

import android.graphics.Rect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * it generate a result for every rect.
 */
class Result {
    int classIndex;
    Float score;
    Rect rect;

    public Result(int cls, Float output, Rect rect) {
        this.classIndex = cls;
        this.score = output;
        this.rect = rect;
    }

    @Override
    public String toString() {
        return "Result{" +
                "classIndex=" + classIndex +
                ", score=" + score +
                ", rect=" + rect +
                '}';
    }

    public int getClassIndex() {
        return classIndex;
    }
};


// Henry: Aqui hay varios valores que tendremos que poner como input de la imagen:
// mThreshold (conf), mInputWidth y mInputHeight (resize), mOutputColumn (depende del numero de clases)

public class PrePostProcessor {
    // for yolov5 model, no need to apply MEAN and STD
    static float[] NO_MEAN_RGB = new float[] {0.0f, 0.0f, 0.0f};
    static float[] NO_STD_RGB = new float[] {1.0f, 1.0f, 1.0f};

    // model input image size
    static int mInputWidth = 640;
    static int mInputHeight = 640;

    static String[] mClasses;

    // model output is of size 25200*(num_of_class+5)
    private static int mOutputRow = 25200; // as decided by the YOLOv5 model for input image of size 640*640
    private static int mOutputColumn = 0; // Inicializo en el método con el tamaño del array más 5 parámetros:tamaño y score. Así se adapta a cada modo. left, top, right, bottom, score and 80 class probability (for yolov5)



    /**Indice mínimo de IOU (30%) */
    private static float mThresholdIOU = 0.30f; // score above which a detection is generated
    private static float mThreshold= 0.30f; // score above which a box is eliminated.
    private static int mNmsLimit = 15;//Amount of Object detected

    /**
     * Getters and Setters for seekBar control.
     * @param mThreshold
     */
    public static void setmNmsLimit(int mNmsLimit) {
        PrePostProcessor.mNmsLimit = mNmsLimit;
    }

    public static void setmThreshold(float mThreshold) {
        PrePostProcessor.mThreshold = mThreshold;
    }

    public static void setmThresholdIOU(float mThresholdIOU) {
        PrePostProcessor.mThresholdIOU = mThresholdIOU;
    }

    // The two methods nonMaxSuppression and IOU below are ported from https://github.com/hollance/YOLO-CoreML-MPSNNGraph/blob/master/Common/Helpers.swift
    /**
     Removes bounding boxes that overlap too much with other boxes that have
     a higher score.
     - Parameters:
     - boxes: an array of bounding boxes and their scores
     - limit: the maximum number of boxes that will be selected
     - threshold: used to decide whether boxes overlap too much
     */
    static ArrayList<Result> nonMaxSuppression(ArrayList<Result> boxes, int limit, float threshold) {

        // Do an argsort on the confidence scores, from high to low.
        Collections.sort(boxes,
                new Comparator<Result>() {
                    @Override
                    public int compare(Result o1, Result o2) {
                        return o1.score.compareTo(o2.score);
                    }
                });

        ArrayList<Result> selected = new ArrayList<>();
        boolean[] active = new boolean[boxes.size()];
        Arrays.fill(active, true);
        int numActive = active.length;

        /**The algorithm is simple: Start with the box that has the highest score.
         *
         * Remove any remaining boxes that overlap it more than the given threshold
         * amount.If there are any boxes left (i.e. these did not overlap with any
         * previous boxes), then repeat this procedure, until no more boxes remain
         * or the limit has been reached.
       */
        boolean done = false;
        for (int i=0; i<boxes.size() && !done; i++) {
            if (active[i]) {
                Result boxA = boxes.get(i);
                selected.add(boxA);
                if (selected.size() >= limit) break;

                for (int j=i+1; j<boxes.size(); j++) {
                    if (active[j]) {
                        Result boxB = boxes.get(j);

                        if (IOU(boxA.rect, boxB.rect) > threshold) {
                            active[j] = false;
                            numActive -= 1;
                            if (numActive <= 0) {
                                done = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return selected;
    }

    /**
     Computes intersection-over-union overlap between two bounding boxes.
     */
    static float IOU(Rect a, Rect b) {
        float areaA = (a.right - a.left) * (a.bottom - a.top);
        if (areaA <= 0.0)
            return 0.0f;

        float areaB = (b.right - b.left) * (b.bottom - b.top);
        if (areaB <= 0.0)
            return 0.0f;

        float intersectionMinX = Math.max(a.left, b.left);
        float intersectionMinY = Math.max(a.top, b.top);
        float intersectionMaxX = Math.min(a.right, b.right);
        float intersectionMaxY = Math.min(a.bottom, b.bottom);
        float intersectionArea = Math.max(intersectionMaxY - intersectionMinY, 0) *
                Math.max(intersectionMaxX - intersectionMinX, 0);
        return intersectionArea / (areaA + areaB - intersectionArea);
    }

    /**
     * @param outputs
     * @param imgScaleX
     * @param imgScaleY
     * @param ivScaleX
     * @param ivScaleY
     * @param startX
     * @param startY
     * @return ArrayList()
     * This method calc the threshold
     */
    static ArrayList<Result> outputsToNMSPredictions(float[] outputs, float imgScaleX, float imgScaleY, float ivScaleX, float ivScaleY, float startX, float startY) {
        ArrayList<Result> results = new ArrayList<>();

        mOutputColumn = (mClasses.length)+5; //Here take the number of classes possibilities

        /**
         *
         */
        for (int i = 0; i< mOutputRow; i++) {
            if (outputs[i* mOutputColumn +4] > mThreshold) {
                float x = outputs[i* mOutputColumn];
                float y = outputs[i* mOutputColumn +1];
                float w = outputs[i* mOutputColumn +2];
                float h = outputs[i* mOutputColumn +3];

                float left = imgScaleX * (x - w/2);
                float top = imgScaleY * (y - h/2);
                float right = imgScaleX * (x + w/2);
                float bottom = imgScaleY * (y + h/2);

                /**
                 * The class of object is recognized at this point.
                 */
                float max = outputs[i* mOutputColumn +5];
                int cls = 0;


                for (int j = 0; j < mOutputColumn -5; j++) {
                    if (outputs[i* mOutputColumn +5+j] > max ) {
                        max = outputs[i* mOutputColumn +5+j];
                        cls = j;//inform about class of object detected.
                    }
                }

                /**
                 * Make the Rect which content the result and add it to a Array (result)
                 */
                Rect rect = new Rect((int)(startX+ivScaleX*left), (int)(startY+top*ivScaleY), (int)(startX+ivScaleX*right), (int)(startY+ivScaleY*bottom));
                Result result = new Result(cls, outputs[i*mOutputColumn+4], rect);
                results.add(result);
            }
        }
        return nonMaxSuppression(results, mNmsLimit, mThresholdIOU);
    }
}
