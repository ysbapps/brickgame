package ysb.apps.games.brick;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ysb.apps.games.brick.game.Game;


public class InAppsProductsManager implements PurchasesUpdatedListener
{
  public final static String PROD_AUTOSAVE = "ysb.apps.games.brick.autosave";
  public final static String PROD_20_LEVELS = "ysb.apps.games.brick.20_levels";

  private final Activity activity;
  private final Game game;
  private final BillingClient billingClient;
  private HashMap<String, SkuDetails> products = new HashMap<>();


  public InAppsProductsManager(Activity activity, Game game)
  {
    this.activity = activity;
    this.game = game;

    billingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases()
        .build();
    log("billingClient: " + billingClient);
  }

  public void connect()
  {
    billingClient.startConnection(new BillingClientStateListener()
    {
      @Override
      public void onBillingSetupFinished(@NonNull BillingResult billingResult)
      {
        log("onBillingSetupFinished, code: " + billingResult.getResponseCode() + "  debugMsg: " + billingResult.getDebugMessage());
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK)    // The BillingClient is ready. You can query purchases here.
        {
          queryProducts();

        }
      }

      @Override
      public void onBillingServiceDisconnected()
      {
        // Try to restart the connection on the next request to   todo
        // Google Play by calling the startConnection() method.
        log("onBillingServiceDisconnected");
      }
    });
  }

  public void disconnect()
  {
    billingClient.endConnection();
  }

  private void queryProducts()
  {
    log("queryProducts..");
    List<String> skuList = new ArrayList<>();
    skuList.add(PROD_AUTOSAVE);
    skuList.add(PROD_20_LEVELS);
    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
    params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
    billingClient.querySkuDetailsAsync(params.build(),
        new SkuDetailsResponseListener()
        {
          @Override
          public void onSkuDetailsResponse(@NonNull BillingResult billingResult, List<SkuDetails> skuDetailsList)
          {            // Process the result.
            log("billingResult, code:" + billingResult.getResponseCode() + "  , debugMsg: " + billingResult.getDebugMessage());
            log("billingResult, skuDetailsList.size:" + skuDetailsList.size());
            for (SkuDetails sku : skuDetailsList)
            {
              String sku1 = sku.getSku();
              String title = sku.getTitle();
              String description = sku.getDescription();
              String type = sku.getType();
              String price = sku.getPrice();
              String priceCurrencyCode = sku.getPriceCurrencyCode();
              String originalJson = sku.getOriginalJson();
              String zzb = sku.zzb();
              products.put(sku.getSku(), sku);
            }
          }

        });
  }

  public void purchase(String productId)
  {
    log("purchase, productId: " + productId);
    SkuDetails sku = products.get(productId);
    if (sku != null)
    {
      log("purchasing sku: " + sku.getTitle());
      BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
          .setSkuDetails(sku)
          .build();
      int responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).getResponseCode();
      log("purchasing responseCode: " + responseCode);
      if (responseCode == BillingClient.BillingResponseCode.OK)
      {
        log("purchased.");

      }

    }

  }

  @Override
  public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases)
  {
    int responseCode = billingResult.getResponseCode();
    String debugMessage = billingResult.getDebugMessage();
    log("onPurchasesUpdated, code:" + responseCode + "  , debugMsg: " + debugMessage);
    if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null)
    {
      for (Purchase purchase : purchases)
      {
        log(purchase);
        String purchaseToken = purchase.getPurchaseToken();
        String orderId = purchase.getOrderId();
      }
    }
    else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED)
    {      // Handle an error caused by a user cancelling the purchase flow.
      log("user cancelled");
    }
    else
    {      // Handle any other error codes.
      log("other error: " + responseCode);
    }
  }

  void log(Object o)
  {
    Log.v("QQ", "" + o);
  }
}
