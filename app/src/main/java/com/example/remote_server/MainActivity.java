package com.example.remote_server; // 패키지 선언: 이 파일이 속한 패키지를 명시합니다.

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.annotation.Nullable;

import com.example.remote_myserver.R;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public final class MainActivity extends Activity {
    @Nullable
    private MediaProjectionManager projectionManager; // 화면 캡처를 위한 MediaProjectionManager 객체
    private TextView statusTextView; // 상태 표시 TextView
    private SeekBar volumeSeekBar; // 볼륨 조절 SeekBar
    private TextView volumeTextView; // 볼륨 값 표시 TextView
    private AudioManager audioManager; // 오디오 관리를 위한 AudioManager 객체
    private VideoView videoView; // 비디오 재생을 위한 VideoView
    private final Handler handler = new Handler(Looper.getMainLooper()); // UI 스레드에서 작업을 수행하기 위한 핸들러
    private final Runnable hideControlsRunnable = this::hideControls; // 볼륨 컨트롤을 숨기기 위한 Runnable 객체

    private static final int REQUEST_CODE = 100; // 화면 캡처 요청 코드를 위한 상수

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 레이아웃 설정

        // 시스템 서비스 및 뷰 초기화
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        statusTextView = findViewById(R.id.statusTextView);
        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        volumeTextView = findViewById(R.id.volumeTextView);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        videoView = findViewById(R.id.videoView);

        // 초기에는 볼륨 컨트롤 숨기기
        volumeSeekBar.setVisibility(View.GONE);
        volumeTextView.setVisibility(View.GONE);

        // 비디오 설정 및 준비
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + '/' + R.raw.sample_video);
        videoView.setVideoURI(videoUri);
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true); // 비디오 반복 재생 설정
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) videoView.getLayoutParams();
            params.width = RelativeLayout.LayoutParams.MATCH_PARENT;
            params.height = RelativeLayout.LayoutParams.MATCH_PARENT;
            videoView.setLayoutParams(params); // 비디오 뷰 크기 설정
            resetServer(); // 서버 리셋 호출
        });

        // 볼륨 시크바 설정
        volumeSeekBar.setMax(100); // 볼륨 최대값 100으로 설정
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC); // 오디오 매니저로부터 최대 볼륨 값 가져오기
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC); // 현재 볼륨 값 가져오기
        int volumePercentage = (int) ((currentVolume / (float) maxVolume) * 100); // 현재 볼륨 값을 퍼센트로 계산
        volumeSeekBar.setProgress(volumePercentage); // 시크바의 현재 볼륨 값 설정
        volumeTextView.setText("Volume: " + volumePercentage); // 텍스트뷰에 현재 볼륨 값 표시

        // 볼륨 시크바 변경 리스너 설정
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newVolume = (int) (progress / 100.0 * maxVolume); // 새로운 볼륨 값 계산
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0); // 새로운 볼륨 값 설정
                volumeTextView.setText("Volume: " + progress); // 텍스트뷰에 새로운 볼륨 값 표시
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    /**
     * 서버를 리셋하고 비디오 재생을 시작하는 메서드
     * 서버 리셋 요청을 보내고 성공 시 비디오를 재생하며 화면 캡처를 시작합니다.
     */
    private void resetServer() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://192.168.0.108:8080/reset.jsp")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("MainActivity", "서버 리셋 실패", e); // 요청 실패 시 로그 출력
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("MainActivity", "서버 리셋 성공"); // 요청 성공 시 로그 출력
                    runOnUiThread(() -> {
                        videoView.start(); // 비디오 재생 시작
                        startScreenCapture(); // 화면 캡처 시작
                    });
                } else {
                    Log.e("MainActivity", "서버 리셋 실패: " + response.message()); // 요청 실패 시 로그 출력
                }
            }
        });
    }

    /**
     * 화면 캡처를 시작하는 메서드
     * 화면 캡처 인텐트를 생성하고 결과를 받기 위해 startActivityForResult를 호출합니다.
     */
    private void startScreenCapture() {
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Intent captureIntent = new Intent(this, CaptureService.class);
            captureIntent.putExtra("resultCode", resultCode);
            captureIntent.putExtra("data", data);
            startForegroundService(captureIntent); // CaptureService를 포그라운드 서비스로 시작
            statusTextView.setText("Status: Capturing"); // 상태 텍스트뷰 업데이트
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, @Nullable KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
                togglePower(); // 전원 토글
                return true;
            case KeyEvent.KEYCODE_2:
                showControls(); // 컨트롤 표시
                adjustVolume(true); // 볼륨 증가
                return true;
            case KeyEvent.KEYCODE_3:
                showControls(); // 컨트롤 표시
                adjustVolume(false); // 볼륨 감소
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    /**
     * 볼륨 및 재생 컨트롤을 화면에 표시하는 메서드
     * 3초 후에 컨트롤을 숨기도록 설정합니다.
     */
    private void showControls() {
        volumeSeekBar.setVisibility(View.VISIBLE);
        volumeTextView.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideControlsRunnable);
        handler.postDelayed(hideControlsRunnable, 3000); // 3초 후에 컨트롤 숨기기
    }

    /**
     * 볼륨 및 재생 컨트롤을 화면에서 숨기는 메서드
     */
    private void hideControls() {
        volumeSeekBar.setVisibility(View.GONE);
        volumeTextView.setVisibility(View.GONE);
    }

    /**
     * 볼륨을 조절하는 메서드
     * @param increase 볼륨을 증가시킬지 여부를 나타냅니다.
     */
    private void adjustVolume(boolean increase) {
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int increment = 1;

        int newVolume = increase ? Math.min(currentVolume + increment, maxVolume) : Math.max(currentVolume - increment, 0);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, AudioManager.FLAG_SHOW_UI);
        int volumePercentage = (int) ((newVolume / (float) maxVolume) * 100);
        volumeSeekBar.setProgress(volumePercentage);
        volumeTextView.setText("Volume: " + volumePercentage);
    }

    /**
     * 비디오 재생을 토글하는 메서드
     * 재생 중인 비디오를 일시 정지하거나 다시 시작합니다.
     */
    private void togglePower() {
        if (videoView.isPlaying()) {
            videoView.pause();
        } else {
            videoView.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 앱이 종료될 때 CaptureService를 중지
        Intent stopIntent = new Intent(this, CaptureService.class);
        stopService(stopIntent);
    }
}
