package ysb.apps.games.brick;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import ysb.apps.utils.logs.L;


public class InAppsProductsManager implements PurchasesUpdatedListener, AcknowledgePurchaseResponseListener, PurchasesResponseListener
{
  public final static String PROD_AUTOSAVE = "ysb.apps.games.brick.autosave";
  public final static String PROD_20_LEVELS = "ysb.apps.games.brick.20_levels";

  public final static String[] PS = new String[]{"UNSPECIFIED_STATE", "PURCHASED", "PENDING"};

  private final Activity activity;
  private BillingClient billingClient = null;
  public ArrayList<Product> products = new ArrayList<>();
  public HashMap<String, Product> productsById = new HashMap<>();
  public String message = "";
  public boolean purchasesUpdated = false;
  public boolean productDetailsUpdated = false;
  private long lastUpdateQueried = 0;


  public InAppsProductsManager(Activity activity)
  {
    this.activity = activity;

    products.add(new Product(PROD_AUTOSAVE));
    products.add(new Product(PROD_20_LEVELS));
    for (Product p : products)
      productsById.put(p.id, p);
  }

  public void update()
  {
    long now = System.currentTimeMillis();
    if (now - lastUpdateQueried < 5000)    // no often than once per 5 seconds
    {
      L.w("billingClient: update in process..");
      return;
    }

    lastUpdateQueried = now;
    if (isConnected())
      disconnect();

    for (Product p : products)
      if (p.state == Product.STATE_PROCESSING && now - p.stateUpdated > 60000) // longer than 1 minute - reset processing state
        p.setState(Product.STATE_OPEN);

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
          message = "";
          queryProducts();
          queryPurchases();
        }
        else
        {
          L.w("onBSF, billingClient failure with code: " + billingResult.getResponseCode(), "  msg:");
          L.w(billingResult.getDebugMessage());
          message = billingResult.getDebugMessage();
          if (BuildConfig.DEBUG)
            createTestProducts();
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
    return billingClient != null && billingClient.isReady();
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
            L.i("onSDR, code: " + billingResult.getResponseCode(), "debugMsg: " + billingResult.getDebugMessage());
            L.i("onSDR, skuDetailsList.size: " + skuDetailsList.size());
            for (SkuDetails sku : skuDetailsList)
            {
              L.i("onSDR, id, type: " + sku.getSku(), sku.getType());
              L.i("onSDR, title: " + sku.getTitle());
              L.i("onSDR, desc: " + sku.getDescription());
              L.i("onSDR, price: " + sku.getPrice(), sku.getPriceCurrencyCode());
              Product p = productsById.get(sku.getSku());
              if (p != null)
              {
                p.sku = sku;
                L.i("onSDR, product found by id, p:");
                L.i(p);
                productDetailsUpdated = true;
              }
              else
                L.w("onSDR, product NOT found by id: " + sku.getSku());
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
      L.i("onQPR, purchases.size: " + list.size());
      for (Purchase purchase : list)
      {
        L.i("onQPR, state: " + purchase.getPurchaseState(), ps(purchase.getPurchaseState()));
        L.i("onQPR, token: " + purchase.getPurchaseToken());
        L.i("onQPR, orderId: " + purchase.getOrderId());
        String productId = purchase.getSkus().size() > 0 ? purchase.getSkus().get(0) : "???";
        L.i("onQPR, product: " + productId);
        L.i("onQPR, time: " + new Date(purchase.getPurchaseTime()));
        handlePurchase(purchase);
      }

      purchasesUpdated = true;
    }
    else       // Handle any other error codes.
    {
      L.w("onQPR, failed, code:" + billingResult.getResponseCode(), "  msg:");
      L.w(billingResult.getDebugMessage());
    }
  }

  private void setProductState(String productId, int state)
  {
    Product p = productsById.get(productId);
    if (p != null)
      p.setState(state);
    else
      L.w("setPP, product NOT found by id: " + productId);
  }

