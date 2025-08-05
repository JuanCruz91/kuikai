// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package org.pytorch.demo.objectdetection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
/**
 * this class control the rect draw.
 * */

public class ResultView extends View {

    private final static int TEXT_X = 40;
    private final static int TEXT_Y = 35;
    private final static int TEXT_WIDTH = 260;
    private final static int TEXT_HEIGHT = 50;

    private Paint mPaintRectangle;
    private Paint mPaintText;
    private ArrayList<Result> mResults;

    public ResultView(Context context) {
        super(context);
    }

    /**
     * Result view paints on top of the imageView
     * @param context
     * @param attrs
     */
    public ResultView(Context context, AttributeSet attrs){
        super(context, attrs);
        mPaintRectangle = new Paint();
        mPaintRectangle.setColor(Color.YELLOW); // Aquí se incluye el color del recuadro.
        mPaintText = new Paint();
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mResults == null) return;
        for (Result result : mResults) {

            /**
             * Draw IA-box
             */
            mPaintRectangle.setStrokeWidth(5);
            mPaintRectangle.setStyle(Paint.Style.STROKE);
            /**
             * Set color at @color that return a Hexa
             * make the color name with String and int.
             * to choose and extract the color need a buffer an slow down the app.
             * I keep rect in yellow because i don't find the form to introduce the colors without  buffer.
             */
            mPaintRectangle.setColor(Color.YELLOW);
            canvas.drawRect(result.rect, mPaintRectangle);


            /**
             * Draw TextBox
             */
            Path mPath = new Path();
            RectF mRectF = new RectF(result.rect.left, result.rect.top, result.rect.left + TEXT_WIDTH,  result.rect.top + TEXT_HEIGHT);
            mPath.addRect(mRectF, Path.Direction.CW);
            mPaintText.setColor(Color.BLACK);
            canvas.drawPath(mPath, mPaintText);

            /**
             * Draw Text
              */
            mPaintText.setColor(Color.WHITE);
            mPaintText.setStrokeWidth(0);
            mPaintText.setStyle(Paint.Style.FILL);
            mPaintText.setTextSize(32);
            canvas.drawText(String.format("%s %.2f", PrePostProcessor.mClasses[result.classIndex], result.score), result.rect.left + TEXT_X, result.rect.top + TEXT_Y, mPaintText);
        }
    }

    public void setResults(ArrayList<Result> results) {
        mResults = results;
    }
}
