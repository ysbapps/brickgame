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
import java.util.LinkedHashMap;
import java.util.List;

import ysb.apps.games.brick.game.Game;
import ysb.apps.utils.logs.L;


public class InAppsProductsManager implements PurchasesUpdatedListener, AcknowledgePurchaseResponseListener, PurchasesResponseListener
{
  public final static String PROD_AUTOSAVE = "ysb.apps.games.brick.autosave";
  public final static String PROD_20_LEVELS = "ysb.apps.games.brick.20_levels";

  public final static String[] PS = new String[]{"UNSPECIFIED_STATE", "PURCHASED", "PENDING"};

  private final Activity activity;
  private final Game game;
  private final BillingClient billingClient;
  public LinkedHashMap<String, Product> products = new LinkedHashMap<>();
  public String message = "";


  private static class Product
  {
    final String id;
    final SkuDetails sku;
    boolean purchased;

    public Product(String id, SkuDetails sku)
    {
      this.id = id;
      this.sku = sku;
    }
  }

  public InAppsProductsManager(Activity activity, Game game)
  {
    this.activity = activity;
    this.game = game;

    L.i("billingClient init..");
    billingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases()
        .build();

    billingClient.startConnection(new BillingClientStateListener()
    {
      @Override
      public void onBillingSetupFinished(@NonNull BillingResult billingResult)
      {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK)    // The BillingClient is ready. You can query purchases here.
        {
          L.i("onBSF, billingClient connected OK.");
          message="";
          queryProducts();
          queryPurchases();
        }
        else
        {
          L.w("onBSF, billingClient failure with code: " + billingResult.getResponseCode(), "  msg:");
          L.w(billingResult.getDebugMessage());
          message = billingResult.getDebugMessage();
        }
      }

      @Override
      public void onBillingServiceDisconnected()
      {        // Try to restart the connection on the next request to Google Play by calling the startConnection() method. - not sure that it is needed
        L.w("onBillingServiceDisconnected");
      }
    });
  }

  public boolean isConnected()
  {
    return billingClient.isReady();
  }

  public void disconnect()
  {
    L.i("billingClient endConnection.");
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
            L.i("onSDR, code:" + billingResult.getResponseCode() + "  , debugMsg: " + billingResult.getDebugMessage());
            L.i("onSDR, skuDetailsList.size:" + skuDetailsList.size());
            for (SkuDetails sku : skuDetailsList)
            {
              Product p = new Product(sku.getSku(), sku);
              products.put(p.id, p);
              L.i("onSDR, id, type:" + sku.getSku(), sku.getType());
              L.i("onSDR, title:" + sku.getTitle());
              L.i("onSDR, description:" + sku.getDescription());
              L.i("onSDR, price:" + sku.getPrice(), sku.getPriceCurrencyCode());
            }
          }
        });
  }

  private void queryPurchases()
  {
    L.i("queryPurchases..");
    billingClient.queryPurchasesAsync("inapp", this);
  }

  private String ps(int state)
  {
    if (state >= 0 && state < PS.length)
      return PS[state];
    else
      return "UNKNOWN STATE";
  }

  @Override
  public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list)
  {
    L.i("onQPR, code:" + billingResult.getResponseCode());
    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK)
    {
      for (Purchase purchase : list)
      {
        L.i("onQPR, state: " + purchase.getPurchaseState(), ps(purchase.getPurchaseState()));
        L.i("onQPR, token: " + purchase.getPurchaseToken());
        L.i("onQPR, orderId: " + purchase.getOrderId());
        String productId = purchase.getSkus().size() > 0 ? purchase.getSkus().get(0) : "???";
        L.i("onQPR, product: " + productId);
        L.i("onQPR, time: " + purchase.getPurchaseTime());
        L.i("onQPR, package: " + purchase.getPackageName());
        L.i("onQPR, account: " + purchase.getAccountIdentifiers());
        handlePurchase(purchase);
      }
    }
    else       // Handle any other error codes.
    {
      L.w("onQPR, failed, code:" + billingResult.getResponseCode(), "  msg:");
      L.w(billingResult.getDebugMessage());
    }
  }

  private void setProductPurchased(String productId)
  {
    Product p = products.get(productId);
    if (p != null)
      p.purchased = true;
    else
      L.w("setPP, product NOT found: " + productId);
  }

  public void purchase(String productId)
  {
    L.i("purchase, productId: " + productId);
    Product p = products.get(productId);
    if (p != null)
    {
      L.i("purchase, sku: " + p.sku.getTitle());
      BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
          .setSkuDetails(p.sku)
          .build();
      int responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).getResponseCode();
      if (responseCode == BillingClient.BillingResponseCode.OK)
        L.i("purchase, launchBillingFlow returned OK");
      else
        L.i("purchase, launchBillingFlow failed!, code: " + responseCode);
    }
    else
      L.w("purchase, product NOT found: " + productId);
  }

  @Override
  public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases)
  {
    L.i("onPU, code:" + billingResult.getResponseCode());
    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null)
    {
      for (Purchase purchase : purchases)
      {
        L.i("onPU, state: " + purchase.getPurchaseState(), ps(purchase.getPurchaseState()));
        L.i("onPU, token: " + purchase.getPurchaseToken());
        L.i("onPU, orderId: " + purchase.getOrderId());
        String productId = purchase.getSkus().size() > 0 ? purchase.getSkus().get(0) : "???";
        L.i("onPU, product: " + productId);
        L.i("onPU, time: " + purchase.getPurchaseTime());
        L.i("onPU, account: " + purchase.getAccountIdentifiers());
        handlePurchase(purchase);
      }
    }
    else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED)
    {      // Handle an error caused by a user cancelling the purchase flow.
      L.w("onPU, USER_CANCELLED code: " + billingResult.getResponseCode(), "  msg:");
      L.w(billingResult.getDebugMessage());
    }
    else
    {      // Handle any other error codes.
      L.w("onPU, failed with other reason, code:" + billingResult.getResponseCode(), "  msg:");
      L.w(billingResult.getDebugMessage());
    }
  }

  private void handlePurchase(Purchase purchase)
  {
    L.i("handlePurchase, state: " + purchase.getPurchaseState(), ps(purchase.getPurchaseState()));
    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED)
    {
      String productId = purchase.getSkus().size() > 0 ? purchase.getSkus().get(0) : "???";
      setProductPurchased(productId);
      L.i("handlePurchase, check acknowledge: " + purchase.isAcknowledged());
      if (!purchase.isAcknowledged())
      {
        L.i("handlePurchase, acknowledge..");
        AcknowledgePurchaseParams acknowledgePurchaseParams =
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        billingClient.acknowledgePurchase(acknowledgePurchaseParams, this);
      }
    }
  }

  @Override
  public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult)
  {
    L.i("onAPR, code: " + billingResult.getResponseCode());
    if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK)
      L.w(billingResult.getDebugMessage());
  }
}
