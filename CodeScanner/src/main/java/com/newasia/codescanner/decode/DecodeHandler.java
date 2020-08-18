/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.newasia.codescanner.decode;

import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;


import com.newasia.codescanner.CaptureActivity;
import com.newasia.codescanner.R;
import com.newasia.codescanner.camera.CameraManager;

import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;






final class DecodeHandler extends Handler {

    private static final String TAG = DecodeHandler.class.getSimpleName();

    private final CaptureActivity activity;
    private ImageScanner zBarDecoder;


    private boolean running = true;

    DecodeHandler(CaptureActivity activity) {
        zBarDecoder = new ImageScanner();
        this.activity = activity;
    }

    @Override
    public void handleMessage(Message message) {
        if (!running) {
            return;
        }
        if (message.what == R.id.decode) {
            decode((byte[]) message.obj, message.arg1, message.arg2);

        } else if (message.what == R.id.quit) {
            running = false;
            Looper.myLooper().quit();
        }
    }


    /**
     * 解码
     */

    private void decode(byte[] data, int width, int height)
    {
       // long start = System.currentTimeMillis();
        // 这里需要将获取的data翻转一下，因为相机默认拿的的横屏的数据
        int Model = 0;
        byte[] rotatedData;
        if(data.length >= width*height)
        {
            rotatedData = new byte[data.length];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++)
                    rotatedData[x * height + height - y - 1] = data[x + y * width];
            }
        }
        else
        {
            CameraManager manager = activity.getmCameraManager();
            Camera.Parameters parameters = manager.getCamera().getParameters();
            List<android.hardware.Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
            List<Camera.Size> supportedPreviewSizes = new ArrayList<Camera.Size>(rawSupportedSizes);
            for (int i=0;i<supportedPreviewSizes.size();++i)
            {
                Camera.Size size = supportedPreviewSizes.get(i);
                if (data.length == (int) (size.width*size.height*1.5))
                {
                    width = size.width;
                    height = size.height;
                    break;
                }
            }

            rotatedData = new byte[data.length];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++)
                        rotatedData[x * height + height - y - 1] = data[x + y * width];
                }

        }
        // 宽高也要调整

        Rect mCropRect = activity.initCrop();
        int result =0;
        String resultString = null;
        if (zBarDecoder != null) {
            try {
                int tmp = width;
                width = height;
                height = tmp;
                net.sourceforge.zbar.Image barcode = new Image(width, height, "Y800");
                barcode.setData(rotatedData);
                barcode.setCrop(mCropRect.left, mCropRect.top, mCropRect.width(),
                        mCropRect.height());
                //result = zBarDecoder.decodeCrop(rotatedData, width, height, mCropRect.left, mCropRect.top, mCropRect.width(), mCropRect.height());
                result = zBarDecoder.scanImage(barcode);
            }catch (Exception ex){
                ex.printStackTrace();
                zBarDecoder=null;
            }

            if (result != 0) {
                SymbolSet syms = zBarDecoder.getResults();
                for (Symbol sym : syms) {
                    resultString = sym.getData();
                }
            }





            Handler handler = activity.getHandler();
            if (resultString != null) {
               // long end = System.currentTimeMillis();
                if (handler != null) {
                    Message message = Message.obtain(handler,
                            R.id.decode_succeeded, resultString);
                    message.sendToTarget();
                }
            } else {
                if (handler != null) {
                    Message message = Message.obtain(handler, R.id.decode_failed);
                    message.sendToTarget();
                }
            }
        }
    }
}
