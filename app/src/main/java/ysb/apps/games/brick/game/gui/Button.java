package ysb.apps.games.brick.game.gui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class Button
{
  protected final Bitmap image;
  public final RectF rect;
  protected final Paint p = new Paint();
  public boolean enabled = true;


  public Button(Bitmap image, float x, float y)
  {
    this.image = image;
    this.rect = new RectF(x, y, x + image.getWidth(), y + image.getHeight());
  }

  public Button(float x, float y, float w, float h)
  {
    this.image = null;
    this.rect = new RectF(x, y, x + w, y + h);
  }

  public void draw(Canvas canvas)
  {
    p.setAlpha(enabled ? 255 : 128);
    if (image != null)
      canvas.drawBitmap(image, rect.left, rect.top, p);
    else
    {
      p.setColor(Color.LTGRAY);
      p.setStyle(Paint.Style.FILL);
      p.setAlpha(164);
      float r = Math.min(rect.right - rect.left, rect.bottom - rect.top) / 2;
      canvas.drawCircle(rect.left + r, rect.top + r, r, p);

      p.setAlpha(255);
      p.setColor(Color.WHITE);
      p.setStyle(Paint.Style.STROKE);
      float sw = canvas.getClipBounds().height() / 200f;
      p.setStrokeWidth(sw);
      canvas.drawCircle(rect.left + r, rect.top + r, r, p);
    }
  }

  public boolean contains(float x, float y, float wider)
  {
    RectF wr = new RectF(rect);
    wr.inset(-wider * rect.width(), -wider * rect.height());
    return wr.contains(x, y);
  }
}