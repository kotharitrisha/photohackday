package your.blind.food;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class AppforPhotoHackdayActivity extends Activity {

	/* CameraActivity Variables */
	private Camera mCamera;
	private Preview mPreview;
	private Overlay mOverlay;
	private Button buttonProc, buttonBinImage, buttonReset;
	private TextView textAns;

	private String localSearchCapableStr = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		localSearchCapableStr = savedInstanceState != null ? savedInstanceState
				.getString("localSearchCapable") : null;
				
				// Create an instance of Camera
		        mCamera = getCameraInstance();
		        Camera.Parameters mParameters = mCamera.getParameters();
		        mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
		        mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
		        mCamera.setParameters(mParameters);
		        
		        // Create our Preview view and set it as the content of our activity.
		        mPreview = new Preview(this, mCamera);
		        FrameLayout preview = (FrameLayout) findViewById(R.id.preview);
		        preview.addView(mPreview);
		        
		        // Create our Overlay
		        mOverlay = new Overlay(this);
		        preview.addView(mOverlay);
		        mPreview.setOverlay(mOverlay);
	}

	public void onPause(Bundle savedInstanceState) {
		mCamera.release();
	}

	public void onResume(Bundle savedInstance) {
		mCamera = Camera.open(Camera.getNumberOfCameras());
	}
	
	/**
	 * Helper Methods
	 */
	public static Camera getCameraInstance() {
    	Camera c = null;
    	try {
    		c = Camera.open();
    	}
    	catch (Exception e) {
    		
    	}
    	return c;
    }
}