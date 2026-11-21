package com.sactre.collar;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Abre el juego a pantalla completa y le pasa el mando de consola directamente:
 * la palanca izquierda apunta (360 grados) y los botones se mandan como teclas.
 * Así no aparece el cursor que pone el navegador de la tele.
 */
public class MainActivity extends Activity {

    private static final String ONLINE = "https://sactre.github.io/juegos/bolas/";
    private static final String OFFLINE = "file:///android_asset/bolas/index.html";

    private WebView web;
    private float lastX = 0f, lastY = 0f;
    private int hatX = 0, hatY = 0;

    /** El juego comprueba window.AndroidApp para saber que va dentro de la app. */
    public static class Bridge {
        @JavascriptInterface
        public String version() { return "1"; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        web.addJavascriptInterface(new Bridge(), "AndroidApp");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                // sin internet: usa la copia del juego que va dentro de la app
                if (req.isForMainFrame()) view.loadUrl(OFFLINE);
            }
        });
        web.setFocusable(false);
        web.setFocusableInTouchMode(false);
        setContentView(web);
        hideSystemUi();
        web.loadUrl(ONLINE);
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void js(String code) {
        web.evaluateJavascript("window.__pad&&" + code, null);
    }

    private void sendKey(int code, boolean down) {
        js("__pad.key(" + code + "," + down + ")");
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code;
        switch (e.getKeyCode()) {
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                code = 13; break;
            case KeyEvent.KEYCODE_BUTTON_START:      // botón Menú (≡): pausa
                code = 80; break;
            case KeyEvent.KEYCODE_BUTTON_SELECT:     // botón Vista (⧉): salir, con confirmación
                if (e.getAction() == KeyEvent.ACTION_UP) askExit();
                return true;
            case KeyEvent.KEYCODE_BACK:
                // la B del mando a veces llega como "Atrás": no debe sacar del juego
                if (fromGamepad(e)) { code = 38; break; }
                if (e.getAction() == KeyEvent.ACTION_UP) askExit();
                return true;
            case KeyEvent.KEYCODE_BUTTON_B:          // B: cambiar de bola (antes salía de la app)
            case KeyEvent.KEYCODE_BUTTON_X:
            case KeyEvent.KEYCODE_BUTTON_Y:
            case KeyEvent.KEYCODE_BUTTON_L1:
            case KeyEvent.KEYCODE_BUTTON_R1:
            case KeyEvent.KEYCODE_DPAD_UP:
                code = 38; break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                code = 40; break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                code = 37; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                code = 39; break;
            default:
                return super.dispatchKeyEvent(e);   // Atrás (B), volumen, inicio...
        }
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            if (e.getRepeatCount() == 0 || code == 37 || code == 39) sendKey(code, true);
        } else if (e.getAction() == KeyEvent.ACTION_UP) {
            sendKey(code, false);
        }
        return true;
    }

    private static boolean fromGamepad(KeyEvent e) {
        int src = e.getSource();
        return (src & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                || (src & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK;
    }

    private AlertDialog exitDialog;

    private void askExit() {
        if (exitDialog != null && exitDialog.isShowing()) return;
        js("__pad.pause(true)");   // pausa mientras se pregunta
        exitDialog = new AlertDialog.Builder(this)
                .setTitle("¿Salir del juego?")
                .setPositiveButton("Salir", (d, w) -> finish())
                .setNegativeButton("Seguir jugando", (d, w) -> js("__pad.pause(false)"))
                .setOnCancelListener(d -> js("__pad.pause(false)"))
                .create();
        exitDialog.show();
        exitDialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent e) {
        boolean joystick = (e.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                || (e.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD;
        if (joystick && e.getAction() == MotionEvent.ACTION_MOVE) {
            float x = e.getAxisValue(MotionEvent.AXIS_X);
            float y = e.getAxisValue(MotionEvent.AXIS_Y);
            if (Math.abs(x - lastX) > 0.02f || Math.abs(y - lastY) > 0.02f) {
                lastX = x; lastY = y;
                js("__pad.axes(" + x + "," + y + ")");
            }
            // la cruceta del mando de Xbox llega como "hat": la convertimos en flechas
            int hx = Math.round(e.getAxisValue(MotionEvent.AXIS_HAT_X));
            int hy = Math.round(e.getAxisValue(MotionEvent.AXIS_HAT_Y));
            if (hx != hatX) {
                if (hatX != 0) sendKey(hatX < 0 ? 37 : 39, false);
                if (hx != 0) sendKey(hx < 0 ? 37 : 39, true);
                hatX = hx;
            }
            if (hy != hatY) {
                if (hy != 0) { sendKey(hy < 0 ? 38 : 40, true); sendKey(hy < 0 ? 38 : 40, false); }
                hatY = hy;
            }
            return true;
        }
        return super.dispatchGenericMotionEvent(e);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    @Override
    protected void onPause() { super.onPause(); web.onPause(); }

    @Override
    protected void onResume() { super.onResume(); web.onResume(); }
}
