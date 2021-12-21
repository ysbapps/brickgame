package ysb.apps.games.brick;

import com.android.billingclient.api.SkuDetails;

public class Product
{
  public final String id;
  public final SkuDetails sku;
  public boolean purchased;

  public Product(String id, SkuDetails sku)
  {
    this.id = id;
    this.sku = sku;
  }
}