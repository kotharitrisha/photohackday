package your.blind.food;


import java.io.IOException;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;

public class AppforPhotoHackdayActivity extends Activity {
	
	Camera camera;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
     
        camera = Camera.open(Camera.getNumberOfCameras());
        final CameraSurfaceView cameraSurfaceView = new CameraSurfaceView(this);
        FrameLayout preview = (FrameLayout) findViewById(R.id.preview); 
        preview.addView(cameraSurfaceView);
                
        Button takeAPicture = (Button)findViewById(R.id.click);
        takeAPicture.setOnClickListener(new OnClickListener() 
        {
                public void onClick(View v) 
                {
                        Camera camera = cameraSurfaceView.getCamera();
                        camera.takePicture(null, null, new HandlePictureStorage());
                }
        });
        
        try {
			camera.setPreviewDisplay((SurfaceHolder) cameraSurfaceView);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        camera.startPreview();
    }
    
    public void onPause(Bundle savedInstanceState){
    	camera.release();
    }
    
    public void onResume(Bundle savedInstance){
    	camera = Camera.open(Camera.getNumberOfCameras());
    }
}