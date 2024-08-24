package com.atpl.digismt.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.pdf.PdfDocument;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.atpl.digismt.R;
import com.atpl.digismt.ScannedBarcodeActivity;
import com.atpl.digismt.bean.AddressInformation;
import com.atpl.digismt.constant.Constants;
import com.atpl.digismt.gps.GPSTracker;
import com.atpl.digismt.utils.CommonFunctions;
import com.atpl.digismt.utils.PrefFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {
    private String URL_PICTURE = "https://ent.ngage.services/installation/takephoto.php";
    //private String DEFAULT_URL = "https://mahindracms.m-devsecops.com";
    private String DEFAULT_URL = "http://mazvlappro01.centralindia.cloudapp.azure.com";
    private String URL_SUBPART = "/installation/index.php";
    private String URL_TAKEPHOTO = "/installation/takephoto.php";
    private String DEFAULT_BASE_URL = "http://192.168.1.126/xibo-cms-1.8.3";
    private String DEFAULT_SECONDARY_URL = "http://192.168.1.126/xibo-cms-1.8.3";
    private String URL = DEFAULT_URL + URL_SUBPART;
    WebView webView;
    AddressInformation addressInformation;
    // GPSTracker class
    GPSTracker gps;

    double latitude;
    double longitude;

    private static final int REQUEST_CODE_PERMISSION = 2;
    private static final int REQUEST_PERMISSION = 20;
    private int QR_REQUEST = 10;
    private int PICTURE_ID = 101;

    private String QRCODE_INFORMATION = "";


    String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    private String IMAGE_FIRST_BASE64 = "";
    private String IMAGE_SECOND_BASE64 = "";
    private String FILE_NAME = "";
    private String FULL_FILE = "";
    private String MY_JS_FILE_NAME = "MyJSClient";
    private String STRING_EXTRA = "?a=1";
    private Uri FILE_URI = null;
    private Uri fileUri = null;
    private TextView tvErrorReport;
    private int locationRequest = 100;
    private LocationManager locationManager = null;
    LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_web_page);

            init();

            if (Build.VERSION.SDK_INT > 23) {
                if (checkSelfPermission()) {
                    initGps();
                    isGpsEnabled();
                    getAddressInformation();
                }
            } else {
                isGpsEnabled();
                initGps();
                getAddressInformation();
            }

            if (CommonFunctions.isNetworkAvailable(this)) {
                loadUrl(URL);
            } else {
                showMessage(getResources().getString(R.string.noNetwork));
            }
        } catch (Exception ex) {
            Toast.makeText(MainActivity.this, "Error" + "" + ex.toString(), Toast.LENGTH_LONG).show();
            tvErrorReport.setVisibility(View.VISIBLE);
            tvErrorReport.setText("Error" + ex.toString());
        }
    }

    void init() {
        webView = (WebView) findViewById(R.id.webView);

        addressInformation = new AddressInformation();


        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }


    /**
     * changes(function will checkSelfPermission) made(24.018) **by #Gyanesh
     **/
    public boolean checkSelfPermission() {
        boolean permissionEnabled = false;
        try {
            if (!hasPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION);
            } else {
                permissionEnabled = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return permissionEnabled;
    }

    /**
     * changes(function will check permission) made (24.018) by #Gyanesh
     **/
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    public void initGps() {
        gps = new GPSTracker(MainActivity.this);
        // check if GPS enabled
        if (gps.canGetLocation()) {
            latitude = gps.getLatitude();
            longitude = gps.getLongitude();

            initializeAddress();
            //displayInformation();
        } else {
            // can't get location
            // GPS or Network is not enabled
            showMessage(getResources().getString(R.string.gpsError));
        }
    }

    /**
     * function shows to enable the gps
     **/
    private void isGpsEnabled() {
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);

        // Check if enabled and if not send user to the GPS settings
        if (!enabled) {
            showMessage("Please Enable the Gps !");
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    void loadUrl(String url) throws Exception {
        webView.addJavascriptInterface(new MyJavascriptInterface(this), MY_JS_FILE_NAME);

        try {
            // webView.loadData(URL,"text/html","UTF-8");
            WebSettings webSettings = webView.getSettings();
            webSettings.setDomStorageEnabled(true);
            webSettings.setJavaScriptEnabled(true); // enable javascript
            webSettings.setSaveFormData(true);
            webSettings.setLoadWithOverviewMode(true);

            webView.loadUrl(url);

            webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
            webView.getSettings().setSupportMultipleWindows(false);
            webView.getSettings().setSupportZoom(false);
            webView.setVerticalScrollBarEnabled(false);
            webView.setHorizontalScrollBarEnabled(false);
            webView.getSettings().setAllowFileAccessFromFileURLs(true); //Maybe you don't need this rule
            webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
            webView.setWebViewClient(new MyBrowser());
            //changes for debugging
            if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        } catch (Exception ex) {
            throw new Exception("ex");
        }
    }

    String getData() {


        String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "    \n" +
                "    <head> \n" +
                "        <title></title>\n" +
                "       <meta charset=\"utf-8\">\n" +
                "        <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\">\n" +
                "        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "                    <base href=\"http://localhost/nGagePNB/installation/\" />\n" +
                "                    \n" +
                "       \n" +
                "        <script src=\"js/jquery.js\">" +
                "" +
                "" +
                "</script>\n" +
                "        \n" +
                "<!--        <script type=\"text/javascript\" src=\"js/instascan.min.js\"></script>    -->\n" +
                "        <!--===============================================================================================-->\n" +
                "        <link rel=\"icon\" type=\"image/png\" href=\"images/icons/favicon.ico\"/>\n" +
                "        <!--===============================================================================================-->\n" +
                "        <link rel=\"stylesheet\" type=\"text/css\" href=\"resource/vendor/bootstrap/css/bootstrap.min.css\">\n" +
                "        <!--===============================================================================================-->\n" +
                "        <link rel=\"stylesheet\" type=\"text/css\" href=\"fonts/font-awesome-4.7.0/css/font-awesome.min.css\">\n" +
                "        <!--===============================================================================================-->\n" +
                "        <link rel=\"stylesheet\" type=\"text/css\" href=\"resource/vendor/animate/animate.css\">\n" +
                "        <!--===============================================================================================-->\n" +
                "        <link rel=\"stylesheet\" type=\"text/css\" href=\"resource/vendor/css-hamburgers/hamburgers.min.css\">\n" +
                "        <!--===============================================================================================-->\n" +
                "        <link rel=\"stylesheet\" type=\"text/css\" href=\"resource/vendor/animsition/css/animsition.min.css\">\n" +
                "        <!--===============================================================================================-->\n" +
                "        <link rel=\"stylesheet\" type=\"text/css\" href=\"resource/vendor/select2/select2.min.css\">\n" +
                "        <!--===============================================================================================-->\n" +
                "        <link rel=\"stylesheet\" type=\"text/css\" href=\"resource/vendor/daterangepicker/daterangepicker.css\">\n" +
                "        <!--===============================================================================================-->\n" +
                "        <link rel=\"stylesheet\" type=\"text/css\" href=\"css/util.css\">\n" +
                "        <link rel=\"stylesheet\" type=\"text/css\" href=\"css/main.css\">\n" +
                "        <!--===============================================================================================-->\n" +
                "    </head>\n" +
                "\n" +
                "    <script>\n" +
                "        var url = document.location.toString();\n" +
                "        var hostname = location.hostname;\n" +
                "        if (hostname == \"localhost\") { //server\n" +
                "            var baseUrl = location.protocol + '//' + hostname + '/nGagePNB/installation/';\n" +
                "            //  var baseUrlservices = location.protocol + '//' + hostname + '/';\n" +
                "        } else {  //loclhost\n" +
                "            var baseUrl = location.protocol + '//' + hostname + '/installation/';\n" +
                "            // var baseUrlservices = location.protocol + '//' + hostname + '';\n" +
                "        }\n" +
                "        \n" +
                "           function Logout() {\n" +
                "                $.ajax({\n" +
                "                    type: \"post\",\n" +
                "                    url: baseUrl + \"api/logout.php\",\n" +
                "                    async: false,\n" +
                "                    success: function (msg) {\n" +
                "                        // alert(msg);\n" +
                "                        var obj = $.parseJSON(msg);\n" +
                "                        alert(obj.msg);\n" +
                "                        if (obj.status == true) {\n" +
                "                            window.location.href = \"index.php\";\n" +
                "                        } else {\n" +
                "                          \n" +
                "                        }\n" +
                "                    }, error: function (xhr, ajaxOptions, thrownError) {\n" +
                "                        //alert(thrownError);\n" +
                "                    }\n" +
                "                });\n" +
                "            }   \n" +
                "            \n" +
                "       \n" +
                "//     let scanner = new Instascan.Scanner({ video: document.getElementById('preview') });\n" +
                "//      scanner.addListener('scan', function (content) {\n" +
                "//         console.log(content);\n" +
                "//         $(\"#contentqr\").append(content);\n" +
                "//      });\n" +
                "//      Instascan.Camera.getCameras().then(function (cameras) {\n" +
                "//        if (cameras.length > 0) {\n" +
                "//          scanner.start(cameras[0]);\n" +
                "//        } else {\n" +
                "//          console.error('No cameras found.');\n" +
                "//        }\n" +
                "//      }).catch(function (e) {\n" +
                "//        console.error(e);\n" +
                "//      });  \n" +
                "//     \n" +
                "    </script>\n" +
                "    <body>\n" +
                "\n" +
                "    <style>\n" +
                "      .loader_center{\n" +
                "\t\tposition: absolute;\n" +
                "\t\tbottom: 50%;\n" +
                "\t\tleft: 50%;\n" +
                "\t\tmargin-top: -47px;\n" +
                "\t\tmargin-left: -47px;\n" +
                "\t\tz-index: 9999;\n" +
                "\t\twidth: auto;\n" +
                "\t\theight:auto;\n" +
                "\t}\n" +
                "        .error_message{\n" +
                "            color:red;\n" +
                "            font-size: 10px;\n" +
                "        }\n" +
                "        /*hold menu*/\n" +
                "\n" +
                "            #primary_nav_wrap\n" +
                "            {\n" +
                "            /*\tmargin-top:15px*/\n" +
                "            }\n" +
                "\n" +
                "            #primary_nav_wrap ul\n" +
                "            {\n" +
                "                    list-style:none;\n" +
                "                    position:relative;\n" +
                "                    float:left;\n" +
                "                    margin:0;\n" +
                "                    padding:0\n" +
                "            }\n" +
                "\n" +
                "            #primary_nav_wrap ul a\n" +
                "            {\n" +
                "                    display:block;\n" +
                "                    text-decoration:none;\n" +
                "                    font-weight:700;\n" +
                "                    font-size:12px;\n" +
                "                    line-height:32px;\n" +
                "                    padding:0 15px;\n" +
                "                    \n" +
                "                    color: #000;\n" +
                "                    white-space: nowrap;\n" +
                "            }\n" +
                "\n" +
                "            #primary_nav_wrap ul li\n" +
                "            {\n" +
                "                    position:relative;\n" +
                "                    margin:0;\n" +
                "                    padding:0;\n" +
                "                    z-index:999;\n" +
                "                    list-style:none;\n" +
                "            }\n" +
                "\n" +
                "\n" +
                "            #primary_nav_wrap ul li:hover\n" +
                "            {\n" +
                "            /*\tbackground:#f6f6f6*/\n" +
                "            }\n" +
                "\n" +
                "            #primary_nav_wrap ul ul\n" +
                "            {\n" +
                "                    display:none;\n" +
                "                    position:absolute;\n" +
                "                    top:90%;\n" +
                "                    left:0px;\n" +
                "            /*\tbackground:#fff;*/\n" +
                "                    padding:0;\n" +
                "            }\n" +
                "\n" +
                "            #primary_nav_wrap ul ul li\n" +
                "            {\n" +
                "                    float:none;\n" +
                "                    width:100%;\n" +
                "            }\n" +
                "\n" +
                "            #primary_nav_wrap ul ul a\n" +
                "            {\n" +
                "                    line-height:100%;\n" +
                "                    padding: 8px 61px;\n" +
                "                    margin-top: 5px;\n" +
                "            }\n" +
                "\n" +
                "            #primary_nav_wrap ul ul ul\n" +
                "            {\n" +
                "                    top:0;\n" +
                "                    left:100%\n" +
                "            }\n" +
                "\n" +
                "            #primary_nav_wrap ul li:hover > ul\n" +
                "            {\n" +
                "                    display:block\n" +
                "            }\n" +
                "            #primary_nav_wrap ul li a{\n" +
                "            /*    \tbackground:#797979;*/\n" +
                "               \n" +
                "                    box-shadow: 0 0 3px -1px rgba(0, 0, 0, 0.75);\n" +
                "            }\n" +
                "            #primary_nav_wrap ul li a:hover{\n" +
                "              \t     background:#797979;\n" +
                "                    color:#fff;\n" +
                "            }\n" +
                "          \n" +
                "            \n" +
                "            .float_right{\n" +
                "                float: right;\n" +
                "            }\n" +
                "    </style>\n" +
                "                <div class=\"float_right\">\n" +
                "                   <nav id=\"primary_nav_wrap\">\n" +
                "                      <ul>\n" +
                "                         <li><a href=\"javascript:void(0);\">  Welcome mahindra@ngage.com <span class=\"caret_down_white margin_left6px\"></span></a>\n" +
                "                         <ul>\n" +
                "                           <li>\n" +
                "                               <a href=\"javascript:void(0);\" onclick=\"Logout()\"> Logout</a>\n" +
                "                           </li>\n" +
                "                          </ul>\n" +
                "                       </ul>\n" +
                "                     </nav>\n" +
                "            </div>\n" +
                "             <div class=\"universalloader\"> </div>\n" +
                "    <div class=\"container-contact100\">\n" +
                "    \n" +
                "        <div class=\"contact100-form-title\" style=\"background-image: url(images/bg-01.png);height: 160px;width: 100%;\">\n" +
                "        <!-- <span class=\"contact100-form-title-1\">\n" +
                "                nGage WebOS Installation\n" +
                "        </span> -->\n" +
                "\n" +
                "        </div>\n" +
                "        \n" +
                "     \n" +
                "    <div class=\"wrap-contact100\">\n" +
                "    \n" +
                "  <form class=\"contact100-form validate-form\" id=\"mform\">\n" +
                "      \n" +
                "  \n" +
                "      \n" +
                "    <div class=\"wrap-input100 validate-input\" data-validate=\"Name is required\">\n" +
                "        <span class=\"label-input100\">Default Display Name <span class=\"error_message\" id=\"error_DefaultDisplayName\"></span>  </span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"DefaultDisplayName\"  id=\"DefaultDisplayName\" placeholder=\"Default Display Name\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>   \n" +
                "      \n" +
                "    <div class=\"wrap-input100 validate-input\" data-validate=\"Name is required\">\n" +
                "        <span class=\"label-input100\"> Display Name <span class=\"error_message\" id=\"error_DisplayName\"></span>  </span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"DisplayName\"  id=\"DisplayName\" placeholder=\" Display Name\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>  \n" +
                "      \n" +
                "      \n" +
                "    <div class=\"wrap-input100 validate-input\" data-validate=\"Name is required\">\n" +
                "        <span class=\"label-input100\"> Firmware Version <span class=\"error_message\" id=\"error_firmwareVersion\"></span>  </span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"firmwareVersion\"  id=\"firmwareVersion\" placeholder=\" Firmware Version\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>    \n" +
                "      \n" +
                "      \n" +
                "   <div class=\"wrap-input100 validate-input\" data-validate=\"Name is required\">\n" +
                "        <span class=\"label-input100\"> Hardware Key <span class=\"error_message\" id=\"error_hardwareKey\"></span>  </span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"hardwareKey\"  id=\"hardwareKey\" placeholder=\" Hardware Key\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>\n" +
                "      \n" +
                "      \n" +
                "    \n" +
                "   \n" +
                "      \n" +
                "    <div class=\"wrap-input100 validate-input\" data-validate=\"Name is required\">\n" +
                "        <span class=\"label-input100\">LG Serial Number  <span class=\"error_message\" id=\"error_SerialNumber\"></span>  </span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"SerialNumber\"  id=\"SerialNumber\" placeholder=\"Enter Serial Number\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"wrap-input100 validate-input\" data-validate=\"Name is required\">\n" +
                "        <span class=\"label-input100\">LG Model Name   <span class=\"error_message\" id=\"error_ModelName\"></span> </span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"ModelName\" id=\"ModelName\" placeholder=\"Enter Model Name\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"wrap-input100 validate-input\" data-validate=\"Name is required\">\n" +
                "        <span class=\"label-input100\">LG Mac Address  <span class=\"error_message\" id=\"error_MacAddress\"></span>  </span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"MacAddress\" id=\"MacAddress\"  placeholder=\"Enter Mac Address\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"wrap-input100 validate-input\" data-validate=\"Name is required\">\n" +
                "        <span class=\"label-input100\">Store Id   <span class=\"error_message\" id=\"error_StoreId\"></span>  </span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"StoreId\" id=\"StoreId\" placeholder=\"Enter Store Id\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"wrap-input100 validate-input\" data-validate=\"Name is required\">\n" +
                "        <span class=\"label-input100\">Store Manager Name  <span class=\"error_message\" id=\"error_ManagerName\"></span> </span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"ManagerName\" id=\"ManagerName\"  placeholder=\"Enter Store Manager Name\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"wrap-input100 validate-input\" data-validate=\"Name is required\">\n" +
                "        <span class=\"label-input100\">Store Manager Mobile Number  <span class=\"error_message\" id=\"error_MobileNumber\"></span> </span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"MobileNumber\" id=\"MobileNumber\" placeholder=\"Enter Store Manager Mobile Number\" maxlength=\"10\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"wrap-input100 validate-input\" data-validate = \"Valid email is required:ex@abc.xyz\">\n" +
                "        <span class=\"label-input100\">Store Manager Mail Id  <span class=\"error_message\" id=\"error_MailId\"></span> </span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"MailId\" id=\"MailId\" placeholder=\"Enter Store Manager Email Address\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>\n" +
                "\n" +
                "\n" +
                "\n" +
                "    <div class=\"wrap-input100 input100-select\">\n" +
                "        <span class=\"label-input100\">Screen Position  <span class=\"error_message\" id=\"error_ScreenPosition\"></span>  </span>\n" +
                "        <div id=\"ScreenPositionReplace\">\n" +
                "<!--            <input type=\"text\" id=\"ScreenPosition\" />-->\n" +
                "         \n" +
                "<!--             <select class=\"selection-2\" name=\"ScreenPosition\" >\n" +
                "                <option>Vehicle Lane 01</option>\n" +
                "                <option>Vehicle Lane 02</option>\n" +
                "                <option>Vehicle Lane 03</option>\n" +
                "                <option>Vehicle Lane 04</option>\n" +
                "                <option>Vehicle Lane 05</option>\n" +
                "             </select>-->\n" +
                "        </div>\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>\n" +
                "    \n" +
                "\n" +
                "    <div class=\"wrap-input100 validate-input\">\n" +
                "        <span class=\"label-input100\">Latitude  </span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"latitude\"  id=\"latitude\" placeholder=\"Latitude\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>\n" +
                "    \n" +
                "    \n" +
                "    <div class=\"wrap-input100 validate-input\">\n" +
                "        <span class=\"label-input100\">Longitude   </span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"longitude\"  id=\"longitude\"  placeholder=\"Longitude\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"wrap-input100 validate-input\">\n" +
                "        <span class=\"label-input100\">City</span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"city\" id=\"city\"  placeholder=\"City\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"wrap-input100 validate-input\">\n" +
                "        <span class=\"label-input100\">State</span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"state\" id=\"state\" placeholder=\"State\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div> \n" +
                "    \n" +
                "   \n" +
                "    <div class=\"wrap-input100 validate-input\">\n" +
                "        <span class=\"label-input100\">Country</span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"country\" id=\"country\" placeholder=\"Country\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>  \n" +
                "    \n" +
                "    \n" +
                "    <div class=\"wrap-input100 validate-input\">\n" +
                "        <span class=\"label-input100\">Locality</span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"locality\" id=\"locality\" placeholder=\"Locality\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>  \n" +
                "    \n" +
                "    <div class=\"wrap-input100 validate-input\">\n" +
                "        <span class=\"label-input100\">Postal Code</span>\n" +
                "        <input class=\"input100\" type=\"text\" name=\"postalcode\" id=\"postalcode\" placeholder=\"postalcode\">\n" +
                "        <span class=\"focus-input100\"></span>\n" +
                "    </div>  \n" +
                "    \n" +
                "    \n" +
                "    <div class=\"wrap-input100 validate-input\">        \n" +
                "      <span class=\"label-input100\">Qr Code Link</span>\n" +
                "      <input class=\"input100\" type=\"text\" name=\"qrcode\" id=\"qrcode\" placeholder=\"Qr Code Link\">\n" +
                "      <span class=\"focus-input100\"></span>\n" +
                "    </div> \n" +
                "    \n" +
                "    <div class=\"wrap-input100 validate-input\">        \n" +
                "      <span class=\"label-input100\">Display Screen Shot Base64</span>\n" +
                "      <textarea id=\"DisplayScreenShotBase64\" name=\"DisplayScreenShotBase64\"></textarea>\n" +
                "    </div> \n" +
                "    \n" +
                "   \n" +
                "    <div class=\"wrap-input100 validate-input\">        \n" +
                "      <span class=\"label-input100\">Report Screen Shot Base64</span>\n" +
                "       <textarea id=\"ReportScreenShotBase64\" name=\"ReportScreenShotBase64\"></textarea>\n" +
                "    </div>    \n" +
                "      \n" +
                "    \n" +
                "    <!-- <div class=\"wrap-input100 input100-select\">\n" +
                "            <span class=\"label-input100\">Budget</span>\n" +
                "            <div>\n" +
                "                    <select class=\"selection-2\" name=\"budget\">\n" +
                "                            <option>Select Budget</option>\n" +
                "                            <option>1500 $</option>\n" +
                "                            <option>2000 $</option>\n" +
                "                            <option>2500 $</option>\n" +
                "                    </select>\n" +
                "            </div>\n" +
                "            <span class=\"focus-input100\"></span>\n" +
                "    </div> -->\n" +
                "\n" +
                "\n" +
                "\n" +
                "    <div class=\"container-contact100-form-btn\">\n" +
                "        <div class=\"wrap-contact100-form-btn\">\n" +
                "            <div class=\"contact100-form-bgbtn\"></div>\n" +
                "<!--            <a href=\"success.php\">-->\n" +
                "                <button class=\"contact100-form-btn\" id=\"mformButton\">\n" +
                "                    <span>\n" +
                "                        Submit\n" +
                "                        <i class=\"fa fa-long-arrow-right m-l-7\" aria-hidden=\"true\"></i>\n" +
                "                    </span>\n" +
                "                </button>\n" +
                "<!--            </a>-->\n" +
                "        </div>\n" +
                "    </div>\n" +
                "    </form> \n" +
                "</div>\n" +
                "\n" +
                "\n" +
                "</div>\n" +
                "\n" +
                "\n" +
                "\n" +
                "<div id=\"dropDownSelect1\"></div>\n" +
                "\n" +
                "\n" +
                "\n" +
                "<script src=\"js/validation.js\"></script>\n" +
                "\n" +
                "\n" +
                "<script src=\"js/main.js\"></script>\n" +
                "\n" +
                "\n" +
                "<script src=\"js/jquery-ui.js\"></script>   \n" +
                "<link href=\"css/jquery-ui.css\" rel=\"stylesheet\" type=\"text/css\"/>\n" +
                "       \n" +
                "\n" +
                "<script type=\"text/javascript\">\n" +
                " $.ajaxPrefilter(function( options, original_Options, jqXHR ) {\n" +
                "    options.async = true;\n" +
                "});\n" +
                " //  firmwareVersion\n" +
                "//  hardwareKey \n" +
                "//  displayName\n" +
                "// window.MyJSClient getCity  getState ,  getCountry ,getLocality,getPostalcode ,\n" +
                "// getLongitude ,getLatitude   getQrCodeText getDisplayScreenShotBase64 getReportScreenShotBase64\n" +
                "\n" +
                "\n" +
                "window.MyJSClient={\n" +
                "            getCity :function(){\n" +
                "                return \"Bangalore\";\n" +
                "            },\n" +
                "             getState :function(){\n" +
                "                return \"Karanataka\";\n" +
                "            },\n" +
                "            getCountry :function(){\n" +
                "                return \"India\";\n" +
                "            },\n" +
                "             getLocality :function(){\n" +
                "                return \"Richmond\";\n" +
                "            },\n" +
                "             getPostalcode :function(){\n" +
                "                return \"560037\";\n" +
                "            },\n" +
                "             getLongitude :function(){\n" +
                "                return \"22.23312\";\n" +
                "            },\n" +
                "             getLatitude :function(){\n" +
                "                return \"77.3424242\";\n" +
                "            },\n" +
                "             getQrCodeText :function(){\n" +
                "                var jsondata='{\"serialNumber\":\"801PMJQ001788\",\"firmwareVersion\":\"04.02.20\",\"macAddress\":\"7C:1C:4E:95:C8:01\",\"hardwareKey\":\"a8a0af7f-6b4f-38c4-8a2f-af18c046d5d6\",\"displayName\":\"webOS_ATPL\"}';\n" +
                "                return jsondata;\n" +
                "            },\n" +
                "\n" +
                "             getDisplayScreenShotBase64 :function(){\n" +
                "                return \"iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAA7DAAAOwwHHb6hkAAAAB3RJTUUH4gUHDDYXwbpKNgAAAcNJREFUSMe11TFrk2EQB/BfrCAuLXQrlSpCoYOCYtVPoMZBUbq6+AEcXHQ0ol3ST+AgioKbX8DF1XZwUNGWdrCVWBDqEBQHTV6XC5wvSZMQc/Dy3nN3z909d//nHsZMlQFsJnEe85gO2Xds4Q2aowSvo4EfaKGIrxWyBlYGTPQfOoO95LDft4ezg5boBp5iIsk+YA27sZ7BOZxINm2cxrt+mf9JmX3DZUx1sZ1CNWwK1JLuNk51O00uywaOJP3DpFtO8uO4ldb3wuYj5nKAeinz2VICtaSvpaSudXHejv+TDMVGclDtUpIc4H7IHsV6BXdLjf+UT3AhYFfgfY+a5wB38KCUbea3sQAHYvM8Dge/hp99YHwIj/G2hMRK9O4S1nOA6cTvBpL2owl8xpUSLDdwseM8Bxh2rHT4r1jCDr5Ew3dwDNfhYJot7Qg4E/LyKV734LdwNYKuR+1fRgkN0+RBaCEa/Ct8GgamvS5ah+YCmkX4msw9aOJFMn7W5aLlkv0u6Y7iVQea4as57KhY7nGCWWyWJmtlmGFXHXDYFbF3cdRxXaRxfTLZtHATz8f14CyO68ms71fzUR79TayO8uj/F/oLTOi2/0plQI8AAAAASUVORK5CYII=\";\n" +
                "            },\n" +
                "\n" +
                "            getReportScreenShotBase64 :function(){\n" +
                "                return \"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\";\n" +
                "            }\n" +
                "        };\n" +
                "\n" +
                "\n" +
                "    function setDisplayName(prefix,city,SerialNumber,ScreenPosition){\n" +
                "       var DisplayName=prefix+'_'+city+'_'+SerialNumber+'_'+ ScreenPosition;\n" +
                "       $(\"#DisplayName\").val(DisplayName);\n" +
                "    }\n" +
                "    \n" +
                "    \n" +
                "    function extractQrCodeJSON(getgetQrCodeTexteromAndroid){ \n" +
                "        var obj=$.parseJSON(getgetQrCodeTexteromAndroid);\n" +
                "        \n" +
                "        //{\"serialNumber\":\"801PMJQ001788\",\"firmwareVersion\":\"04.02.20\",\"macAddress\":\"7C:1C:4E:95:C8:01\",\"hardwareKey\":\"919de631d9b619e2c19b2301818d8bb6\",\"displayName\":\"webOS_ATPL\"}\n" +
                "         if(!validation.isset(obj.serialNumber)){\n" +
                "            return false;\n" +
                "         }else{\n" +
                "               $(\"#SerialNumber\").val(obj.serialNumber); \n" +
                "         }\n" +
                "         \n" +
                "         if(!validation.isset(obj.firmwareVersion)){\n" +
                "                return false;\n" +
                "         }else{\n" +
                "              $(\"#firmwareVersion\").val(obj.firmwareVersion); \n" +
                "         }\n" +
                "         \n" +
                "         if(!validation.isset(obj.hardwareKey)){\n" +
                "               return false;\n" +
                "         }else{\n" +
                "               $(\"#hardwareKey\").val(obj.hardwareKey);\n" +
                "         }\n" +
                "         \n" +
                "         if(!validation.isset(obj.displayName)){\n" +
                "           return false;\n" +
                "         }else{\n" +
                "                $(\"#DefaultDisplayName\").val(obj.displayName);  \n" +
                "         }\n" +
                "         \n" +
                "         \n" +
                "         if(!validation.isset(obj.macAddress)){\n" +
                "              return false;\n" +
                "         \n" +
                "         }else{\n" +
                "               $(\"#MacAddress\").val(obj.macAddress);   \n" +
                "         }\n" +
                "        return true;\n" +
                "    }\n" +
                "     function setError(message,idText,classText){\n" +
                "        // var classText;\n" +
                "        var class_Text_selector=\".\"+classText;\n" +
                "        var id_Text_selector=\"#\"+idText;\n" +
                "        $(class_Text_selector).text('');\n" +
                "        $(id_Text_selector).text(message);\n" +
                "    }\n" +
                "     \n" +
                " $(document).ready(function(){\n" +
                "        $.ajax({\n" +
                "            type: \"post\",\n" +
                "            url:baseUrl+\"api/LayoutList.php\",\n" +
                "            async: false,\n" +
                "            success: function (msg) {\n" +
                "                 var obj = $.parseJSON(msg);\n" +
                "                 var searchArray=[];\n" +
                "                   console.log(obj);\n" +
                "                   \n" +
                "              if(validation.isset(obj.data)){\n" +
                "               var   select=\"<select class=\\\"selection-2\\\" name=\\\"ScreenPosition\\\" id=\\\"ScreenPosition\\\">\";\n" +
                "                     select+=\"<option value=\\\"\\\"> Select Screen Position </option>\";\n" +
                "                if (obj.status == true) {\n" +
                "                    $.each(obj.data,function(key,val){\n" +
                "                       select+=\"<option value=\\\"\"+val[\"layoutId\"]+\"\\\">\"+val[\"layout\"]+\"</option>\";\n" +
                "                   });\n" +
                "                   select+=\"</select>\";\n" +
                "                  console.log(searchArray);    \n" +
                "               \n" +
                "                            //                       $( \"#ScreenPosition\" ).autocomplete({\n" +
                "                            //                        source:searchArray\n" +
                "                            //                     });\n" +
                "                  }\n" +
                "                 $(\"#ScreenPositionReplace\").html(select);\n" +
                "               }else{\n" +
                "                   //alert(obj.msg);\n" +
                "               }\n" +
                "            }, error: function (xhr, ajaxOptions, thrownError) {\n" +
                "                //alert(thrownError);\n" +
                "            }\n" +
                "        });\n" +
                "     \n" +
                "     \n" +
                "\n" +
                "    $(\"#loginFormBuutton\").click(function(event){\n" +
                "        \n" +
                "       event.preventDefault();\n" +
                "         var email=validation.email({\n" +
                "             id:'email',\n" +
                "             msg:'In valid Email'\n" +
                "         });   \n" +
                "         if(email){\n" +
                "             setError(email,'error_email','error_message');\n" +
                "             return false;\n" +
                "         }\n" +
                "\n" +
                "        var password=validation.password({\n" +
                "             id:'password'\n" +
                "         });   \n" +
                "         if(password){\n" +
                "             setError(password,'error_password','error_message');\n" +
                "             return false;\n" +
                "         }\n" +
                "         \n" +
                "         \n" +
                "         window.MyJSClient.getDeviceID=function(){\n" +
                "           return \"sadasdasda\";\n" +
                "         }\n" +
                "         \n" +
                "         var deviceID= window.MyJSClient.getDeviceID();\n" +
                "         var serialObject=$(\"#theform\").serializeArray();\n" +
                "         serialObject.push({name:'deviceID',value:deviceID});\n" +
                "  \n" +
                "         //http://192.168.1.9/noqapi/login.php?email=simran@absonstech.com&password=simran&windowno=5\n" +
                "        //  $(\"#submitbutton\").attr(\"disabled\",\"disabled\").css(\"cursor\",\"default\");\n" +
                "           $(\".universalloader\").html(\"<img  class=\\\"loader_center\\\" src='images/loading.cms' />\").css(\"visibility\", \"visible\");\n" +
                "           $.ajax({\n" +
                "             type:\"post\",\n" +
                "             url:baseUrl+\"api/login.php\",\n" +
                "            // async:true,\n" +
                "             data:serialObject,\n" +
                "             success:function(msg){\n" +
                "             //$(\"#submitbutton\").removeAttr(\"disabled\").removeAttr(\"style\");\n" +
                "              $(\".universalloader\").css(\"visibility\",\"hidden\"); \n" +
                "                 var obj=$.parseJSON(msg);\n" +
                "                 \n" +
                "                 if(obj.status==false){\n" +
                "                     alert(obj.msg);\n" +
                "                 }else{\n" +
                "                   window.localStorage.setItem(\"email\",$(\"#email\").val());  \n" +
                "                   window.location.href=\"steps.php\";   \n" +
                "                 }\n" +
                "              }, error: function( xhr, ajaxOptions, thrownError ){\n" +
                "                    $(\"#loader\").css(\"visibility\",\"hidden\"); \n" +
                "                     alert(thrownError);\n" +
                "              } \n" +
                "            }); \n" +
                "    });\n" +
                "    \n" +
                "    \n" +
                "    \n" +
                "    \n" +
                "        $(\"#mformButton\").click(function(event){ \n" +
                "          event.preventDefault();\n" +
                "         // SerialNumber ModelName MacAddress StoreId ManagerName MailId \n" +
                "         var getgetQrCodeTexteromAndroid= validation.isset(window.MyJSClient)?window.MyJSClient.getQrCodeText():'';\n" +
                "                \n" +
                "         var jdata=extractQrCodeJSON(getgetQrCodeTexteromAndroid);\n" +
                "         if(jdata==false){\n" +
                "             alert(\"JSON extaction issue\");\n" +
                "             return false;\n" +
                "         }\n" +
                "        \n" +
                "        var getCityFromAndroid=validation.isset(window.MyJSClient)?window.MyJSClient.getCity():'';\n" +
                "        var getStateFromAndroid=validation.isset(window.MyJSClient)?window.MyJSClient.getState():'';\n" +
                "        var getCountryromAndroid=validation.isset(window.MyJSClient)?window.MyJSClient.getCountry():'';\n" +
                "        var getLocalityromAndroid= validation.isset(window.MyJSClient)?window.MyJSClient.getLocality():'';\n" +
                "        var getPostalcoderomAndroid= validation.isset(window.MyJSClient)?window.MyJSClient.getPostalcode():'';\n" +
                "        var getLatitudeeromAndroid= validation.isset(window.MyJSClient)?window.MyJSClient.getLatitude():'';\n" +
                "        var getLongituderomAndroid= validation.isset(window.MyJSClient)?window.MyJSClient.getLongitude():'';\n" +
                "         var city=getCityFromAndroid.trim(); \n" +
                "         var state=getStateFromAndroid.trim(); \n" +
                "         var country=getCountryromAndroid.trim(); \n" +
                "         var locality=getLocalityromAndroid.trim(); \n" +
                "         var postalcode=getPostalcoderomAndroid.trim(); \n" +
                "               \n" +
                "         $(\"#city\").val(city);  \n" +
                "         $(\"#state\").val(state);  \n" +
                "         $(\"#country\").val(country);  \n" +
                "         $(\"#locality\").val(locality);   \n" +
                "         $(\"#postalcode\").val(postalcode); \n" +
                "         \n" +
                "         // window.MyJSClient getCity  getState ,  getCountry ,getLocality,getPostalcode ,getLongitude ,getLatitude   getQrCodeText getDisplayScreenShotBase64 getReportScreenShotBase64\n" +
                "       \n" +
                "        var getDisplayScreenShotBase64= validation.isset(window.MyJSClient)?window.MyJSClient.getDisplayScreenShotBase64():'';\n" +
                "        var getReportScreenShotBase64= validation.isset(window.MyJSClient)?window.MyJSClient.getReportScreenShotBase64():'';\n" +
                "        $(\"#latitude\").val(getLatitudeeromAndroid.trim()); \n" +
                "        $(\"#longitude\").val(getLongituderomAndroid.trim());\n" +
                "        $(\"#qrcode\").val(getgetQrCodeTexteromAndroid.trim());\n" +
                "        $(\"#DisplayScreenShotBase64\").text(getDisplayScreenShotBase64.trim()); \n" +
                "        $(\"#ReportScreenShotBase64\").text(getReportScreenShotBase64.trim());   \n" +
                "\n" +
                "\n" +
                "         \n" +
                "        \n" +
                "                \n" +
                "         var SerialNumber=$(\"#SerialNumber\").val().trim();\n" +
                "         var ModelName=$(\"#ModelName\").val().trim();\n" +
                "         var MacAddress=$(\"#MacAddress\").val().trim();\n" +
                "         var StoreId=$(\"#StoreId\").val().trim(); \n" +
                "         var ManagerName=$(\"#ManagerName\").val().trim(); \n" +
                "         var MailId=$(\"#MailId\").val().trim();   \n" +
                "         var ScreenPosition=$(\"#ScreenPosition\").val().trim(); \n" +
                "         \n" +
                "         \n" +
                "         \n" +
                "         \n" +
                "         var SerialNumberCheck = validation.emptyCheck({\n" +
                "            id: 'SerialNumber',\n" +
                "            msg: 'Serial Number should not be Empty.'\n" +
                "        });\n" +
                "        \n" +
                "        if (SerialNumberCheck) {\n" +
                "            setError(SerialNumberCheck,'error_SerialNumber','error_message');\n" +
                "            return false;\n" +
                "        }\n" +
                "        \n" +
                "        \n" +
                "\n" +
                "        var ModelNameCheck = validation.emptyCheck({\n" +
                "            id: 'ModelName',\n" +
                "            msg: 'Model Name should not be Empty.'\n" +
                "        });\n" +
                "        if (ModelNameCheck) {\n" +
                "            setError(ModelNameCheck,'error_ModelName','error_message');\n" +
                "            return false;\n" +
                "        }\n" +
                "        \n" +
                "        var MacAddressCheck = validation.emptyCheck({\n" +
                "            id: 'MacAddress',\n" +
                "            msg: 'Mac Address should not be Empty.'\n" +
                "        });\n" +
                "        if (MacAddressCheck) {\n" +
                "            setError(MacAddressCheck,'error_MacAddress','error_message');\n" +
                "            return false;\n" +
                "        }\n" +
                "        \n" +
                "       \n" +
                "        var StoreIdCheck= validation.emptyCheck({\n" +
                "            id: 'StoreId',\n" +
                "            msg: 'Store Id should not be Empty.'\n" +
                "        });\n" +
                "        if (StoreIdCheck) {\n" +
                "            setError(StoreIdCheck,'error_StoreId','error_message');\n" +
                "            return false;\n" +
                "        }\n" +
                "        \n" +
                "        \n" +
                "        var ManagerNameCheck = validation.emptyCheck({\n" +
                "            id: 'ManagerName',\n" +
                "            msg: 'Manager Name should not be Empty.'\n" +
                "        });\n" +
                "        if (ManagerNameCheck) {\n" +
                "            setError(ManagerNameCheck,'error_ManagerName','error_message');\n" +
                "            return false;\n" +
                "        }\n" +
                "        \n" +
                "     //    SerialNumber ModelName MacAddress StoreId ManagerName MailId \n" +
                "          //    ScreenPosition latitude longitude city  state country locality  postalcode  qrcode\n" +
                "              \n" +
                "        \n" +
                "//       var ManagerName = validation.emptyCheck({\n" +
                "//            id: 'ManagerName',\n" +
                "//            msg: 'ManagerName should not be Empty.'\n" +
                "//        });\n" +
                "//        if (ManagerName) {\n" +
                "//            setError(ManagerName,'error_ManagerName','error_message');\n" +
                "//            return false;\n" +
                "//        }\n" +
                "            \n" +
                "        var MailIdCheck=validation.email({\n" +
                "             id:'MailId',\n" +
                "             msg:'In valid Email'\n" +
                "         });   \n" +
                "         if(MailIdCheck){\n" +
                "             setError(MailIdCheck,'error_MailId','error_message');\n" +
                "             return false;\n" +
                "         }\n" +
                "         \n" +
                "         \n" +
                "         var ScreenPositionCheck=validation.emptyCheck({\n" +
                "             id:'ScreenPosition',\n" +
                "             msg:'please select Screen Position'\n" +
                "         });   \n" +
                "         if(ScreenPositionCheck){\n" +
                "             setError(ScreenPositionCheck,'error_ScreenPosition','error_message');\n" +
                "             return false;\n" +
                "         }\n" +
                "          \n" +
                "        \n" +
                "     \n" +
                "        var ScreenPositionText= $(\"#ScreenPosition option:selected\").text().trim();\n" +
                "        \n" +
                "       \n" +
                "    \n" +
                "         var getDisplayScreenShotBase64Check=validation.emptyCheck({\n" +
                "             value:getDisplayScreenShotBase64,\n" +
                "             msg:'Image Screen Shot Base64 should not be empty'\n" +
                "         });   \n" +
                "         if(getDisplayScreenShotBase64Check){\n" +
                "             alert(getDisplayScreenShotBase64Check);\n" +
                "             return false;\n" +
                "         }\n" +
                "         \n" +
                "         var getReportScreenShotBase64Check=validation.emptyCheck({\n" +
                "             value:getReportScreenShotBase64,\n" +
                "             msg:'Report Screen Shot Base64 should not be empty'\n" +
                "         });   \n" +
                "         if(getReportScreenShotBase64Check){\n" +
                "             alert(getReportScreenShotBase64Check);\n" +
                "             return false;\n" +
                "         }\n" +
                "         \n" +
                "    \n" +
                "          var getgetQrCodeTexteromAndroidCheck=validation.emptyCheck({\n" +
                "             value:getgetQrCodeTexteromAndroid,\n" +
                "             msg:'Please scan Qr Code during DSS setup ( Qr Code link is missing ) .'\n" +
                "         });   \n" +
                "         if(getgetQrCodeTexteromAndroidCheck){\n" +
                "             alert(getgetQrCodeTexteromAndroidCheck);\n" +
                "             return false;\n" +
                "         }\n" +
                "        \n" +
                "         var latitudeCheck=validation.emptyCheck({\n" +
                "             value:getLatitudeeromAndroid,\n" +
                "             msg:'Please enable the GPS location to identify the right location.'\n" +
                "         });   \n" +
                "         if(latitudeCheck){\n" +
                "             alert(latitudeCheck);\n" +
                "             return false;\n" +
                "         }\n" +
                "         \n" +
                "         var longitudeCheck=validation.emptyCheck({\n" +
                "             value:getLongituderomAndroid,\n" +
                "             msg:'Please enable the GPS location to identify the right location.'\n" +
                "         });   \n" +
                "         \n" +
                "         if(longitudeCheck){\n" +
                "             alert(longitudeCheck);\n" +
                "             return false;\n" +
                "          }\n" +
                "          \n" +
                "          \n" +
                "     \n" +
                "       \n" +
                "         \n" +
                "         var address=city+state+country+locality+postalcode;\n" +
                "       \n" +
                "         var cityCheck=validation.emptyCheck({\n" +
                "             value:city,\n" +
                "             msg:'city should not be empty.'\n" +
                "         });   \n" +
                "         \n" +
                "        if(cityCheck){\n" +
                "         alert(cityCheck);\n" +
                "          return false;\n" +
                "        }\n" +
                "       \n" +
                "       \n" +
                "         var addressCheck=validation.emptyCheck({\n" +
                "             value:address,\n" +
                "             msg:'Address should not be empty'\n" +
                "         });   \n" +
                "         \n" +
                "         if(addressCheck){\n" +
                "            alert(addressCheck);\n" +
                "             return false;\n" +
                "          }\n" +
                "            // SerialNumber ModelName MacAddress StoreId ManagerName MailId \n" +
                "     \n" +
                "        setDisplayName(\"MAH\",city,SerialNumber,ScreenPositionText);\n" +
                "            \n" +
                "         var DisplayNameCheck=validation.emptyCheck({\n" +
                "             value:DisplayName,\n" +
                "             msg:'Start Your Installation Put CMS Url, CMS Key thereafter try to Submit this form (  Display Name should not be empty)'\n" +
                "         });   \n" +
                "         \n" +
                "          if(DisplayNameCheck){\n" +
                "            alert(DisplayNameCheck);\n" +
                "             return false;\n" +
                "          }\n" +
                "          \n" +
                "         var formObj=   $(\"#mform\").serializeArray();\n" +
                "         \n" +
                "         formObj.push({\n" +
                "             \"name\":'QrCode',\n" +
                "             \"value\":JSON.stringify(getgetQrCodeTexteromAndroid),\n" +
                "         });\n" +
                "         formObj.push({\n" +
                "             \"name\":'ScreenPositionText',\n" +
                "             \"value\":ScreenPositionText,\n" +
                "         });\n" +
                "         \n" +
                "          \n" +
                "          \n" +
                "       $(\".universalloader\").html(\"<img  class=\\\"loader_center\\\" src='images/loading.cms' />\").css(\"visibility\", \"visible\");     \n" +
                "       $.ajax({\n" +
                "            type: \"post\",\n" +
                "            url:baseUrl+\"api/assignLayouToDisplay.php\",\n" +
                "            data:formObj,\n" +
                "            async: false,\n" +
                "            success: function (msg) {\n" +
                "              var obj = $.parseJSON(msg);\n" +
                "               console.log(obj);\n" +
                "              $(\".universalloader\").css(\"visibility\",\"hidden\");       \n" +
                "              if(validation.isset(obj.data)){\n" +
                "              \n" +
                "               }else{\n" +
                "                   //alert(obj.msg);\n" +
                "               }\n" +
                "            }, error: function (xhr, ajaxOptions, thrownError) {\n" +
                "                //alert(thrownError);\n" +
                "            }\n" +
                "        });    \n" +
                "           \n" +
                "           \n" +
                "       });\n" +
                "  \n" +
                "    }); \n" +
                " </script>\n" +
                "</body>\n" +
                "</html>";

        return html;

    }

    public String initializeAddress() {
        StringBuilder stringBuilder = new StringBuilder();
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(MainActivity.this, Locale.getDefault());

        if (gps != null) {
            try {
                if (geocoder.isPresent()) {
                    addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                    String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                    String city = addresses.get(0).getLocality();
                    String state = addresses.get(0).getAdminArea();
                    String country = addresses.get(0).getCountryName();
                    String postalCode = addresses.get(0).getPostalCode();
                    String knownName = addresses.get(0).getFeatureName();

                    addressInformation.setCity(city);
                    addressInformation.setState(state);
                    addressInformation.setCountry(country);
                    addressInformation.setPostalCode(postalCode);
                    addressInformation.setLocality(knownName);

                    if (address != null) {
                        stringBuilder.append(address);
                    }
                    if (city != null) {
                        stringBuilder.append(city);
                    }
                    if (state != null) {
                        stringBuilder.append(state);
                    }
                    if (country != null) {
                        stringBuilder.append(country);
                    }
                    if (postalCode != null) {
                        stringBuilder.append(postalCode);
                    }
                    if (knownName != null) {
                        stringBuilder.append(knownName);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return stringBuilder.toString();
    }

    public String getAddressInformation() {
        StringBuilder stringBuilder = new StringBuilder();
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        if (gps != null) {
            try {
                addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                String city = addresses.get(0).getLocality();
                String state = addresses.get(0).getAdminArea();
                String country = addresses.get(0).getCountryName();
                String postalCode = addresses.get(0).getPostalCode();
                String knownName = addresses.get(0).getFeatureName();
                if (address != null) {
                    stringBuilder.append(address);
                }
                if (city != null) {
                    stringBuilder.append(city);
                }
                if (state != null) {
                    stringBuilder.append(state);
                }
                if (country != null) {
                    stringBuilder.append(country);
                }
                if (postalCode != null) {
                    stringBuilder.append(postalCode);
                }
                if (knownName != null) {
                    stringBuilder.append(knownName);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return stringBuilder.toString();
    }

    void openQrCodeActivity(String url) {
        PrefFile.saveUrl(MainActivity.this, url);
        Intent i = new Intent(this, ScannedBarcodeActivity.class);
        startActivityForResult(i, QR_REQUEST);
    }

    /**
     * creating pdf file
     **/
    public void stringtopdf(String data) {
        String extstoragedir = Environment.getExternalStorageDirectory().toString();
        File fol = new File(extstoragedir, "pdf");
        File folder = new File(fol, "pdf");
        if (!folder.exists()) {
            boolean bool = folder.mkdir();
        }
        try {
            final File file = new File(folder, "sample.pdf");
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(100, 100, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            canvas.drawText(data, 10, 10, paint);
            document.finishPage(page);
            document.writeTo(fOut);
            document.close();
        } catch (IOException e) {
            Log.i("error", e.getLocalizedMessage());
        }
    }

    private void viewPdf(String file, String directory) {

        File pdfFile = new File(Environment.getExternalStorageDirectory() + "/" + directory + "/" + file);
        Uri path = Uri.fromFile(pdfFile);

        // Setting the intent for pdf reader
        Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
        pdfIntent.setDataAndType(path, "application/pdf");
        pdfIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        try {
            startActivity(pdfIntent);
        } catch (ActivityNotFoundException e) {
            //Toast.makeText(TableActivity.this, "Can't read pdf file", Toast.LENGTH_SHORT).show();
        }
    }


    void openCamera(int pictureID) {

        try {

            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

          /*  FILE_URI = CameraUtils.getOutputMediaFileUri(this);

            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,FILE_URI);*/


            startActivityForResult(cameraIntent, pictureID);

        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    /**
     * showing message) ---#Gyanesh
     **/
    void showMessage(String message) {

        Toast.makeText(MainActivity.this, "" + message, Toast.LENGTH_SHORT).show();

    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public void showCustomDialog(final Context context, final String urlToShow) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_url, null, false);
        //findByIds(view);  /*HERE YOU CAN FIND YOU IDS AND SET TEXTS OR BUTTONS*/
        ((Activity) context).getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.setContentView(view);


        //final EditText editText=(EditText)view.findViewById(R.id.et_url_dialogue);
        TextView textView = (TextView) view.findViewById(R.id.tv_close);

 /*
        //checking if url to show is main
        if(urlToShow.equals(Constants.URL_MAIN)) {
            if (PrefFile.getMainUrl(WebPage.this) != null) {

                editText.setText(PrefFile.getMainUrl(WebPage.this));
            } else {
                editText.setText(DEFAULT_BASE_URL);

            }
        }


        //checking if url to show is secondary
        if(urlToShow.equals(Constants.URL_SECONDARY)){

            if(PrefFile.getMainUrl(WebPage.this)!=null){

                editText.setText(PrefFile.getSecondaryUrl(WebPage.this));

            }else{
                editText.setText(DEFAULT_SECONDARY_URL);

            }

        }

*/


        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                dialog.dismiss();


            }
        });


        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        final Window window = dialog.getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        dialog.show();
    }

  /*  @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }*/


  /*  @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.url:


                showCustomDialog(WebPage.this,Constants.URL_MAIN);
                return true;

            case R.id.urlSecond:

                showCustomDialog(WebPage.this,Constants.URL_SECONDARY);

                return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == RESULT_OK) {


            //request code for qrcode
            if (requestCode == 10) {


                String result = data.getStringExtra(Constants.QR_RESULT);

                QRCODE_INFORMATION = result;

                try {

                    PrefFile.saveQrCode(MainActivity.this, QRCODE_INFORMATION);

                    String dataRefined = QRCODE_INFORMATION.substring(QRCODE_INFORMATION.indexOf("{"), QRCODE_INFORMATION.length());

                    QRCODE_INFORMATION = dataRefined;

                    PrefFile.saveQrCode(MainActivity.this, QRCODE_INFORMATION);

                } catch (Exception ex) {
                    ex.toString();
                }
                startActivityAgain();

            }

            if (requestCode == 101) {


                try {

                    Bitmap bp = (Bitmap) data.getExtras().get("data");

                    // Uri tempUri = data.getData(); //getImageUri(getApplicationContext(), bp);


                    //for rotation issue
                    // Bitmap bp=   CameraUtils.convertImagePathToBitmap(FILE_URI.getPath(),false);

                    // Bitmap rotated=rotateImageIfRequired( bp,FILE_URI);

                    decodeImage(bp, IMAGE_FIRST_BASE64);
                    //capturedImage.setImageBitmap(bp);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }


                String url = PrefFile.getCaptureImageUrl(MainActivity.this);

                if (url != null) {
                    try {

                        loadUrl(url);

                    } catch (Exception e) {


                        tvErrorReport.setText("" + e);

                    }
                }

            }


        }


    }


    private static Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {

        ExifInterface ei = new ExifInterface(selectedImage.getPath());
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:

                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }


    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }


    void decodeImage(Bitmap selectedImage, String imageValue) {


        try {

          /*  Toast.makeText(WebPage.this,"decode image",Toast.LENGTH_SHORT).show();
            final Uri imageUri = data.getData();
            final InputStream imageStream = getContentResolver().openInputStream(imageUri);
         //   final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);*/
            String encodedImage = encodeImage(selectedImage, imageValue);

            //saving the image
            PrefFile.saveCapturedImage(MainActivity.this, FILE_NAME, encodedImage);


            PrefFile.saveAllFileInformation(MainActivity.this, FILE_NAME, FULL_FILE + "," + encodedImage);


        } catch (Exception ex) {


            ex.printStackTrace();
        }
    }

    /**
     * function will start the activity
     **/
    void startActivityAgain() {

        String urlSaved = PrefFile.getSavedUrl(MainActivity.this);


        try {
            if (urlSaved != null) {
                loadUrl(urlSaved);

            } else {

                loadUrl(URL);
            }
        } catch (Exception ex) {
            tvErrorReport.setText("Error:" + ex);
        }
    }


    private String encodeImage(Bitmap bm, String imageValue) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();


        imageValue = Base64.encodeToString(b, Base64.DEFAULT);

        String encImage = Base64.encodeToString(b, Base64.DEFAULT);

        return encImage;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        try {

            isGpsEnabled();

            initGps();


        } catch (Exception ex) {
            Toast.makeText(MainActivity.this, "Please enable gps", Toast.LENGTH_LONG).show();
        }

    }


    class MyJavascriptInterface {

        Context context;

        public MyJavascriptInterface(Context context) {
            this.context = context;
        }


        @android.webkit.JavascriptInterface
        public String getLatitude() {


            // Toast.makeText(WebPage.this, "Latitude", Toast.LENGTH_SHORT).show();


            isGpsEnabled();

            if (gps.canGetLocation()) {
                return "" + latitude;

            } else {


                return "" + latitude;
            }


        }

        @android.webkit.JavascriptInterface
        public String getCity() {

            //Toast.makeText(WebPage.this,"City",Toast.LENGTH_SHORT).show();
            try {

                if (addressInformation != null) {
                    return addressInformation.getCity();
                } else {
                    return null;
                }
            } catch (Exception ex) {
                return null;
            }


        }

        @android.webkit.JavascriptInterface
        public String getState() {

            //Toast.makeText(WebPage.this,"State",Toast.LENGTH_SHORT).show();

            try {

                if (addressInformation != null) {
                    return addressInformation.getState();
                } else {
                    return null;
                }
            } catch (Exception ex) {
                return null;
            }


        }

        @android.webkit.JavascriptInterface
        public String getPostalcode() {

            //Toast.makeText(WebPage.this,"Postal Code",Toast.LENGTH_SHORT).show();
            try {

                if (addressInformation != null) {
                    return addressInformation.getPostalCode();
                } else {
                    return null;
                }
            } catch (Exception ex) {
                return null;
            }

        }

        @android.webkit.JavascriptInterface
        public String getLocality() {

            // Toast.makeText(WebPage.this,"Locality",Toast.LENGTH_SHORT).show();

            try {

                if (addressInformation != null) {
                    return addressInformation.getLocality();
                } else {
                    return null;
                }
            } catch (Exception ex) {
                return null;
            }
        }


        @android.webkit.JavascriptInterface
        public String getCountry() {


            try {

                if (addressInformation != null) {
                    return addressInformation.getCountry();
                } else {
                    return null;
                }
            } catch (Exception ex) {
                return null;
            }

        }

        @android.webkit.JavascriptInterface
        public void deleteSavedData() {


            try {
                PrefFile.deleteSharedPreference(MainActivity.this);

            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }


        @android.webkit.JavascriptInterface
        public String getLongitude() {


            isGpsEnabled();

            //Toast.makeText(WebPage.this, "Longi", Toast.LENGTH_SHORT).show();
            if (gps.canGetLocation()) {
                return "" + longitude;

            } else {


                return "" + longitude;
            }
        }

        public List<Address> addressInformation() {

            Geocoder geocoder;
            List<Address> addresses = new ArrayList<>();
            geocoder = new Geocoder(MainActivity.this, Locale.getDefault());

            String city = "";

            try {


                if (geocoder.isPresent()) {


                    addresses = geocoder.getFromLocation(latitude, longitude, 1);

                } else {


                }


            } catch (Exception e) {
                e.printStackTrace();

            }

            return addresses;
        }

        @android.webkit.JavascriptInterface
        public String getQrCodeText(String url) {

            //Toast.makeText(WebPage.this,"Loading scanner page.." +"Url to save.."+url,Toast.LENGTH_SHORT).show();


            String qrCode = null;

            openQrCodeActivity(url);

            try {


                if (qrCode == null) {

                } else {

                }

                if (QRCODE_INFORMATION != "") {

                    return QRCODE_INFORMATION;//"{\"serialNumber\":\"801PMJQ001788\",\"firmwareVersion\":\"04.02.20\",\"macAddress\":\"7C:1C:4E:95:C8:01\",\"hardwareKey\":\"a8a0af7f-6b4f-38c4-8a2f-af18c046d5d6\",\"displayName\":\"webOS_ATPL\"}";
                } else {

                    return "";
                }

            } catch (Exception ex) {

                return qrCode;
            }

        }

        @android.webkit.JavascriptInterface
        public String getAddress() {
            if (gps.canGetLocation()) {
                return getAddressInformation();
            } else {
                return null;
            }
        }

        @android.webkit.JavascriptInterface
        public void captureImage(String imageName, String divName, String url, String typeName) {
            PrefFile.saveCaptureImageUrl(MainActivity.this, url);
            FILE_NAME = typeName;
            FULL_FILE = imageName + "," + divName + "," + url + "," + typeName;
            PrefFile.saveAllFileInformation(MainActivity.this, typeName, imageName + "," + divName + "," + url + "," + typeName);
            openCamera(PICTURE_ID);
            // return null;
        }

        @android.webkit.JavascriptInterface
        public String getSavedQrInfo() {
            //Toast.makeText(WebPage.this,"Getting qr code",Toast.LENGTH_SHORT).show();
            String qrCodeInfo = PrefFile.getSavedQrCode(MainActivity.this);
            if (qrCodeInfo != null) {
                return qrCodeInfo;

            } else {
                return null;
            }
        }

        @android.webkit.JavascriptInterface
        public void getFieldEmpty() {

            //Toast.makeText(WebPage.this, "Error ..Please fill all the details..", Toast.LENGTH_SHORT).show();
        }

        @android.webkit.JavascriptInterface
        public String
        getDisplayScreenShotBase64(String fileName) {


            String saveImageFile = PrefFile.getAllInformation(MainActivity.this, fileName);      //PrefFile.getSavedImagefile(WebPage.this,fileName);


            if (saveImageFile != null) {


                return saveImageFile;

            } else {

                return null;
            }
        }

        @android.webkit.JavascriptInterface
        public String getReportScreenShotBase64() {
            //Toast.makeText(WebPage.this, "Error ..Please fill all the details..", Toast.LENGTH_SHORT).show();

            return "Third";
        }

        @android.webkit.JavascriptInterface
        public String getDeviceID() {

            String uuid = getUUID(context).toString();

            return getUUID(context).toString();
        }


        @android.webkit.JavascriptInterface
        public void deleteCache() {

            //Toast.makeText(WebPage.this,"Deleting file",Toast.LENGTH_LONG).show();
            try {
                PrefFile.deleteSharedPreference(MainActivity.this);

            } catch (Exception ex) {

                Log.e("Webpage deleteCache", ex.toString());

                ex.printStackTrace();
            }


        }


    }

    public UUID getUUID(Context context) {
        try {
            String string = Settings.Secure.getString((ContentResolver) context.getContentResolver(), (String) "android_id");
            if (!"9774d56d682e549c".equals((Object) string) && !"19764045b48c416a".equals((Object) string)) {
                return UUID.nameUUIDFromBytes((byte[]) string.getBytes("utf8"));
            }
            try {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    @SuppressLint("WrongConstant") String string2 = ((TelephonyManager) context.getSystemService("phone")).getDeviceId();
                    if (string2 == null) return UUID.randomUUID();
                    return UUID.nameUUIDFromBytes((byte[]) string2.getBytes("utf8"));

                }
            } catch (Exception var3_5) {
                // empty catch block
            }
            return UUID.randomUUID();
        } catch (UnsupportedEncodingException var1_7) {
            throw new RuntimeException((Throwable) var1_7);
        }
    }


    public String getSecondaryUrl() {

        if (PrefFile.getSecondaryUrl(MainActivity.this) != null) {

            return PrefFile.getSecondaryUrl(MainActivity.this);

        } else {
            return DEFAULT_SECONDARY_URL;
        }


    }

    private class MyBrowser extends WebViewClient {
        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }


        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.d("MyApp", "onPageStarted ");
        }


        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            int ht = view.getContentHeight();
            Log.d("MyApp", "onPageFinished ");
        }


        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);


        }

        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

        }

    }

    public void locationListener() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };


        if (Build.VERSION.SDK_INT > 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, locationRequest);

            } else {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }


        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }


    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }


}
