package info.androidhive.imagepicker;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;
import java.io.IOException;
import java.util.List;

public class DetectTextFragment extends Fragment {
    public static final int REQUEST_IMAGE = 100;
    Button detectTextButton;
    Button captureImageButton;
    Bitmap imageBitmap;
    TextView textView;
    ImageView selected_image;
    FloatingActionButton copyButton;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.detect_text, container, false);
        detectTextButton = root.findViewById(R.id.detectTextButtonId);
        captureImageButton = root.findViewById(R.id.captureImageButtonId);
        textView = root.findViewById(R.id.text_display);
        selected_image = root.findViewById(R.id.img_view);
        copyButton = root.findViewById(R.id.copyTextButton);
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        detectTextButton.setOnClickListener(view -> detectTextFromImage());

        captureImageButton.setOnClickListener(view -> onCaptureImageButtonClick());

        copyButton.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("whatever", textView.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getActivity(), "Text has been copied", Toast.LENGTH_SHORT).show();
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
                // loading profile image from local cache

                loadImage(uri.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void detectTextFromImage() {
        if(imageBitmap == null){
            Toast.makeText(getActivity(), "Select a picture first!", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionTextDetector firebaseVisionTextDetector = FirebaseVision.getInstance().getVisionTextDetector();
        firebaseVisionTextDetector.detectInImage(firebaseVisionImage).addOnSuccessListener(
                this::displayTextFromImage).addOnFailureListener(e -> {
            Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d("Error: " , e.getMessage());
        });
    }

    private void displayTextFromImage(FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.Block> blockList = firebaseVisionText.getBlocks();
        copyButton.hide();
        if (blockList.size() == 0){
            Toast.makeText(getActivity(), "No Text Found in image", Toast.LENGTH_SHORT).show();
            textView.setText("");
        } else {
            copyButton.show();
            String text = "";
            StringBuilder s = new StringBuilder(text);
            for (FirebaseVisionText.Block block : firebaseVisionText.getBlocks()){
                text = block.getText();
                s.append(text);
                s.append("\n");
            }
            text = s.toString();
            textView.setText(text);
        }
    }
}