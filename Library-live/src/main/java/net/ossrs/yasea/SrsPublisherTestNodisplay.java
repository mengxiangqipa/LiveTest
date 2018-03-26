package net.ossrs.yasea;

import android.graphics.Rect;
import android.media.AudioRecord;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.util.Log;

import com.github.faucamp.simplertmp.RtmpHandler;
import com.seu.magicfilter.utils.MagicFilterType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * @author YobertJomi
 *         className SrsPublisherTest
 *         created at  2017/12/27  9:53
 */
public class SrsPublisherTestNodisplay {
    private static AudioRecord mic;
    private static AcousticEchoCanceler aec;
    private static AutomaticGainControl agc;
    byte[] logo_data;
    private byte[] mPcmBuffer = new byte[4096];
    private Thread aworker;
    private SrsSurfaceNoDisplay srsSurfaceNoDisplay;
    private boolean sendVideoOnly = false;
    private boolean sendAudioOnly = false;
    private int videoFrameCount;
    private long lastTimeMillis;
    private double mSamplingFps;
    private SrsFlvMuxer mFlvMuxer;
    private SrsMp4Muxer mMp4Muxer;
    private static SrsEncoder mEncoder;//TODO 本来非静态
    public static SrsEncoder getSrsEncoder(){//TODO 本来非静态
        return mEncoder;
    }

