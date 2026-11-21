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

import java.util.ArrayList;

/**
 * Abre los juegos a pantalla completa y les pasa los mandos de consola directamente
 * (sin el cursor que pone el navegador de la tele). Distingue hasta 2 mandos:
 * cada uno es un jugador (window.__pads). Los juegos de un solo jugador usan window.__pad.
 */
public class MainActivity extends Activity {

    private static final String HOME = "https://sactre.github.io/juegos/";
    private static final String OFFLINE = "file:///android_asset/site/";

    private WebView web;
    private final ArrayList<Integer> pads = new ArrayList<>();
    private final float[] lastX = new float[2], lastY = new float[2];
    private final int[] hatX = new int[2], hatY = new int[2];

    /** El juego comprueba window.AndroidApp para saber que va dentro de la app. */
    public static class Bridge {
        @JavascriptInterface
        public String version() { return "2"; }
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
                // sin internet: usa la copia de los juegos que va dentro de la app
                if (req.isForMainFrame()) {
                    String u = req.getUrl().toString();
                    view.loadUrl(u.startsWith(HOME) ? offline(u.substring(HOME.length())) : OFFLINE + "index.html");
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String u = req.getUrl().toString();
                // dentro de la app, "bolas/" tiene que ser "bolas/index.html"
                if (u.startsWith(OFFLINE)) {
                    String fixed = offline(u.substring(OFFLINE.length()));
                    if (!fixed.equals(u)) { view.loadUrl(fixed); return true; }
                }
                return false;
            }
        });
        web.setFocusable(false);
        web.setFocusableInTouchMode(false);
        setContentView(web);
        hideSystemUi();
        web.loadUrl(HOME);
    }

    private static String offline(String rest) {
        String query = "";
        int q = rest.indexOf('?');
        if (q >= 0) { query = rest.substring(q); rest = rest.substring(0, q); }
        if (rest.isEmpty() || rest.endsWith("/")) rest = rest + "index.html";
        return OFFLINE + rest + query;
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void js(String code) {
        web.evaluateJavascript(code, null);
    }

    /** Jugador 1 o 2 según el orden en que se usan los mandos. El mando de la tele es el jugador 1. */
    private int slotOf(int deviceId, boolean gamepad) {
        if (!gamepad) return 0;
        int i = pads.indexOf(deviceId);
        if (i < 0) { pads.add(deviceId); i = pads.size() - 1; }
        return Math.min(i, 1);
    }

    /** btn = nombre para los juegos de 2 jugadores; code = tecla para los de 1 jugador. */
    private void sendKey(int slot, String btn, int code, boolean down) {
        js("if(window.__pads)__pads.key(" + slot + ",'" + btn + "'," + down + ");"
                + "else if(window.__pad)__pad.key(" + code + "," + down + ");");
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code;
        String btn;
        switch (e.getKeyCode()) {
            case KeyEvent.KEYCODE_BUTTON_A:
                code = 13; btn = "a"; break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                code = 13; btn = "a"; break;
            case KeyEvent.KEYCODE_BUTTON_START:      // botón Menú (≡): pausa
                code = 80; btn = "start"; break;
            case KeyEvent.KEYCODE_BUTTON_SELECT:     // botón Vista (⧉): salir, con confirmación
                if (e.getAction() == KeyEvent.ACTION_UP) askExit();
                return true;
            case KeyEvent.KEYCODE_BACK:
                // la B del mando a veces llega como "Atrás": no debe sacar del juego
                if (fromGamepad(e)) { code = 38; btn = "b"; break; }
                if (e.getAction() == KeyEvent.ACTION_UP) askExit();
                return true;
            case KeyEvent.KEYCODE_BUTTON_B:          // B: cambiar de bola en Collar de Colores
                code = 38; btn = "b"; break;
            case KeyEvent.KEYCODE_BUTTON_X:
                code = 38; btn = "x"; break;
            case KeyEvent.KEYCODE_BUTTON_Y:
                code = 38; btn = "y"; break;
            case KeyEvent.KEYCODE_BUTTON_L1:
                code = 38; btn = "l1"; break;
            case KeyEvent.KEYCODE_BUTTON_R1:
                code = 38; btn = "r1"; break;
            case KeyEvent.KEYCODE_DPAD_UP:
                code = 38; btn = "up"; break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                code = 40; btn = "down"; break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                code = 37; btn = "left"; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                code = 39; btn = "right"; break;
            default:
                return super.dispatchKeyEvent(e);   // volumen, inicio...
        }
        int slot = slotOf(e.getDeviceId(), fromGamepad(e));
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            if (e.getRepeatCount() == 0 || code == 37 || code == 39) sendKey(slot, btn, code, true);
        } else if (e.getAction() == KeyEvent.ACTION_UP) {
            sendKey(slot, btn, code, false);
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
        js("window.__pads?__pads.pause(true):window.__pad&&__pad.pause(true)");   // pausa mientras se pregunta
        exitDialog = new AlertDialog.Builder(this)
                .setTitle("¿Salir del juego?")
                .setPositiveButton("Salir", (d, w) -> finish())
                .setNeutralButton("Elegir otro juego", (d, w) -> web.loadUrl(HOME))
                .setNegativeButton("Seguir jugando", (d, w) -> js("window.__pad&&__pad.pause(false)"))
                .setOnCancelListener(d -> js("window.__pad&&__pad.pause(false)"))
                .create();
        exitDialog.show();
        exitDialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent e) {
        boolean joystick = (e.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                || (e.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD;
        if (joystick && e.getAction() == MotionEvent.ACTION_MOVE) {
            int slot = slotOf(e.getDeviceId(), true);
            float x = e.getAxisValue(MotionEvent.AXIS_X);
            float y = e.getAxisValue(MotionEvent.AXIS_Y);
            if (Math.abs(x - lastX[slot]) > 0.02f || Math.abs(y - lastY[slot]) > 0.02f) {
                lastX[slot] = x; lastY[slot] = y;
                js("if(window.__pads)__pads.axes(" + slot + "," + x + "," + y + ");"
                        + "else if(window.__pad)__pad.axes(" + x + "," + y + ");");
            }
            // la cruceta del mando de Xbox llega como "hat": la convertimos en flechas
            int hx = Math.round(e.getAxisValue(MotionEvent.AXIS_HAT_X));
            int hy = Math.round(e.getAxisValue(MotionEvent.AXIS_HAT_Y));
            if (hx != hatX[slot]) {
                if (hatX[slot] != 0) sendKey(slot, hatX[slot] < 0 ? "left" : "right", hatX[slot] < 0 ? 37 : 39, false);
                if (hx != 0) sendKey(slot, hx < 0 ? "left" : "right", hx < 0 ? 37 : 39, true);
                hatX[slot] = hx;
            }
            if (hy != hatY[slot]) {
                if (hatY[slot] != 0) sendKey(slot, hatY[slot] < 0 ? "up" : "down", hatY[slot] < 0 ? 38 : 40, false);
                if (hy != 0) sendKey(slot, hy < 0 ? "up" : "down", hy < 0 ? 38 : 40, true);
                hatY[slot] = hy;
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
