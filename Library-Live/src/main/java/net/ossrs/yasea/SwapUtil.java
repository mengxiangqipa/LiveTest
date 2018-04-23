package net.ossrs.yasea;

import java.nio.ByteBuffer;

public class SwapUtil {

    private static volatile SwapUtil instance;

    public SwapUtil() {
    }

    public static SwapUtil getInstance() {
        if (null == instance) {
            synchronized (SwapUtil.class) {
                if (null == instance) {
                    instance = new SwapUtil();
                }
            }
        }
        return instance;
    }

    //NV21转nv12
    public void swapNV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) return;
        int framesize = width * height;
        int i = 0, j = 0;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j + 1] = nv21[j + framesize];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j] = nv21[j + framesize + 1];
        }
    }

    //YV12转I420
    public void swapYV12toI420(byte[] yv12bytes, byte[] i420bytes, int width, int height) {
        System.arraycopy(yv12bytes, 0, i420bytes, 0, width * height);
        System.arraycopy(yv12bytes, width * height + width * height / 4, i420bytes, width * height, width * height / 4);
        System.arraycopy(yv12bytes, width * height, i420bytes, width * height + width * height / 4, width * height / 4);
    }

    //yv12转nv12
    public void swapYV12toNV12(byte[] yv12bytes, byte[] nv12bytes, int width, int height) {
        int nLenY = width * height;
        int nLenU = nLenY / 4;
        System.arraycopy(yv12bytes, 0, nv12bytes, 0, width * height);
        for (int i = 0; i < nLenU; i++) {
            nv12bytes[nLenY + 2 * i + 1] = yv12bytes[nLenY + i];
            nv12bytes[nLenY + 2 * i] = yv12bytes[nLenY + nLenU + i];
        }
    }

    //nv12转I420
    public void swapNV12toI420(byte[] nv12bytes, byte[] i420bytes, int width, int height) {
        int nLenY = width * height;
        int nLenU = nLenY / 4;
        System.arraycopy(nv12bytes, 0, i420bytes, 0, width * height);
        for (int i = 0; i < nLenU; i++) {
            i420bytes[nLenY + i] = nv12bytes[nLenY + 2 * i + 1];
            i420bytes[nLenY + nLenU + i] = nv12bytes[nLenY + 2 * i];
        }
    }

    public static void swapNV21ToI420(byte[] nv21bytes, byte[] i420bytes, int width, int height) {
        int total = width * height;
        ByteBuffer bufferY = ByteBuffer.wrap(i420bytes, 0, total);
        ByteBuffer bufferU = ByteBuffer.wrap(i420bytes, total, total / 4);
        ByteBuffer bufferV = ByteBuffer.wrap(i420bytes, total + total / 4, total / 4);

        bufferY.put(nv21bytes, 0, total);
        for (int i = total; i < nv21bytes.length; i += 2) {
            bufferV.put(nv21bytes[i]);
            bufferU.put(nv21bytes[i + 1]);
        }
    }

    // YV12格式一个像素占1.5个字节
    public byte[] YV12_To_RGB24(byte[] yv12, int width, int height) {
        if (yv12 == null) {
            return null;
        }
        int nYLen = (int) width * height;
        int halfWidth = width >> 1;
        if (nYLen < 1 || halfWidth < 1) {
            return null;
        }
        // yv12's data structure
        // |WIDTH |
        // y......y--------
        // y......y   HEIGHT
        // y......y
        // y......y--------
        // v..v
        // v..v
        // u..u
        // u..u

        // Convert YV12 to RGB24
        byte[] rgb24 = new byte[width * height * 3];
        int[] rgb = new int[3];
        int i, j, m, n, x, y;
        m = -width;
        n = -halfWidth;
        for (y = 0; y < height; y++) {
            m += width;
            if (y % 2 != 0) {
                n += halfWidth;
            }
            for (x = 0; x < width; x++) {
                i = m + x;
                j = n + (x >> 1);
                rgb[2] = (int) ((int) (yv12[i] & 0xFF) + 1.370705 * ((int) (yv12[nYLen + j] & 0xFF) - 128)); // r
                rgb[1] = (int) ((int) (yv12[i] & 0xFF) - 0.698001 * ((int) (yv12[nYLen + (nYLen >> 2) + j] & 0xFF) -
                        128) - 0.703125 * ((int) (yv12[nYLen + j] & 0xFF) - 128));   // g
                rgb[0] = (int) ((int) (yv12[i] & 0xFF) + 1.732446 * ((int) (yv12[nYLen + (nYLen >> 2) + j] & 0xFF) -
                        128)); // b

                //j = nYLen - iWidth - m + x;
                //i = (j<<1) + j;    //图像是上下颠倒的

                j = m + x;
                i = (j << 1) + j;

                for (j = 0; j < 3; j++) {
                    if (rgb[j] >= 0 && rgb[j] <= 255) {
                        rgb24[i + j] = (byte) rgb[j];
                    } else {
                        rgb24[i + j] = (byte) ((rgb[j] < 0) ? 0 : 255);
                    }
                }
            }
        }
        return rgb24;
    }
}
