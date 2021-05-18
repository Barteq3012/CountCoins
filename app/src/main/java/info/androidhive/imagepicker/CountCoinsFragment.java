package info.androidhive.imagepicker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import java.io.IOException;
import java.util.List;
import info.androidhive.imagepicker.ui.main.PageViewModel;


public class CountCoinsFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "section_number";
    private PageViewModel pageViewModel;

    private static final String TAG = DetectText.class.getSimpleName();
    public static final int REQUEST_IMAGE = 100;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    Button countCoinsButton;
    Button captureImageButton;
    Bitmap imageBitmap;
    Bitmap imageBitmap2;
    TextView textView;
    ImageView selected_image;
    Mat mat;
    Mat grayMat;
    //all items connected with sliders
    TextView label1, label2, label3, label4;
    SeekBar seekbar1, seekbar2, seekbar3, seekbar4;
    int minRadius = 0;
    int maxRadius = 200;
    double param1 = 70; //gradient
    double param2 = 100; //threshold


    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.count_coins, container, false);
        countCoinsButton = root.findViewById(R.id.countCoinsButtonId);
        captureImageButton = root.findViewById(R.id.captureImageButtonId);
        textView = root.findViewById(R.id.text_display);
        selected_image = root.findViewById(R.id.img_view);

        //sliders initialization
        label1 = root.findViewById(R.id.textView1);
        label2 = root.findViewById(R.id.textView2);
        label3 = root.findViewById(R.id.textView3);
        label4 = root.findViewById(R.id.textView4);
        seekbar1 = root.findViewById(R.id.seekBar1);
        seekbar2 = root.findViewById(R.id.seekBar2);
        seekbar3 = root.findViewById(R.id.seekBar3);
        seekbar4 = root.findViewById(R.id.seekBar4);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        OpenCVLoader.initDebug();

        countCoinsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                countCoinsFromImage();
            }
        });

        captureImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCaptureImageButtonClick();
                textView.setText("Number of coins: ");
            }
        });

        seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                label1.setText("Gradient: " + i);
                param1 = 20.0 + (double) i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }


        });

        seekbar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                label2.setText("Max size: " + i);
                maxRadius = 150 + i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }


        });

        seekbar3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                label3.setText("Threshold: " + i);
                param2 = 50.0 + (double) i;
                Log.d("myTag", "value of threshold is set: " + param2 );
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }


        });

        seekbar4.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                label4.setText("Min size: " + i);
                minRadius = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }


        });

        textView.setMovementMethod(new ScrollingMovementMethod());

        loadDefaultImage();

        // Clearing older images from cache directory
        // don't call this line if you want to choose multiple images in the same activity
        ImagePickerActivity.clearCache(getActivity());
    }

    private void loadImage(String url) {
        Log.d(TAG, "Image cache path: " + url);

        GlideApp.with(this).load(url).into(selected_image);

        selected_image.setColorFilter(ContextCompat.getColor(getContext(), android.R.color.transparent));
    }

    private void loadDefaultImage() {
        GlideApp.with(this).load(R.drawable.outline_image_black_48)
                .into(selected_image);
        selected_image.setColorFilter(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.black));
    }

    void onCaptureImageButtonClick() {
       /* Dexter.withActivity(getActivity())
                .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            showImagePickerOptions();
                            //textView.setText("");
                        }

                        if (report.isAnyPermissionPermanentlyDenied()) {
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();*/
        showImagePickerOptions();
        /*Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.isPermissionGranted(Manifest.permission.CAMERA)) {
                    if (resultSet.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        showImagePickerOptions();
                    } else {
                        showSettingsDialog();
                    }
                }else {
                    showSettingsDialog();
                }

            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                Permiso.getInstance().showRationaleInDialog("Title", "Message", null, callback);
            }
        }, Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALENDAR);*/
    }

    private void showImagePickerOptions() {
        ImagePickerActivity.showImagePickerOptions(getActivity(), new ImagePickerActivity.PickerOptionListener() {
            @Override
            public void onTakeCameraSelected() {
                launchCameraIntent();
            }

            @Override
            public void onChooseGallerySelected() {
                launchGalleryIntent();
            }
        });
    }

    private void launchCameraIntent() {
        Intent intent = new Intent(getActivity(), ImagePickerActivity.class);
        intent.putExtra(ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION, ImagePickerActivity.REQUEST_IMAGE_CAPTURE);

        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true);
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1); // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1);

        // setting maximum bitmap width and height
        intent.putExtra(ImagePickerActivity.INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT, true);
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_WIDTH, 1000);
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_HEIGHT, 1000);

        startActivityForResult(intent, REQUEST_IMAGE);
    }

    private void launchGalleryIntent() {
        Intent intent = new Intent(getActivity(), ImagePickerActivity.class);
        intent.putExtra(ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION, ImagePickerActivity.REQUEST_GALLERY_IMAGE);

        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true);
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1); // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1);
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getParcelableExtra("path");
            try {
                // can update this bitmap to server
                imageBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);

                imageBitmap2 = imageBitmap.copy(imageBitmap.getConfig(), true);

                // loading image from local cache

                loadImage(uri.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void countCoinsFromImage() {
        if(imageBitmap == null){
            Toast.makeText(getActivity(), "Select a picture first!", Toast.LENGTH_SHORT).show();
            return;
        }

        imageBitmap = imageBitmap2.copy(imageBitmap2.getConfig(), true);

        // convert bitmap to mat
        mat = new Mat(imageBitmap.getWidth(), imageBitmap.getHeight(),
                CvType.CV_8UC1);
        grayMat = new Mat(imageBitmap.getWidth(), imageBitmap.getHeight(),
                CvType.CV_8UC1);

        Utils.bitmapToMat(imageBitmap, mat);

        // convert to grayscale
        int colorChannels = (mat.channels() == 3) ? Imgproc.COLOR_BGR2GRAY
                : ((mat.channels() == 4) ? Imgproc.COLOR_BGRA2GRAY : 1);

        Imgproc.cvtColor(mat, grayMat, colorChannels);

        // reduce the noise
        Imgproc.GaussianBlur(grayMat, grayMat, new Size(9, 9), 2, 2);

        // accumulator value
        double dp = 1.2d;
        // minimum distance between the center coordinates of detected circles in pixels
        double minDist = 100;

        // min and max radius
        //minRadius = 0;
        //maxRadius = 200;

        // param1 = gradient value used to handle edge detection
        // param2 = Accumulator threshold value for the
        // cv2.CV_HOUGH_GRADIENT method.

       // param1 = 70;
       // param2 = 100;

        // create a Mat object to store the circles detected
        Mat circles = new Mat(imageBitmap.getWidth(),
                imageBitmap.getHeight(), CvType.CV_8UC1);

        // find the circle in the image
        Imgproc.HoughCircles(grayMat, circles,
                Imgproc.CV_HOUGH_GRADIENT, dp, minDist, param1,
                param2, minRadius, maxRadius);

        // get the number of circles detected
        int numberOfCircles = (circles.rows() == 0) ? 0 : circles.cols();

        textView.setText("Number of coins: " + Integer.toString(numberOfCircles));

        if( numberOfCircles != 0 && numberOfCircles < 100){
            // draw the circles found on the image
            for (int i=0; i<numberOfCircles; i++) {

                /* get the circle details, circleCoordinates[0, 1, 2] = (x,y,r)
                 * (x,y) are the coordinates of the circle's center
                 */
                double[] circleCoordinates = circles.get(0, i);


                if( circleCoordinates.length > 1 ){
                    int x = (int) circleCoordinates[0], y = (int) circleCoordinates[1];

                    Point center = new Point(x, y);

                    int radius = (int) circleCoordinates[2];

                    /* circle's outline */
                    Imgproc.circle(mat, center, radius, new Scalar(0,
                            255, 0), 4);

                    /* circle's center outline */
                    Imgproc.rectangle(mat, new Point(x - 5, y - 5),
                            new Point(x + 5, y + 5),
                            new Scalar(0, 128, 255), -1);
                } else {
                    Log.d("myTag", "for some reason vector does not contain 2 or 3 elements, here length  " + circleCoordinates.length );
                }
            }

            /* convert back to bitmap */
            Utils.matToBitmap(mat, imageBitmap);
            selected_image.setImageBitmap(imageBitmap);
        }

    }

    // permissions
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.dialog_permission_title));
        builder.setMessage(getString(R.string.dialog_permission_message));
        builder.setPositiveButton(getString(R.string.go_to_settings), (dialog, which) -> {
            dialog.cancel();
            openSettings();
        });
        builder.setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> dialog.cancel());
        builder.show();

    }

    // navigating user to app settings
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }

}
