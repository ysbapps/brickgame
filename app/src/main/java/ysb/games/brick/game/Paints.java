package ysb.games.brick.game;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

class Paints
{
  final int[] cupColors = new int[]{Color.rgb(255, 150, 255), Color.rgb(222, 222, 222),       // contents on merge, merged contents, original cup contents (colored)
    Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.rgb(255, 165, 0), Color.rgb(255, 192, 203)};
  final int figureColor = Color.rgb(180, 255, 180);
  final Paint cupEdge = new Paint();
  final Paint cupContents = new Paint();
  final Paint control = new Paint();
  final int controlColor = Color.rgb(200, 200, 200);
  final Paint text = new Paint();
  final Paint debugLine = new Paint();
  final Paint debugText = new Paint();


  Paints(float cupEdgeWidth)
  {
    cupEdge.setStyle(Paint.Style.STROKE);
    cupEdge.setStrokeWidth(cupEdgeWidth);
    cupEdge.setColor(Color.rgb(150, 150, 255));

    text.setFlags(Paint.ANTI_ALIAS_FLAG);
    text.setTypeface(Typeface.SANS_SERIF);

    cupContents.setStyle(Paint.Style.FILL);

    debugLine.setStyle(Paint.Style.STROKE);
    debugLine.setStrokeWidth(3);
    debugLine.setColor(Color.RED);
    debugText.setFlags(Paint.ANTI_ALIAS_FLAG);
    debugText.setTextSize(30);
    debugText.setColor(Color.WHITE);
    debugText.setTypeface(Typeface.SANS_SERIF);
  }

}