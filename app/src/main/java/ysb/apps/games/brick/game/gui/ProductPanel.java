package ysb.apps.games.brick.game.gui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import ysb.apps.games.brick.Product;
import ysb.apps.games.brick.game.DrawView;

public class ProductPanel extends Button
{
  private Product product = null;


  public ProductPanel()
  {
    super(0, 0, 0, 0);
  }

  public void setProduct(Product p)
  {
    product = p;
    enabled = !product.purchased;
  }

  public Product getProduct()
  {
    return product;
  }

  public void draw(Canvas canvas)
  {
    float dk = DrawView.dk;
    float r = 40 * dk;
    p.setColor(Color.BLACK);
    p.setStyle(Paint.Style.FILL);
    p.setAlpha(128);
    canvas.drawRoundRect(rect, 40 * dk, 40 * dk, p);
    p.setColor(Color.rgb(200, 200, 200));
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(8 * dk);
    p.setAlpha(enabled ? 255 : 128);
    canvas.drawRoundRect(rect, 40 * dk, 40 * dk, p);
    float cx = rect.right - 2 * r + 14 * dk;
    float cy = rect.bottom - 2 * r + 14 * dk;
    canvas.drawCircle(cx, cy, r, p);
    if (!enabled)   // purchased
    {
      p.setColor(Color.rgb(0, 164, 0));
      p.setStrokeWidth(14 * dk);
      cx += r / 2.5f;
      canvas.drawLine(cx - r, cy - r / 2, cx - r / 4, cy + r / 2, p);
      canvas.drawLine(cx - r / 3.2f, cy + r / 3f, cx + 2 * r / 4, cy - r * 1.2f, p);
    }
    p.setStyle(Paint.Style.FILL);
    p.setTextSize(54 * dk);
    p.setColor(Color.WHITE);
    p.setTextAlign(Paint.Align.LEFT);
    p.setAlpha(enabled ? 255 : 164);
    String name = product.sku.getTitle();
    int ei = name.indexOf(" (ysb.");
    canvas.drawText(name.substring(0, ei > 0 ? ei : name.length()), rect.left + 22 * dk, rect.top + rect.height() / 3.2f, p);
    p.setTextSize(44 * dk);
    p.setTextAlign(Paint.Align.RIGHT);
    p.setColor(Color.YELLOW);
    p.setAlpha(enabled ? 255 : 164);
    canvas.drawText(product.sku.getPrice(), rect.right - 22 * dk, rect.top + rect.height() / 3.2f, p);
    p.setTextSize(36 * dk);
    p.setColor(Color.rgb(200, 200, 200));
    p.setTextAlign(Paint.Align.LEFT);
    p.setAlpha(enabled ? 255 : 164);
    float ty = rect.top + rect.height() / 1.6f;
    int ps = 0;
    int pe = 0;
    Rect tr = new Rect();
    String desc = product.sku.getDescription();
    while (pe < desc.length())
    {
      while (tr.width() < 2 * rect.width() / 3)
      {
        pe = desc.indexOf(' ', pe + 1);
        if (pe > -1)
          p.getTextBounds(desc, ps, pe, tr);
        else
        {
          pe = desc.length();
          break;
        }
      }
      canvas.drawText(desc.substring(ps, pe), rect.left + 22 * dk, ty, p);
      ps = pe + 1;
      ty += rect.height() / 4.5f;
      tr.left = tr.right = 0;
    }
  }

}