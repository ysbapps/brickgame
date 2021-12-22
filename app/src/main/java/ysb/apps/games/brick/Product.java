package ysb.apps.games.brick;

import androidx.annotation.NonNull;

import com.android.billingclient.api.SkuDetails;

public class Product
{
  public final String id;
  public SkuDetails sku = null;
  public boolean purchased;

  public Product(String id)
  {
    this.id = id;
  }

  @NonNull
  @Override
  public String toString()
  {
    return "id=" + id + ", sku=" + (sku != null ? sku.getType() : "null") +  ", pur=" + purchased;
  }
}