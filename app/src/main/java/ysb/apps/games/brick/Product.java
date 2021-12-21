package ysb.apps.games.brick;

import androidx.annotation.NonNull;

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

  @NonNull
  @Override
  public String toString()
  {
    return "Product{" +
        "id='" + id + '\'' +
        ", purchased=" + purchased +
        '}';
  }
}