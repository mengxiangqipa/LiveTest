package test.com.livetest;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.faucamp.simplertmp.RtmpHandler;
import com.seu.magicfilter.utils.MagicFilterType;
import com.seu.magicfilter.utils.OpenGLUtils;

import net.ossrs.yasea.SrsEncodeHandler;
import net.ossrs.yasea.SrsEncoder;
import net.ossrs.yasea.SrsPublisherTestNodisplay;
import net.ossrs.yasea.SrsRecordHandler;
import net.ossrs.yasea.SrsSurfaceNoDisplay;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Sikang on 2017/5/2.
 */
public class CameraTestNoDisplayActivity extends Activity implements SrsEncodeHandler.SrsEncodeListener, RtmpHandler
        .RtmpListener,
        SrsRecordHandler.SrsRecordListener, View.OnClickListener {
    private static final String TAG = "CameraActivity";
    private Button mPublishBtn;
    private Button mCameraSwitchBtn;
    private Button mEncoderBtn;
    private EditText mRempUrlEt;
    private SrsPublisherTestNodisplay mPublisher;
    //    private SrsPublisherTest mPublisher;
    private String rtmpUrl;
    private Camera mCamera;
    private int mPreviewWidth = 1920;
    private int mPreviewHeight = 1080;
    private boolean mIsTorchOn = false;
    private int mPreviewRotation = 90;
    private SurfaceTexture surfaceTexture;
    private byte[] callbackBuffer;
    private int mOESTextureId = OpenGLUtils.NO_TEXTURE;
    private int mCamId = -1;

    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera_no_display);

        mPublishBtn = (Button) findViewById(R.id.publish);
        mCameraSwitchBtn = (Button) findViewById(R.id.swCam);
        mEncoderBtn = (Button) findViewById(R.id.swEnc);
        mRempUrlEt = (EditText) findViewById(R.id.url);
        mPublishBtn.setOnClickListener(this);
        mCameraSwitchBtn.setOnClickListener(this);
        mEncoderBtn.setOnClickListener(this);

        mPublisher = new SrsPublisherTestNodisplay(new SrsSurfaceNoDisplay(CameraTestNoDisplayActivity.this));
//        SrsCameraView srsCameraView = new SrsCameraView(this);
//        mPublisher = new SrsPublisherTest(srsCameraView);

        //编码状态回调
        mPublisher.setEncodeHandler(new SrsEncodeHandler(this));
        mPublisher.setRecordHandler(new SrsRecordHandler(this));
        //rtmp推流状态回调
        mPublisher.setRtmpHandler(new RtmpHandler(this));

//        //预览分辨率
//        mPublisher.setPreviewResolution(800, 480);
//        //推流分辨率
//        mPublisher.setOutputResolution(480, 800);

//        //预览分辨率
//        mPublisher.setPreviewResolution(1280, 720);
//        //推流分辨率
//        mPublisher.setOutputResolution(720, 1280);

        //预览分辨率
        mPublisher.setPreviewResolution(1920, 1080);
        //推流分辨率
        mPublisher.setOutputResolution(1080, 1920);

        //传输率
        mPublisher.setVideoHDMode();
//        mPublisher.setVideoSmoothMode();
        //开启美颜（其他滤镜效果在MagicFilterType中查看）
        mPublisher.switchCameraFilter(MagicFilterType.NONE);
        //打开摄像头，开始预览（未推流）
        mPublisher.startCamera();
        mPublisher.switchCameraFace((mPublisher.getCamraId() + 1) % Camera.getNumberOfCameras());

