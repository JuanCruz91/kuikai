// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package org.pytorch.demo.objectdetection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Runnable {

    //Attributes
    private int mImageIndex = 0;
    private String[] mTestImages = {"epi1.jpg", "epi2.jpg", "epi3.jpg", "epi4.jpg", "nacho.jpg", "test1.jpeg", "test2.jpg", "test3.jpeg", "test4.jpeg", "test5.jpeg", "test6.jpg", "test7.jpeg", "test8.jpg"};
    public static String assetName = "best_EPISV3_nano.torchscript";
    static public String fileName = "epis_detection_class.txt", url_website = "http://visionanalytics.ai", url_linkedIn = "https://www.linkedin.com/company/visionanalytics-ai/";
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;

    private ZoomableImageView mImageView;
    private Toolbar toolbar;

    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        super.setSupportActionBar(toolbar);
    }

    private ResultView mResultView;
    private Button mButtomNavView;
    private BottomNavigationItemView mButtonDetect;

    private ProgressBar mProgressBar;
    private Bitmap mBitmap = null;
    static private Module mModule = null;

    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String FileName = "JPEG_" + timeStamp + ".png";
    final String FolderName = "/Kuikai";

    private float mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY;

    private String[] imgFormat = {"bmp", "dng", "jpeg", "jpg", "mpo", "png", "tif", "tiff"};
    private boolean formatOK = false;


    //Metodo de clase que devuelve la ruta del fichero activo
    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }


        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    private void sendEmail() {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/html");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"info@visionanalytics.ai"});
        emailIntent.putExtra(android.content.Intent.EXTRA_TITLE, "Title");
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Subject");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.my_text));
        try {
            //Enviamos el Correo iniciando una nueva Activity con el emailIntent.
            startActivity(Intent.createChooser(emailIntent, "Enviar Correo..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this, "No hay ningun cliente de correo instalado.", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * Comprueba y realiza solicitud de los permisos de la camará y la lectura de ficheros de memoria externa
         */
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        setContentView(R.layout.activity_main);


        /**
         *
        genera un Bitmap decodificando el stream resultante de extraer de la carpeta Assets
        del archivo con posicion por defecto en 0, del String[] mTestImages
        */
        try {
            mBitmap = BitmapFactory.decodeStream(getAssets().open(mTestImages[mImageIndex]));
        } catch (
                IOException e) {
            Log.e("Object Detection", "Error reading assets", e);
            finish();
        }

        mImageView = findViewById(R.id.imageView);
        mImageView.setImageBitmap(mBitmap);
        mResultView = findViewById(R.id.resultView);
        mResultView.setVisibility(View.INVISIBLE);
        mButtomNavView = findViewById(R.id.button_navView);

        mButtomNavView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.setVisibility(View.VISIBLE);
            }
        });

        //OnClick of ButtonTest
        final BottomNavigationItemView buttonTest = findViewById(R.id.testButton);
        buttonTest.setTitle(("Test 1/" + mTestImages.length));
        buttonTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mResultView.setVisibility(View.INVISIBLE);
                mImageIndex = (mImageIndex + 1) % mTestImages.length;
                buttonTest.setTitle(String.format("Test %d/%d", mImageIndex + 1, mTestImages.length));

                try {
                    mBitmap = BitmapFactory.decodeStream(getAssets().open(mTestImages[mImageIndex]));
                    mImageView.setImageBitmap(mBitmap);
                } catch (IOException e) {
                    Log.e("Object Detection", "Error reading assets", e);
                    finish();
                }
            }
        });

        /**NavigationView*/
        navigationView = (NavigationView) findViewById(R.id.navView);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

                Intent intent = new Intent(MainActivity.this, ObjectDetectionActivity.class);
                Intent intent_website;
                Uri uri = Uri.parse(url_website), uri_linkedin = Uri.parse(url_linkedIn);

                if (item.isChecked()) {
                    item.setChecked(true);
                } else {
                    item.setChecked(false);
                }
                drawerLayout.closeDrawers();

                switch (item.getItemId()) {

                    case R.id.profile:
                        intent_website = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent_website);
                        drawerLayout.closeDrawers();
                        drawerLayout.setVisibility(View.INVISIBLE);
                        return false;
                    case R.id.epis_detection:
                        assetName = "best_EPISV3_nano.torchscript";
                        fileName = "epis_detection_class.txt";
                        intent.putExtra("AssetName", assetName);
                        changeAI(assetName, fileName);
                        Log.i("TAG", "ASSET_NAME: " + assetName);
                        drawerLayout.setVisibility(View.INVISIBLE);
                        return false;
                    case R.id.objects_detection:
                        assetName = "yolov5s.torchscript.ptl";
                        fileName = "object_detection_class.txt";
                        intent.putExtra("AssetName", assetName);
                        changeAI(assetName, fileName);
                        Log.i("TAG", "ASSET_NAME: " + assetName);
                        drawerLayout.setVisibility(View.INVISIBLE);
                        return false;
                    case R.id.age_detection:
                        drawerLayout.closeDrawers();
                        drawerLayout.setVisibility(View.INVISIBLE);
                        return false;
                    case R.id.gender_detection:
                        drawerLayout.closeDrawers();
                        drawerLayout.setVisibility(View.INVISIBLE);
                        return false;
                    case R.id.website:
                        intent_website = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent_website);
                        drawerLayout.setVisibility(View.INVISIBLE);
                        return false;
                    case R.id.linkedIn:
                        intent_website = new Intent(Intent.ACTION_VIEW, uri_linkedin);
                        startActivity(intent_website);
                        drawerLayout.closeDrawers();
                        drawerLayout.setVisibility(View.INVISIBLE);
                        return false;
                    case R.id.emailadress:
                        sendEmail();
                        drawerLayout.closeDrawers();
                        drawerLayout.setVisibility(View.INVISIBLE);
                        return false;
                    case R.id.help:
                        intent_website = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent_website);
                        drawerLayout.closeDrawers();
                        drawerLayout.setVisibility(View.INVISIBLE);
                        return false;
                }
                return true;
            }
        });


        /**ActionBarDrawerToggle*/
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.abrirDrawer, R.string.cerrarDrawer) {

            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };

        /**Métodos que serán llamados cuando se abra y se cierre el drawer*/
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        /**Se invoca al método syncSTate(), que sincronizará el estado del drawer con el DrawerLayout*/
        actionBarDrawerToggle.syncState();


        /**OnClick del ButtonSelect*/
        final BottomNavigationItemView buttonSelect = findViewById(R.id.selectButton);
        buttonSelect.setTitle(getString(R.string.select));
        buttonSelect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mResultView.setVisibility(View.INVISIBLE);

                //create menu in order to choose take photo or enter in galery
                final CharSequence[] options = {"Choose from Photos", "Take Picture", "Cancel"};
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("New Test Image");

                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        if (options[item].equals("Take Picture")) {
//                            dispatchTakePictureIntent();
                            Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(takePicture, 0);
                        } else if (options[item].equals("Choose from Photos")) {
                            Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                            startActivityForResult(pickPhoto, 1);
                        } else if (options[item].equals("Cancel")) {
                            dialog.dismiss();
                        }
                    }
                });
                builder.show();
            }
        });


        /**onCLick del boton Live*/
        final BottomNavigationItemView buttonLive = findViewById(R.id.liveButton);
        buttonLive.setTitle(getString(R.string.live));
        buttonLive.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final Intent intent = new Intent(MainActivity.this, ObjectDetectionActivity.class);
                startActivity(intent);
            }
        });

        mButtonDetect = findViewById(R.id.detectButton);
        mButtonDetect.setTitle(getString(R.string.detect));
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mButtonDetect.setOnClickListener(new View.OnClickListener() {

            /**onClick del botón Detect*/
            public void onClick(View v) {
                mProgressBar.setVisibility(ProgressBar.VISIBLE);
                mProgressBar.getIndeterminateDrawable().setColorFilter(getColor(R.color.white), PorterDuff.Mode.SRC_IN);

                mImgScaleX = (float) mBitmap.getWidth() / PrePostProcessor.mInputWidth;
                mImgScaleY = (float) mBitmap.getHeight() / PrePostProcessor.mInputHeight;

                mIvScaleX = (mBitmap.getWidth() > mBitmap.getHeight() ? (float) mImageView.getWidth() / mBitmap.getWidth() : (float) mImageView.getHeight() / mBitmap.getHeight());
                mIvScaleY = (mBitmap.getHeight() > mBitmap.getWidth() ? (float) mImageView.getHeight() / mBitmap.getHeight() : (float) mImageView.getWidth() / mBitmap.getWidth());

                mStartX = (mImageView.getWidth() - mIvScaleX * mBitmap.getWidth()) / 2;
                mStartY = (mImageView.getHeight() - mIvScaleY * mBitmap.getHeight()) / 2;

                Thread thread = new Thread(MainActivity.this);
                thread.start();
            }
        });
        changeAI(assetName, fileName);
    }

    /**
     * This two methods just are complements to make sure if the device has and sdcard and if it is inserted
     */
    public boolean hasRealRemovableSdCard(Context context) {
        return ContextCompat.getExternalFilesDirs(context, null).length >= 2;
    }

    public String getRemovableSDCardPath(Context context) {
        File[] storages = ContextCompat.getExternalFilesDirs(context, null);
        if (storages.length > 1 && storages[0] != null && storages[1] != null)
            return storages[1].toString();
        else
            return "";
    }

    protected void DialogSaveImage(Bitmap mBitmap) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Save Image");

        String[] options = {getString(R.string.SDCard), getString(R.string.internal), getString(R.string.cancel)};
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (options[item].equals(getString(R.string.SDCard))) {
                    String SDroute = route_SD();
                    saveImage(SDroute, mBitmap);
                } else if (options[item].equals(getString(R.string.internal))) {
                    String route = "sdcard/DCIM" + FolderName;
                    saveImage(route, mBitmap);
                } else if (options[item].equals(getString(R.string.cancel))) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    /**
     * This method provides SD Route, which will be use to provide sdcard route
     */
    public String route_SD() {
        String route_sd = "";
        if (hasRealRemovableSdCard(getApplicationContext())) {
            String file_path = getRemovableSDCardPath(getApplicationContext());
            String[] file_sdcard = file_path.split("/");
            route_sd = file_sdcard[1].toString() + "/" + file_sdcard[2].toString() + "/DCIM" + FolderName;

        }
        return route_sd;
    }

    /**
     * This method has the function of create the directory and the file which the user wants to save
     */
    public void saveImage(String route, Bitmap ImageToSave) {
        File dir = new File(route);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(route, FileName);

        try {
            FileOutputStream fOut = new FileOutputStream(file);

            ImageToSave.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
            MakeSureFileWasCreatedThenMakeAvailable(file);
            Toast.makeText(this, "Imagen guardada en la galería.", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void MakeSureFileWasCreatedThenMakeAvailable(File file) {
        MediaScannerConnection.scanFile(this,
                new String[]{file.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                    }
                });
    }

    protected void changeAI(String assetName, String fileName) {
        try {
            /**
             * Carga la red neuronal que se pasa por parámetro según que opción de menú hayas pulsado
             * Lee el archivo que contiene los nombres que usa la red neuronal
             */
            mModule = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), assetName));
            BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open(fileName)));

            String line;
            List<String> classes = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                classes.add(line);
            }
            PrePostProcessor.mClasses = new String[classes.size()];
            classes.toArray(PrePostProcessor.mClasses);
        } catch (IOException e) {

            finish();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {

                /**Take Picture
                 * Take the result intent which content the image and transform to Bitmat
                 * [NICE TO HAVE] Here is where we could replaces bitmap with a file.
                 * */
                case 0:
                    if (resultCode == RESULT_OK && data != null) {

                        mBitmap = (Bitmap) data.getExtras().get("data");
                        mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), null, false); //Foto con mala calidad
                        DialogSaveImage(mBitmap);
                        mImageView.setImageBitmap(mBitmap);
                    }
                    break;

                /**Choose from gallery
                 * Here is where we control the image format.
                 * */
                case 1:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        if (selectedImage != null) {
                            Cursor cursor = getContentResolver().query(selectedImage,
                                    filePathColumn, null, null, null);
                            if (cursor != null) {
                                cursor.moveToFirst();
                                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                                String picturePath = cursor.getString(columnIndex);
                              
                                for (String format : imgFormat){       //FORMAT CONTROL
                                    if (picturePath.toLowerCase().contains(format)) {
                                        formatOK = true;
                                    }
                                }

                                if (formatOK) {
                                    mBitmap = BitmapFactory.decodeFile(picturePath);
                                    mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), null, true);
                                    mImageView.setImageBitmap(mBitmap);
                                }else{
                                    Toast.makeText(this, "Picture format is not allowed", Toast.LENGTH_SHORT).show();
                                }
                                cursor.close();
                            }
                        }
                    }
                    break;
            }
        }
    }



    /**
     * this method execute when the backgroundThread method is called.
     * create a bitMap to include the IA answer and show the progressBar while the IA respond.
     */
    @Override
    public void run() {

        //Tensores que devuelve la IA
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(mBitmap, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple(); /**Henry: Parece ser evaluado por la red*/
        final Tensor outputTensor = outputTuple[0].toTensor();/**Henry: Output de la red*/
        final float[] outputs = outputTensor.getDataAsFloatArray();/**Henry: Transforma a array (por alguna razón)*/
        final ArrayList<Result> results = PrePostProcessor.outputsToNMSPredictions(outputs, mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY);/**Henry: Postprocesamiento*/

        runOnUiThread(() -> {
//            mButtonDetect.setEnabled(true);
//            mButtonDetect.setText(getString(R.string.detect));
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            mResultView.setResults(results);
            mResultView.invalidate();
            mResultView.setVisibility(View.VISIBLE);
        });
    }
}
