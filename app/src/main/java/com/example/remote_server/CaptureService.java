package com.example.remote_server; // 패키지 선언: 이 파일이 속한 패키지를 명시합니다.

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.PixelFormat;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.remote_myserver.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class CaptureService extends Service {
    private MediaProjection mediaProjection; // MediaProjection 객체: 화면 캡처를 위한 객체
    private VirtualDisplay virtualDisplay; // VirtualDisplay 객체: 가상 디스플레이를 위한 객체
    private ImageReader imageReader; // ImageReader 객체: 이미지를 읽기 위한 객체
    private Timer timer; // Timer 객체: 주기적으로 작업을 수행하기 위한 객체

    public static final int REQUEST_CODE = 100; // 요청 코드 상수

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        startForegroundServiceCompat(); // 포그라운드 서비스 시작

        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        int resultCode = intent != null ? intent.getIntExtra("resultCode", -1) : -1; // resultCode 가져오기
        Intent data = intent != null ? intent.getParcelableExtra("data") : null; // data 가져오기

        if (data != null) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data); // MediaProjection 설정
            setupVirtualDisplay(); // 가상 디스플레이 설정
            startCapture(); // 화면 캡처 시작
        } else {
            stopSelf(); // data가 null이면 서비스 중지
        }

        return START_STICKY; // 서비스가 강제 종료되었을 때 재시작
    }

    /**
     * 포그라운드 서비스를 시작하는 메서드 (안드로이드 26 이상)
     */
    @RequiresApi(26)
    private void startForegroundServiceCompat() {
        String channelId = "CaptureServiceChannel"; // 알림 채널 ID
        String channelName = "Screen Capture Service"; // 알림 채널 이름

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel); // 알림 채널 생성

        Notification notification = new Notification.Builder(this, channelId)
                .setContentTitle("Screen Capture")
                .setContentText("Screen capture in progress")
                .setSmallIcon(R.drawable.ic_notification)
                .build(); // 알림 생성

        startForeground(1, notification); // 포그라운드 서비스 시작
    }

    /**
     * 가상 디스플레이를 설정하는 메서드
     */
    private void setupVirtualDisplay() {
        DisplayMetrics metrics = getResources().getDisplayMetrics(); // 디스플레이 메트릭스 가져오기
        imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2); // ImageReader 생성

        if (mediaProjection != null) {
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "ScreenCapture",
                    metrics.widthPixels,
                    metrics.heightPixels,
                    metrics.densityDpi,
                    0, // 플래그
                    imageReader.getSurface(),
                    null,
                    null
            ); // 가상 디스플레이 생성
        }
    }

    /**
     * 화면 캡처를 시작하는 메서드
     */
    private void startCapture() {
        timer = new Timer(); // Timer 객체 생성
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                captureScreen(); // 주기적으로 화면 캡처 실행
            }
        }, 0, 5000); // 5초 간격으로 실행
    }

    /**
     * 화면을 캡처하는 메서드
     */
    private void captureScreen() {
        Bitmap bitmap = getBitmapFromImageReader(); // ImageReader에서 Bitmap 가져오기
        if (bitmap != null) {
            uploadBitmapToServer(bitmap); // Bitmap을 서버에 업로드
        } else {
            Log.d("CaptureService", "화면 캡처 실패");
        }
    }

    /**
     * ImageReader에서 Bitmap을 가져오는 메서드
     */
    private Bitmap getBitmapFromImageReader() {
        if (imageReader == null) {
            return null; // imageReader가 null이면 null 반환
        }

        try (Image image = imageReader.acquireLatestImage()) { // 최신 이미지를 가져오기
            if (image != null) {
                return imageToBitmap(image); // Image를 Bitmap으로 변환
            }
        } catch (Exception e) {
            Log.e("CaptureService", "이미지 획득 중 오류 발생", e);
        }

        return null; // 이미지 획득 실패 시 null 반환
    }

    /**
     * Image 객체를 Bitmap으로 변환하는 메서드
     */
    private Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes(); // Image의 Plane 배열 가져오기
        ByteBuffer buffer = planes[0].getBuffer(); // Plane의 버퍼 가져오기
        int pixelStride = planes[0].getPixelStride(); // PixelStride 가져오기
        int rowStride = planes[0].getRowStride(); // RowStride 가져오기
        int rowPadding = rowStride - pixelStride * image.getWidth(); // RowPadding 계산

        Bitmap bitmap = Bitmap.createBitmap(
                image.getWidth() + rowPadding / pixelStride,
                image.getHeight(),
                Config.ARGB_8888
        ); // Bitmap 생성
        bitmap.copyPixelsFromBuffer(buffer); // 버퍼에서 Bitmap으로 픽셀 복사

        return Bitmap.createBitmap(bitmap, 0, 0, image.getWidth(), image.getHeight()); // 최종 Bitmap 반환
    }

    /**
     * 캡처한 Bitmap을 서버에 업로드하는 메서드
     */
    private void uploadBitmapToServer(Bitmap bitmap) {
        OkHttpClient client = new OkHttpClient(); // OkHttpClient 객체 생성
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()); // 날짜 포맷 설정
        String fileName = dateFormat.format(new Date()) + ".png"; // 파일 이름 설정

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); // ByteArrayOutputStream 객체 생성
        bitmap.compress(CompressFormat.PNG, 100, byteArrayOutputStream); // Bitmap을 PNG 형식으로 압축
        byte[] byteArray = byteArrayOutputStream.toByteArray(); // 압축된 바이트 배열 가져오기

        RequestBody requestBody = RequestBody.create(byteArray, MediaType.parse("image/png")); // RequestBody 객체 생성
        Request request = new Request.Builder()
                .url("http://192.168.0.108:8080/upload.jsp")
                .post(requestBody)
                .addHeader("filename", fileName)
                .build(); // Request 객체 생성

        client.newCall(request).enqueue(new Callback() { // 비동기 요청 실행
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("CaptureService", "업로드 실패", e); // 업로드 실패 시 로그 출력
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    Log.d("CaptureService", "업로드 성공: " + response.code()); // 업로드 성공 시 로그 출력
                } else {
                    Log.e("CaptureService", "업로드 실패: " + response.message()); // 업로드 실패 시 로그 출력
                }
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(@Nullable Intent intent) {
        return null; // 바인딩을 지원하지 않음
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (timer != null) {
            timer.cancel(); // Timer 취소
        }

        if (mediaProjection != null) {
            mediaProjection.stop(); // MediaProjection 중지
        }
    }
}