//        init(this);//todo test
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //开始/停止推流
            case R.id.publish:
                if (mPublishBtn.getText().toString().contentEquals("开始")) {
                    rtmpUrl = mRempUrlEt.getText().toString();
                    if (TextUtils.isEmpty(rtmpUrl)) {
                        Toast.makeText(getApplicationContext(), "地址不能为空！", Toast.LENGTH_SHORT).show();
                    }
                    mPublisher.startPublish(rtmpUrl);
                    mPublisher.startCamera();

                    if (mEncoderBtn.getText().toString().contentEquals("软编码")) {
                        Toast.makeText(getApplicationContext(), "当前使用硬编码", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "当前使用软编码", Toast.LENGTH_SHORT).show();
                    }
                    mPublishBtn.setText("停止");
                    mEncoderBtn.setEnabled(false);
                } else if (mPublishBtn.getText().toString().contentEquals("停止")) {
                    mPublisher.stopPublish();
                    mPublisher.stopRecord();
                    mPublishBtn.setText("开始");
                    mEncoderBtn.setEnabled(true);
                }
                break;
            //切换摄像头
            case R.id.swCam:
                mPublisher.switchCameraFace((mPublisher.getCamraId() + 1) % Camera.getNumberOfCameras());
                break;
            //切换编码方式
            case R.id.swEnc:
                if (mEncoderBtn.getText().toString().contentEquals("软编码")) {
                    mPublisher.switchToSoftEncoder();
                    mEncoderBtn.setText("硬编码");
                } else if (mEncoderBtn.getText().toString().contentEquals("硬编码")) {
                    mPublisher.switchToHardEncoder();
                    mEncoderBtn.setText("软编码");
                }
                break;
            case R.id.startCamera:
                Log.e("yy", "我的");
                startCamera();
                break;
            case R.id.showImageview:
                ImageView imageView = (ImageView) findViewById(R.id.iv);
                SrsSurfaceNoDisplay.showBitmap(imageView);
                break;
        }
    }

    public boolean startCamera() {
        if (mCamera == null) {
            mCamera = openCamera();
            if (mCamera == null) {
                return false;
            }
        }
        Log.e("yy", "startCamera1");
        Camera.Parameters params = mCamera.getParameters();
        params.setPictureSize(mPreviewWidth, mPreviewHeight);
        params.setPreviewSize(mPreviewWidth, mPreviewHeight);
        int[] range = adaptFpsRange(SrsEncoder.VFPS, params.getSupportedPreviewFpsRange());
        params.setPreviewFpsRange(range[0], range[1]);
        params.setPreviewFormat(ImageFormat.NV21);
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        Log.e("yy", "startCamera2");
        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null && !supportedFocusModes.isEmpty()) {
            if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mCamera.autoFocus(null);
            } else {
                params.setFocusMode(supportedFocusModes.get(0));
            }
        }
        Log.e("yy", "startCamera3");
        List<String> supportedFlashModes = params.getSupportedFlashModes();
        if (supportedFlashModes != null && !supportedFlashModes.isEmpty()) {
            if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                if (mIsTorchOn) {
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                }
            } else {
                params.setFlashMode(supportedFlashModes.get(0));
            }
        }
        Log.e("yy", "startCamera4");
        mCamera.setParameters(params);

        mCamera.setDisplayOrientation(mPreviewRotation);
        Log.e("yy", "startCamera5");
        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.e("yy", "startCamera6");
        callbackBuffer = new byte[1920 * 1080];
        mCamera.addCallbackBuffer(callbackBuffer);
//        mCamera.setPreviewCallback(new Camera.PreviewCallback()
//        {
//            @Override
//            public void onPreviewFrame(byte[] data, Camera camera)
//            {
//                Log.e("yy", "setPreviewCallback：" + (data == null));
//            }
//        });
        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