    public SrsPublisherTestNodisplay(SrsSurfaceNoDisplay view) {
        try {

            InputStream logo_input_stream = getClass().getResourceAsStream(
                    "/assets/logo.png");

            logo_data = ReadAssetFileDataToByte(logo_input_stream);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("yy", "write logo file to /sdcard/ failed");
        }

        srsSurfaceNoDisplay = view;
        if (null != srsSurfaceNoDisplay)
            srsSurfaceNoDisplay.setPreviewCallback(new SrsSurfaceNoDisplay.PreviewCallback() {
                @Override
                public void onGetRgbaFrame(byte[] data, int width, int height) {
                    String a = "我是初始数据";
                    try {
                        a = new String(data, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    Log.e("onGetRgbaFrame", "onGetRgbaFrame:" + "data:" +
                            (data.length) +
                            " width:" + width + " height:" +
                            height + "  videoFrameCount:" + videoFrameCount + "   mSamplingFps:" + mSamplingFps);
                    calcSamplingFps();
                    if (!sendAudioOnly) {
                        try {
//                            StringBuilder sb=new StringBuilder();
//                            for (int i = 0; i < data.length; i++) {
//                                sb.append(data[i]);
//                            }
//                            Log.e("onGetRgbaFrame","onGetRgbaFrame111:"+"lenth:"+data.length+"   width:"+width+"   height:"+height+"  ddd"+sb.toString());
                            mEncoder.onGetRgbaFrame(data, width, height);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
    }

    private byte[] ReadAssetFileDataToByte(InputStream in) throws IOException {
        ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
        int c = 0;

        while ((c = in.read()) != -1) {
            bytestream.write(c);
        }

        byte bytedata[] = bytestream.toByteArray();
        bytestream.close();
        return bytedata;
    }

    private void calcSamplingFps() {
        // Calculate sampling FPS
        if (videoFrameCount == 0) {
            lastTimeMillis = System.nanoTime() / 1000000;
            videoFrameCount++;
        } else {
            if (++videoFrameCount >= SrsEncoder.VGOP) {
                long diffTimeMillis = System.nanoTime() / 1000000 - lastTimeMillis;
                mSamplingFps = (double) videoFrameCount * 1000 / diffTimeMillis;
                videoFrameCount = 0;
            }
        }
    }

    public void startCamera() {
        if (null != srsSurfaceNoDisplay)
            srsSurfaceNoDisplay.startCamera();
    }

    public void stopCamera() {
        if (null != srsSurfaceNoDisplay)
            srsSurfaceNoDisplay.stopCamera();
    }

    public void startAudio() {
        mic = mEncoder.chooseAudioRecord();
        if (mic == null) {
            return;
        }

        if (AcousticEchoCanceler.isAvailable()) {
            aec = AcousticEchoCanceler.create(mic.getAudioSessionId());
            if (aec != null) {
                aec.setEnabled(true);
            }
        }

        if (AutomaticGainControl.isAvailable()) {
            agc = AutomaticGainControl.create(mic.getAudioSessionId());
            if (agc != null) {
                agc.setEnabled(true);
            }
        }

        aworker = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                mic.startRecording();
                while (!Thread.interrupted()) {
                    if (sendVideoOnly) {
                        mEncoder.onGetPcmFrame(mPcmBuffer, mPcmBuffer.length);
                        try {
                            // This is trivial...
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            break;
                        }
                    } else {
                        int size = mic.read(mPcmBuffer, 0, mPcmBuffer.length);
                        if (size > 0) {
                            mEncoder.onGetPcmFrame(mPcmBuffer, size);
                        }
                    }
                }
            }
        });
        aworker.start();
    }

    public void stopAudio() {
        if (aworker != null) {
            aworker.interrupt();
            try {
                aworker.join();
            } catch (InterruptedException e) {
                aworker.interrupt();
            }
            aworker = null;
        }

        if (mic != null) {
            mic.setRecordPositionUpdateListener(null);
            mic.stop();
            mic.release();
            mic = null;
        }

        if (aec != null) {
            aec.setEnabled(false);
            aec.release();
            aec = null;
        }

        if (agc != null) {
            agc.setEnabled(false);
            agc.release();
            agc = null;
        }
    }

    public void startEncode() {
        if (!mEncoder.start()) {
            return;
        }
        if (null != srsSurfaceNoDisplay)
            srsSurfaceNoDisplay.enableEncoding();

        startAudio();
    }

    public void stopEncode() {
        stopAudio();
        stopCamera();
        mEncoder.stop();
    }

    public void startPublish(String rtmpUrl) {
        if (mFlvMuxer != null) {
            mFlvMuxer.start(rtmpUrl);
            mFlvMuxer.setVideoResolution(mEncoder.getOutputWidth(), mEncoder.getOutputHeight());
            startEncode();
        }
    }

    public void stopPublish() {
        if (mFlvMuxer != null) {
            stopEncode();
            mFlvMuxer.stop();
        }
    }

    public boolean startRecord(String recPath) {
        return mMp4Muxer != null && mMp4Muxer.record(new File(recPath));
    }

    public void stopRecord() {
        if (mMp4Muxer != null) {
            mMp4Muxer.stop();
        }
    }

    public void pauseRecord() {
        if (mMp4Muxer != null) {
            mMp4Muxer.pause();
        }
    }

    public void resumeRecord() {
        if (mMp4Muxer != null) {
            mMp4Muxer.resume();
        }
    }

    public void switchToSoftEncoder() {
        mEncoder.switchToSoftEncoder();
    }

    public void switchToHardEncoder() {
        mEncoder.switchToHardEncoder();
    }

    public boolean isSoftEncoder() {
        return mEncoder.isSoftEncoder();
    }

    public int getPreviewWidth() {
        return mEncoder.getPreviewWidth();
    }

    public int getPreviewHeight() {
        return mEncoder.getPreviewHeight();
    }

    public double getmSamplingFps() {
        return mSamplingFps;
    }

    /**
     * @return
     */
    public int getCamraId() {
        return srsSurfaceNoDisplay == null ? -1 : srsSurfaceNoDisplay.getCameraId();
    }

    public void setPreviewResolution(int width, int height) {
        if (null != srsSurfaceNoDisplay) {
            int resolution[] = srsSurfaceNoDisplay.setPreviewResolution(width, height);
            mEncoder.setPreviewResolution(resolution[0], resolution[1]);
        }
    }

    public void setOutputResolution(int width, int height) {
        if (width <= height) {
            mEncoder.setPortraitResolution(width, height);
        } else {
            mEncoder.setLandscapeResolution(width, height);
        }
    }

    public void setScreenOrientation(int orientation) {
        if (null != srsSurfaceNoDisplay)
            srsSurfaceNoDisplay.setPreviewOrientation(orientation);
        mEncoder.setScreenOrientation(orientation);
    }

    public void setVideoHDMode() {
        mEncoder.setVideoHDMode();
    }

    public void setVideoSmoothMode() {
        mEncoder.setVideoSmoothMode();
    }

    public void setSendVideoOnly(boolean flag) {
        if (mic != null) {
            if (flag) {
                mic.stop();
                mPcmBuffer = new byte[4096];
            } else {
                mic.startRecording();
            }
        }
        sendVideoOnly = flag;
    }

    public void setSendAudioOnly(boolean flag) {
        sendAudioOnly = flag;
    }

    public boolean switchCameraFilter(MagicFilterType type) {
        if (null != srsSurfaceNoDisplay)
            return srsSurfaceNoDisplay.setFilter(type);
        else return false;
    }

    public void switchCameraFace(int id) {
        if (null != srsSurfaceNoDisplay) {
            srsSurfaceNoDisplay.stopCamera();
            srsSurfaceNoDisplay.setCameraId(id);
        }
        if (id == 0) {
            mEncoder.setCameraBackFace();
        } else {
            mEncoder.setCameraFrontFace();
        }
        if (mEncoder != null && mEncoder.isEnabled()) {
            if (null != srsSurfaceNoDisplay)
                srsSurfaceNoDisplay.enableEncoding();
        }
        if (null != srsSurfaceNoDisplay)
            srsSurfaceNoDisplay.startCamera();
    }

    public void setRtmpHandler(RtmpHandler handler) {
        mFlvMuxer = new SrsFlvMuxer(handler);
        if (mEncoder != null) {
            mEncoder.setFlvMuxer(mFlvMuxer);
        }
    }

    public void setRecordHandler(SrsRecordHandler handler) {
        mMp4Muxer = new SrsMp4Muxer(handler);
        if (mEncoder != null) {
            mEncoder.setMp4Muxer(mMp4Muxer);
        }
    }

    public void setEncodeHandler(SrsEncodeHandler handler) {
        mEncoder = new SrsEncoder(handler);
        if (mFlvMuxer != null) {
            mEncoder.setFlvMuxer(mFlvMuxer);
        }
        if (mMp4Muxer != null) {
            mEncoder.setMp4Muxer(mMp4Muxer);
        }
    }
}
