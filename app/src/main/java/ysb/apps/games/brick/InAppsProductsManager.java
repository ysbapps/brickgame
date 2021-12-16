package ysb.apps.games.brick;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ysb.apps.games.brick.game.Game;
import ysb.apps.utils.logs.L;


public class InAppsProductsManager implements PurchasesUpdatedListener, AcknowledgePurchaseResponseListener, PurchasesResponseListener
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
  }

  public void connect()
  {
    L.i("billingClient connect..");
    billingClient.startConnection(new BillingClientStateListener()
    {
      @Override
      public void onBillingSetupFinished(@NonNull BillingResult billingResult)
      {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK)    // The BillingClient is ready. You can query purchases here.
        {
          L.i("billingClient connected OK.");
          queryProducts();
          queryPurchases();
        }
        else
        {
          L.w("billingClient failure with code: " + billingResult.getResponseCode(), "  msg:");
          L.w(billingResult.getDebugMessage());
        }
      }

      @Override
      public void onBillingServiceDisconnected()
      {
        // Try to restart the connection on the next request to   todo
        // Google Play by calling the startConnection() method.
        L.w("onBillingServiceDisconnected");
      }
    });
  }

  public void disconnect()
  {
    billingClient.endConnection();
  }

  private void queryProducts()
  {
    L.i("queryProducts..");
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
            L.i("billingResult, code:" + billingResult.getResponseCode() + "  , debugMsg: " + billingResult.getDebugMessage());
            L.i("billingResult, skuDetailsList.size:" + skuDetailsList.size());
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
              L.i(originalJson);
            }
          }

        });
  }

  private void queryPurchases()
  {
    L.i("queryPurchases..");
    billingClient.queryPurchasesAsync("inapp", this);
//    billingClient.queryPurchaseHistoryAsync("inapp", this);
  }

  public void purchase(String productId)
  {
    L.i("purchase, productId: " + productId);
    SkuDetails sku = products.get(productId);
    if (sku != null)
    {
      L.i("purchasing sku: " + sku.getTitle());
      BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
          .setSkuDetails(sku)
          .build();
      int responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).getResponseCode();
      if (responseCode == BillingClient.BillingResponseCode.OK)
      {
        L.i("purchased OK");

      }
      else
        L.i("purchase failed!!!, responseCode: " + responseCode);
    }

  }

  @Override
  public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases)
  {
    int responseCode = billingResult.getResponseCode();
    String debugMessage = billingResult.getDebugMessage();
    if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null)
    {
      for (Purchase purchase : purchases)
      {
        L.i("purchase update, token, orderId: " + purchase.getPurchaseToken(), purchase.getOrderId());
        purchase.getSkus();
        L.i("product: " + (purchase.getSkus().size() > 0 ? purchase.getSkus().get(0) : "???"));
        L.i(purchase);
        handlePurchase(purchase);
      }
    }
    else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED)
    {      // Handle an error caused by a user cancelling the purchase flow.
      L.w("onPurchasesUpdated, USER_CANCELLED code: " + responseCode, "  msg:");
      L.w(debugMessage);
    }
    else
    {      // Handle any other error codes.
      L.w("onPurchasesUpdated, failed with other reason, code:" + responseCode, "  msg:");
      L.w(debugMessage);
    }
  }

  void handlePurchase(Purchase purchase)
  {
    L.i("handlePurchase, state: " + purchase.getPurchaseState());
    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED)
    {
      L.i("handlePurchase, PURCHASED, check acknowledge: " + purchase.isAcknowledged(), "  token:");
      L.i(purchase.getPurchaseToken());
      if (!purchase.isAcknowledged())
      {
        AcknowledgePurchaseParams acknowledgePurchaseParams =
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        L.i("handlePurchase, PURCHASED, acknowledgePurchase, token:");
        billingClient.acknowledgePurchase(acknowledgePurchaseParams, this);
      }
      else
        L.i("handlePurchase, PURCHASED, already acknowledged");
    }
    else
      L.w("handlePurchase, NOT PURCHASED");
  }

  @Override
  public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult)
  {
    L.i("onAcknowledgePurchaseResponse, code: " + billingResult.getResponseCode());
    if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK)
      L.w(billingResult.getDebugMessage());
  }

  @Override
  public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list)
  {
    int responseCode = billingResult.getResponseCode();
    String debugMessage = billingResult.getDebugMessage();
    if (responseCode == BillingClient.BillingResponseCode.OK)
    {
      for (Purchase purchase : list)
      {
        L.i("purchase response, token, orderId: " + purchase.getPurchaseToken(), purchase.getOrderId());
        purchase.getSkus();
        L.i("product: " + (purchase.getSkus().size() > 0 ? purchase.getSkus().get(0) : "???"));
        L.i(purchase);
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged())
          handlePurchase(purchase);
      }
    }
    {      // Handle any other error codes.
      L.w("onQueryPurchasesResponse, failed with other reason, code:" + responseCode, "  msg:");
      L.w(debugMessage);
    }

  }
}