//                Log.e("yy", "onPreviewFrame：" + (data == null));
                mCamera.addCallbackBuffer(callbackBuffer);
            }
        });
        Log.e("yy", "startCamera7");
        mCamera.startPreview();
        Log.e("yy", "startCamera8");
        return true;
    }

    public void init(@NonNull Context context) {
        GLES20.glDisable(GL10.GL_DITHER);
        GLES20.glClearColor(0, 0, 0, 0);

        mOESTextureId = OpenGLUtils.getExternalOESTextureID();
        surfaceTexture = new SurfaceTexture(mOESTextureId);
        Log.e("yy", "init:" + mOESTextureId);
        // For camera preview on activity creation
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(surfaceTexture);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private Camera openCamera() {
        Camera camera;
        if (mCamId < 0) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            int numCameras = Camera.getNumberOfCameras();
            int frontCamId = -1;
            int backCamId = -1;
            for (int i = 0; i < numCameras; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    backCamId = i;
                } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    frontCamId = i;
                    break;
                }
            }
            if (frontCamId != -1) {
                mCamId = frontCamId;
            } else if (backCamId != -1) {
                mCamId = backCamId;
            } else {
                mCamId = 0;
            }
        }
        camera = Camera.open(mCamId);
        return camera;
    }

    private int[] adaptFpsRange(int expectedFps, List<int[]> fpsRanges) {
        expectedFps *= 1000;
        int[] closestRange = fpsRanges.get(0);
        int measure = Math.abs(closestRange[0] - expectedFps) + Math.abs(closestRange[1] - expectedFps);
        for (int[] range : fpsRanges) {
            if (range[0] <= expectedFps && range[1] >= expectedFps) {
                int curMeasure = Math.abs(range[0] - expectedFps) + Math.abs(range[1] - expectedFps);
                if (curMeasure < measure) {
                    closestRange = range;
                    measure = curMeasure;
                }
            }
        }
        return closestRange;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else {
            switch (id) {
                case R.id.cool_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.COOL);
                    break;
                case R.id.beauty_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.BEAUTY);
                    break;
                case R.id.early_bird_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.EARLYBIRD);
                    break;
                case R.id.evergreen_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.EVERGREEN);
                    break;
                case R.id.n1977_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.N1977);
                    break;
                case R.id.nostalgia_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.NOSTALGIA);
                    break;
                case R.id.romance_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.ROMANCE);
                    break;
                case R.id.sunrise_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.SUNRISE);
                    break;
                case R.id.sunset_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.SUNSET);
                    break;
                case R.id.tender_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.TENDER);
                    break;
                case R.id.toast_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.TOASTER2);
                    break;
                case R.id.valencia_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.VALENCIA);
                    break;
                case R.id.walden_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.WALDEN);
                    break;
                case R.id.warm_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.WARM);
                    break;
                case R.id.original_filter:
                default:
                    mPublisher.switchCameraFilter(MagicFilterType.NONE);
                    break;
            }
        }
        setTitle(item.getTitle());
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPublisher.resumeRecord();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPublisher.pauseRecord();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPublisher.stopPublish();
        mPublisher.stopRecord();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mPublisher.stopEncode();
        mPublisher.stopRecord();
        mPublisher.setScreenOrientation(newConfig.orientation);
        if (mPublishBtn.getText().toString().contentEquals("停止")) {
            mPublisher.startEncode();
        }
        mPublisher.startCamera();
    }

    @Override
    public void onNetworkWeak() {
        Toast.makeText(getApplicationContext(), "网络型号弱", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNetworkResume() {

    }

    @Override
    public void onEncodeIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    private void handleException(Exception e) {
        try {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            mPublisher.stopPublish();
            mPublisher.stopRecord();
            mPublishBtn.setText("开始");
        } catch (Exception e1) {
            //
        }
    }

    @Override
    public void onRtmpConnecting(String msg) {
        Toast.makeText(getApplicationContext(), "onRtmpConnecting:" + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpConnected(String msg) {
        Toast.makeText(getApplicationContext(), "onRtmpConnected" + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpVideoStreaming() {

    }

    @Override
    public void onRtmpAudioStreaming() {

    }

    @Override
    public void onRtmpStopped() {
        Toast.makeText(getApplicationContext(), "已停止", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpDisconnected() {
        Toast.makeText(getApplicationContext(), "未连接服务器", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpVideoFpsChanged(double fps) {

    }

    @Override
    public void onRtmpVideoBitrateChanged(double bitrate) {

    }

    @Override
    public void onRtmpAudioBitrateChanged(double bitrate) {

    }

    @Override
    public void onRtmpSocketException(SocketException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalStateException(IllegalStateException e) {
        handleException(e);
    }

    @Override
    public void onRecordPause() {
        Toast.makeText(getApplicationContext(), "Record paused", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordResume() {
        Toast.makeText(getApplicationContext(), "Record resumed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordStarted(String msg) {
        Toast.makeText(getApplicationContext(), "Recording file: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordFinished(String msg) {
        Toast.makeText(getApplicationContext(), "MP4 file saved: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRecordIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }
}
