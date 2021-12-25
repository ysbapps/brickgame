package ysb.apps.games.brick;

import androidx.annotation.NonNull;

import com.android.billingclient.api.SkuDetails;

import ysb.apps.utils.logs.L;

public class Product
{
  public final static int STATE_OPEN = 0;
  public final static int STATE_PROCESSING = 1;
  public final static int STATE_PURCHASED = 2;
  public final static String[] PS = new String[]{"OPEN", "PROCESSING", "PURCHASED"};

  public final String id;
  public SkuDetails sku = null;
  public int state = STATE_OPEN;
  public long stateUpdated = System.currentTimeMillis();


  public Product(String id)
  {
    this.id = id;
  }

  public void setState(int state)
  {
    this.state=state;
    stateUpdated = System.currentTimeMillis();
    L.i("setProductState, p: " + this);
  }

  @NonNull
  @Override
  public String toString()
  {
    return "id=" + id + ", sku=" + (sku != null ? sku.getType() : "null") +  ", state=" + PS[state];
  }
}