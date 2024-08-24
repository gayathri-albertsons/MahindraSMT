package com.atpl.digismt.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.atpl.digismt.constant.Constants;

public class PrefFile {



    public static SharedPreferences getSharedPreference(Context context){

        return context.getSharedPreferences(Constants.PREF_FILE, 0); // 0 - for private mode

    }

    /**function saves url **/
    public static boolean saveUrl(Context context,String url){



        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(Constants.PREF_FILE, 0);

        try {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(Constants.PREF_URL, url);
            editor.apply();//I added this line and started to work...
            editor.commit();
        }catch (Exception ex){
            ex.printStackTrace();
        }




        return true;
    }

    /**function returns url **/
    public static String getSavedUrl(Context context){
        SharedPreferences sharedPreferences=getSharedPreference(context);

        return  sharedPreferences.getString(Constants.PREF_URL,null);

    }


    /**function saved captureimageurl **/
    public static void saveCaptureImageUrl(Context context,String captureImageUrl){

        SharedPreferences sharedPreferences=getSharedPreference(context);

        sharedPreferences.edit().putString(Constants.PREF_CAPTURE_ImURL,captureImageUrl).commit();
    }

     /**function returns capturedSavedUrl **/

     public static String getCaptureImageUrl(Context context){

         SharedPreferences sharedPreferences=getSharedPreference(context);

         return  sharedPreferences.getString(Constants.PREF_CAPTURE_ImURL,null);
     }

    /**function will save image file **/
    public static void  saveCapturedImage(Context context,String fileName,String fileValue){
        SharedPreferences sharedPreferences=getSharedPreference(context);

        sharedPreferences.edit().putString(fileName,fileValue).commit();
    }

    /**function will get image file **/
    public static String getSavedImagefile(Context context,String imageFileName){
        SharedPreferences sharedPreferences=getSharedPreference(context);

        return  sharedPreferences.getString(imageFileName,null);
    }



    /**function saved captureimageurl **/
    public static String getAllInformation(Context context,String fileName){

        SharedPreferences sharedPreferences=getSharedPreference(context);

        return  sharedPreferences.getString(fileName,null);

    }

    /**function saved captureimageurl **/
    public static void saveAllFileInformation(Context context,String fileName,String dataInfo){

        SharedPreferences sharedPreferences=getSharedPreference(context);

        sharedPreferences.edit().putString(fileName,dataInfo).commit();
    }

    /**function saves QrCode **/
    public static void saveQrCode(Context context,String jsonQrData){

        SharedPreferences sharedPreference=getSharedPreference(context);

        sharedPreference.edit().putString(Constants.PREF_JSON_DATA,jsonQrData).commit();

    }


    /**function returns QrCode **/

    public static String getSavedQrCode(Context context){

        SharedPreferences sharedPreferences=getSharedPreference(context);

        return sharedPreferences.getString(Constants.PREF_JSON_DATA,null);

    }

    /**changes function will delete sharepreferece **/

    public static void deleteSharedPreference(Context context){
        SharedPreferences pref = getSharedPreference(context);

        pref.edit().clear().commit();

    }


    public static void saveMainUrl(Context context,String mainUrl){

        SharedPreferences sharedPreferences=getSharedPreference(context);

        sharedPreferences.edit().putString(Constants.URL_MAIN,mainUrl).commit();

    }

    public static String getMainUrl(Context context){

        SharedPreferences sharedPreferences=getSharedPreference(context);

        return sharedPreferences.getString(Constants.URL_MAIN,null);

    }


    public static void saveSecondaryUrl(Context context,String secondaryUrl){

        SharedPreferences sharedPreferences=getSharedPreference(context);

        sharedPreferences.edit().putString(Constants.URL_SECONDARY,secondaryUrl).commit();

    }

    public static String getSecondaryUrl(Context context){

        return  getSharedPreference(context).getString(Constants.URL_SECONDARY,null);

    }
    /**function will refreshUrl **/

}
