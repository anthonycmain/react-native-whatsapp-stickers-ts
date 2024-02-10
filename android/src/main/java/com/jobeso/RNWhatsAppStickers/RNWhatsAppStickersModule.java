
package com.jobeso.RNWhatsAppStickers;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.util.ArrayList;
public class RNWhatsAppStickersModule extends ReactContextBaseJavaModule {

  private static final String TAG = "RNWAStickersModule";
  public static final String EXTRA_STICKER_PACK_ID = "sticker_pack_id";
  public static final String EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority";
  public static final String EXTRA_STICKER_PACK_NAME = "sticker_pack_name";

  public static final int ADD_PACK = 200;
  public static final String ERROR_ADDING_STICKER_PACK = "Could not add this sticker pack. Please install the latest version of WhatsApp before adding sticker pack";

  private final ReactApplicationContext reactContext;

  public RNWhatsAppStickersModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNWhatsAppStickers";
  }

  @ReactMethod
  public void test(Promise promise){
    promise.resolve("");
  }

  public static String getContentProviderAuthority(Context context){
    return context.getPackageName() + ".stickercontentprovider";
  }

  @ReactMethod
  public void send(String identifier, String stickerPackName, Promise promise) {
    Intent intent = new Intent();
    intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
    intent.putExtra(EXTRA_STICKER_PACK_ID, identifier);
    intent.putExtra(EXTRA_STICKER_PACK_AUTHORITY, getContentProviderAuthority(reactContext));
    intent.putExtra(EXTRA_STICKER_PACK_NAME, stickerPackName);

    try {
      Activity activity = getCurrentActivity();
      Context context = activity.getApplicationContext();
      PackageManager packageManager = activity.getPackageManager();
      ResolveInfo should = activity.getPackageManager().resolveActivity(intent, 0);
      if (should != null) {

        // Check the stickers JSON
        ArrayList<StickerPack> stickerPackList;
        stickerPackList = StickerPackLoader.fetchStickerPacks(context);

        if (stickerPackList.size() == 0) {
          throw new Exception("Empty sticker pack list");
        }
        for (StickerPack stickerPack : stickerPackList) {
          StickerPackValidator.verifyStickerPackValidity(context, stickerPack);
        }

        Log.d(TAG, "Validated " + stickerPackList.size() + " sticker packs");

        // Add using revised code
        try {
          //if neither WhatsApp Consumer or WhatsApp Business is installed, then tell user to install the apps.
          if (!WhitelistCheck.isWhatsAppConsumerAppInstalled(packageManager) && !WhitelistCheck.isWhatsAppSmbAppInstalled(packageManager)) {
            throw new Exception("Neither WhatsApp Consumer or WhatsApp Business is installed");
          }
          final boolean stickerPackWhitelistedInWhatsAppConsumer = WhitelistCheck.isStickerPackWhitelistedInWhatsAppConsumer(context, identifier);
          final boolean stickerPackWhitelistedInWhatsAppSmb = WhitelistCheck.isStickerPackWhitelistedInWhatsAppSmb(context, identifier);
          if (!stickerPackWhitelistedInWhatsAppConsumer && !stickerPackWhitelistedInWhatsAppSmb) {
            //ask users which app to add the pack to.
            //launchIntentToAddPackToChooser(identifier, stickerPackName);
            throw new Exception("Cannot install stickers when both WhatsApp Consumer and WhatsApp Business are installed");
          } else if (!stickerPackWhitelistedInWhatsAppConsumer) {
            launchIntentToAddPackToSpecificPackage(identifier, stickerPackName, WhitelistCheck.CONSUMER_WHATSAPP_PACKAGE_NAME);
          } else if (!stickerPackWhitelistedInWhatsAppSmb) {
            launchIntentToAddPackToSpecificPackage(identifier, stickerPackName, WhitelistCheck.SMB_WHATSAPP_PACKAGE_NAME);
          } else {
            throw new Exception("Sticker pack not added. If you'd like to add it, make sure you update to the latest version of WhatsApp.");
          }
        } catch (Exception e) {
          Log.e(TAG, "error adding sticker pack to WhatsApp", e);
          throw new Exception("Sticker pack not added. If you'd like to add it, make sure you update to the latest version of WhatsApp.");
        }

        //activity.startActivityForResult(intent, ADD_PACK);
        promise.resolve("OK");
      } else {
        promise.resolve("OK, but not opened");
      }
    } catch (ActivityNotFoundException e) {
      Log.d(TAG, e.getLocalizedMessage());
      promise.reject(ERROR_ADDING_STICKER_PACK, e);
    } catch  (Exception e){
      Log.d(TAG, e.getLocalizedMessage());
      promise.reject(ERROR_ADDING_STICKER_PACK, e);
    }
  }

  private void launchIntentToAddPackToSpecificPackage(String identifier, String stickerPackName, String whatsappPackageName) {
    Activity activity = getCurrentActivity();
    Intent intent = createIntentToAddStickerPack(identifier, stickerPackName);
    intent.setPackage(whatsappPackageName);
    try {
      activity.startActivityForResult(intent, ADD_PACK);
    } catch (ActivityNotFoundException e) {
      Log.d(TAG, e.getLocalizedMessage());
    }
  }
//
//  //Handle cases either of WhatsApp are set as default app to handle this intent. We still want users to see both options.
//  private void launchIntentToAddPackToChooser(String identifier, String stickerPackName) {
//    Intent intent = createIntentToAddStickerPack(identifier, stickerPackName);
//    try {
//      startActivityForResult(Intent.createChooser(intent, getString(R.string.add_to_whatsapp)), ADD_PACK);
//    } catch (ActivityNotFoundException e) {
//      Toast.makeText(this, R.string.add_pack_fail_prompt_update_whatsapp, Toast.LENGTH_LONG).show();
//    }
//  }

  @NonNull
  private Intent createIntentToAddStickerPack(String identifier, String stickerPackName) {
    Intent intent = new Intent();
    intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
    intent.putExtra(EXTRA_STICKER_PACK_ID, identifier);
    intent.putExtra(EXTRA_STICKER_PACK_AUTHORITY, getContentProviderAuthority(reactContext));
    intent.putExtra(EXTRA_STICKER_PACK_NAME, stickerPackName);
//    intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_ID, identifier);
//    intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_AUTHORITY, BuildConfig.CONTENT_PROVIDER_AUTHORITY);
//    intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_NAME, stickerPackName);
    return intent;
  }
}