  public void purchase(String productId)
  {
    L.i("purchase, productId: " + productId);
    Product p = productsById.get(productId);
    if (p != null)
    {
      L.i("purchase, sku: " + p.sku.getTitle());
//      if (BuildConfig.DEBUG)
//      {
//        p.purchased = true;
//        return;
//      }

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
    L.i("onPU, code: " + billingResult.getResponseCode());
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
      L.w("onPU, failed with other reason, code: " + billingResult.getResponseCode(), "  msg:");
      L.w(billingResult.getDebugMessage());
    }
  }

  private void handlePurchase(Purchase purchase)
  {
    L.i("handlePurchase, state: " + purchase.getPurchaseState(), ps(purchase.getPurchaseState()));
    String productId = purchase.getSkus().size() > 0 ? purchase.getSkus().get(0) : "???";
    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED)
    {
      setProductState(productId, Product.STATE_PURCHASED);
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

//      consume(purchase);  // to enable purchased product for testing
    }
    else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING)
      setProductState(productId, Product.STATE_PROCESSING);
    else
      setProductState(productId, Product.STATE_OPEN);
  }

  private void consume(Purchase purchase)
  {
    L.i("consume purchase: " + purchase);
    L.i("token: " + purchase.getPurchaseToken());
    L.i("orderId: " + purchase.getOrderId());
    ConsumeParams consumeParams =
        ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.getPurchaseToken())
            .build();
    billingClient.consumeAsync(consumeParams, new ConsumeResponseListener()
    {
      @Override
      public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String purchaseToken)
      {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK)
          L.w("oCR, consumed, token: " + purchaseToken);
      }
    });
  }

  @Override
  public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult)
  {
    L.i("onAPR, code: " + billingResult.getResponseCode());
    if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK)
      L.w(billingResult.getDebugMessage());
  }

  public boolean isProductPurchased(String id)
  {
    Product p = productsById.get(id);
    return p != null && p.state == Product.STATE_PURCHASED;
  }

  private void createTestProducts()
  {
    try
    {
      L.i("createTestProducts..");
      String[] jsonSkuDetails = new String[]{"{\n" +
          "\"productId\":\"ysb.apps.games.brick.autosave\",\n" +
          "\"type\":\"inapp\",\n" +
          "\"title\":\"Autosave\",\n" +
          "\"name\":\"Autosave\",\n" +
          "\"price\":\"100,00?????\",\n" +
          "\"price_amount_micros\":100000000,\n" +
          "\"price_currency_code\":\"RUB\",\n" +
          "\"description\":\"Allows you to start the game from the highest level that you achieved.\",\n" +
          "\"skuDetailsToken\":\"BEuhp4IBDAONRO9LfBGiubsI1ptNt5nyzHpqVdG1pI2TVy4PW7WHnjHp06sfttoQ8M8K\"\n" +
          "}\n",
          "{\n" +
              "\"productId\":\"ysb.apps.games.brick.20_levels\",\n" +
              "\"type\":\"inapp\",\n" +
              "\"title\":\"+20 ?????????????? (ysb.apps.games.brick (unreviewed))\",\n" +
              "\"name\":\"+20 ??????????????\",\n" +
              "\"price\":\"200,00?????\",\n" +
              "\"price_amount_micros\":200000000,\n" +
              "\"price_currency_code\":\"RUB\",\n" +
              "\"description\":\"???? ???????????????? ?????? 20 ?????????????? ?? ?? ?????? ???????????? 30 ?????????????? ??????????.\",\n" +
              "\"skuDetailsToken\":\"AEuhp4IBDAONRO9LfBGiubsI1ptNt5nyzHpqVdG1pI2TVy4PW7WHnjHp06sfttoQ8M8K\"\n" +
              "}\n"};
      for (String json : jsonSkuDetails)
      {
        SkuDetails sku = new SkuDetails(json);
        Product p = productsById.get(sku.getSku());
        if (p != null)
          p.sku = sku;
        L.i("p: " + p);
      }
    }
    catch (JSONException e)
    {
      e.printStackTrace();
    }
  }
}
