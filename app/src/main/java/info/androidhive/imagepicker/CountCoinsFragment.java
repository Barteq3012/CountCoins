package info.androidhive.imagepicker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import java.io.IOException;

public class CountCoinsFragment extends Fragment {
    public static final int REQUEST_IMAGE = 100;
    Button countCoinsButton;
    Button captureImageButton;
    Switch gamma_switch;
    Switch contrast_switch;
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
    int maxRadius = 250;
    double param1 = 80; //gradient
    double param2 = 80; //threshold

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.count_coins, container, false);
        countCoinsButton = root.findViewById(R.id.countCoinsButtonId);
        captureImageButton = root.findViewById(R.id.captureImageButtonId);
        textView = root.findViewById(R.id.text_display);
        selected_image = root.findViewById(R.id.img_view);
        gamma_switch = root.findViewById(R.id.gamma_switch);
        contrast_switch = root.findViewById(R.id.contrast_switch);
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

    @SuppressLint("SetTextI18n")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        OpenCVLoader.initDebug();

        countCoinsButton.setOnClickListener(view -> countCoinsFromImage());

        captureImageButton.setOnClickListener(view -> {
            onCaptureImageButtonClick();
            textView.setText("Number of coins: ");
        });

        seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                i = i/5;i=i*5;
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
                i = i/5;i=i*5;
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
                i = i/5;i=i*5;
                label3.setText("Threshold: " + i);
                param2 = 50.0 + (double) i;
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
                i = i/5;i=i*5;
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
        ImagePickerActivity.clearCache(getActivity());
    }

    private void loadImage(String url) {
        Log.d("myTag", "Image cache path: " + url);

        GlideApp.with(this).load(url).into(selected_image);

        selected_image.setColorFilter(ContextCompat.getColor(getContext(), android.R.color.transparent));
    }

    private void loadDefaultImage() {
        GlideApp.with(this).load(R.drawable.outline_image_black_48)
                .into(selected_image);
        selected_image.setColorFilter(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.black));
    }

    void onCaptureImageButtonClick() {
        showImagePickerOptions();
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
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1);
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
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1);
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1);
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getParcelableExtra("path");
            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);
                imageBitmap2 = imageBitmap.copy(imageBitmap.getConfig(), true);
                loadImage(uri.toString()); // loading image from local cache
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void countCoinsFromImage() {
        double sum = 0.0;
        //double s = 15.5;
        double s = 24.0;
        //double[] dim_ratio = {1.0,1.0,16.5/s,17.5/s,18.5/s,19.5/s,20.5/s,21.5/s,24.0/s,24.0/s};
        double[] dim_ratio = {1.0,1.0,23.0/s,21.5/s,20.5/s,19.5/s,18.5/s,17.5/s,16.5/s,15.5/s};
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

        // convert to grayscale RGB or ARGB
        int colorChannels = (mat.channels() == 3) ? Imgproc.COLOR_BGR2GRAY
                : ((mat.channels() == 4) ? Imgproc.COLOR_BGRA2GRAY : 1);
        Imgproc.cvtColor(mat, grayMat, colorChannels);

        if(gamma_switch.isChecked()){
            double gamma_val = 0.6;
            Mat lookUpTable = new Mat(1, 256, CvType.CV_8U);
            byte[] lookUpTableData = new byte[(int) (lookUpTable.total()*lookUpTable.channels())];
            for (int i = 0; i < lookUpTable.cols(); i++) {
                int value = (int) Math.round(Math.pow(i / 255.0, gamma_val) * 255.0);
                lookUpTableData[i] = value > 255 ? (byte) 255 : (byte) (Math.max(value, 0));
            }
            lookUpTable.put(0, 0, lookUpTableData);
            Core.LUT(grayMat, lookUpTable, grayMat);
        }

        if(contrast_switch.isChecked()) {
            Mat cmat = new Mat(grayMat.size(),grayMat.type());
            Core.MinMaxLocResult minmaxV = Core.minMaxLoc(grayMat);
            Core.subtract(grayMat, new Scalar(minmaxV.minVal), grayMat);
            grayMat.convertTo(cmat, CvType.CV_8U,1.5,10.0);
            grayMat = cmat;
        }

        // reduce the noise by GaussianBlur
        Imgproc.GaussianBlur(grayMat, grayMat, new Size(13, 13), 5, 5);

        // accumulator value
        double acc = 1.2;
        // minimum distance between the center coordinates of detected circles in pixels
        double minDist = 100;

        // param1 = gradient value used to handle edge detection
        // param2 = Accumulator threshold value for the
        // cv2.CV_HOUGH_GRADIENT method.

        // create a Mat object to store the circles detected
        Mat circles = new Mat(imageBitmap.getWidth(),
                imageBitmap.getHeight(), CvType.CV_8UC1);

        // find the circle in the image
        Imgproc.HoughCircles(grayMat, circles,
                Imgproc.CV_HOUGH_GRADIENT, acc, minDist, param1,
                param2, minRadius, maxRadius);

        // get the number of circles detected
        int numberOfCircles = (circles.rows() == 0) ? 0 : circles.cols();

        textView.setText("Number of coins: " + Integer.toString(numberOfCircles));

        if( numberOfCircles != 0 && numberOfCircles < 100){
            double r;
            double highest_r = 0.0;

            for (int i=0; i<numberOfCircles; i++) {
                double[] circleCoordinates = circles.get(0, i);
                r = circleCoordinates[2];
                if(r > highest_r){
                    highest_r = r;
                }
            }


            // draw the circles
            for (int i=0; i<numberOfCircles; i++) {

                //circleCoordinates[0, 1, 2] = (x,y,r)
                double[] circleCoordinates = circles.get(0, i);

                if( circleCoordinates.length > 1 ){
                    int x = (int) circleCoordinates[0];
                    int y = (int) circleCoordinates[1];
                    Point center = new Point(x, y);
                    double radius = circleCoordinates[2];

                    Imgproc.circle(mat, center, (int) radius, new Scalar(0,
                            255, 0), 4);

                    Scalar color = new Scalar(0, 0, 0);
                    int font = Core.FONT_HERSHEY_SIMPLEX;

                    String[] coins = new String[]{"5", "1", "2", "50", "5", "20", "2", "10", "1"};
                    for(int j = 1; j < 9; j++){
                        double l = (dim_ratio[j-1] +  dim_ratio[j])/2.0;
                        double h = (dim_ratio[j] +  dim_ratio[j+1])/2.0;

                        if( radius > highest_r * h && radius <= highest_r * l){
                            Imgproc.putText(mat, coins[j-1], new Point(x - 10, y - 10), font, 3, color , 3);
                            Log.d("myTag", "coins radius: " + radius + " kind: " + coins[j-1] );
                        } else if( j == 8 && radius <= highest_r * h) {
                            Imgproc.putText(mat, coins[j], new Point(x - 10, y - 10), font, 3, color, 3);
                            Log.d("myTag", "coins radius: " + radius + " kind: " + coins[j] );
                        }

                    }

                } else {
                    Log.d("myTag", "for some reason vector does not contain 2 or 3 elements, here length  " + circleCoordinates.length );
                }
            }

            // convert back to bitmap
            Utils.matToBitmap(mat, imageBitmap);
            selected_image.setImageBitmap(imageBitmap);
        }
    }
}
