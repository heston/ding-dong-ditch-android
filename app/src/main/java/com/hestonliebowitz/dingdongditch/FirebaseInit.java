package com.hestonliebowitz.dingdongditch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONObject;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

class FirebaseInit {

    public static boolean init(Context ctx, String applicationId, String apiKey, String databaseUrl, String storageBucket) {
        if (applicationId == null || apiKey == null || databaseUrl == null || storageBucket == null) {
            return false;
        }

        FirebaseOptions.Builder builder = new FirebaseOptions.Builder()
                .setApplicationId(applicationId)
                .setApiKey(apiKey)
                .setDatabaseUrl(databaseUrl)
                .setStorageBucket(storageBucket);
        FirebaseApp.initializeApp(ctx, builder.build());
        return true;
    }

    public static boolean initFromPrefs(Context ctx) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        final String appId = settings.getString(ctx.getString(R.string.firebase_pref_app_id), null);
        final String apiKey = settings.getString(ctx.getString(R.string.firebase_pref_api_key), null);
        final String dbUrl = settings.getString(ctx.getString(R.string.firebase_pref_database_url), null);
        final String storageBucket = settings.getString(ctx.getString(R.string.firebase_pref_storage_bucket), null);

        return init(ctx, appId, apiKey, dbUrl, storageBucket);
    }

    public static boolean initFromGoogleServices(Context ctx, JSONObject json) {
        String appId;
        String apiKey;
        String dbUrl;
        String storageBucket;

        try {
            JSONObject projectInfo = json.getJSONObject("project_info");
            JSONObject client = json.getJSONArray("client").getJSONObject(0);
            JSONObject clientInfo = client.getJSONObject("client_info");

            appId = clientInfo.getString("mobilesdk_app_id");
            apiKey = client.getJSONArray("api_key").getJSONObject(0).getString("current_key");
            dbUrl = projectInfo.getString("firebase_url");
            storageBucket = projectInfo.getString("storage_bucket");

        } catch (org.json.JSONException e) {
            return false;
        }

        boolean result = init(ctx, appId, apiKey, dbUrl, storageBucket);

        if (result) {
            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(ctx.getString(R.string.firebase_pref_app_id), appId);
            editor.putString(ctx.getString(R.string.firebase_pref_api_key), apiKey);
            editor.putString(ctx.getString(R.string.firebase_pref_database_url), dbUrl);
            editor.putString(ctx.getString(R.string.firebase_pref_storage_bucket), storageBucket);
            editor.apply();
        }

        return result;
    }

    public static void reset(Context ctx) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signOut();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.commit();

        triggerRebirth(ctx);
    }

    private static void triggerRebirth(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }

        Runtime.getRuntime().exit(0);
    }
}
