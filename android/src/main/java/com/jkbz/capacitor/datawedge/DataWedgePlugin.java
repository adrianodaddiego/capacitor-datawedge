package com.jkbz.capacitor.datawedge;

import com.getcapacitor.Plugin;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import static android.content.Context.RECEIVER_EXPORTED;

import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.ActivityNotFoundException;

import android.os.Build;
import android.os.Bundle;

import android.util.Log;


@CapacitorPlugin(name = "DataWedge")
public class DataWedgePlugin extends Plugin {

    private final DataWedge implementation = new DataWedge();

    // This is the default and can be changed when re-registering
    private String scanIntent = "com.capacitor.datawedge.RESULT_ACTION";

    @PluginMethod
    public void enable(PluginCall call) {
        Intent intent = implementation.enable();

        try {
            broadcast(intent);
        } catch (ActivityNotFoundException e) {
            call.reject("DataWedge is not installed or not running");
        }
    }
    @PluginMethod
    public void disable(PluginCall call) {
        Intent intent = implementation.disable();

        try {
            broadcast(intent);
        } catch (ActivityNotFoundException e) {
            call.reject("DataWedge is not installed or not running");
        }
    }

    @PluginMethod
    public void enableScanner(PluginCall call) {
        Intent intent = implementation.enableScanner();

        try {
            broadcast(intent);
        } catch (ActivityNotFoundException e) {
            call.reject("DataWedge is not installed or not running");
        }
    }

    @PluginMethod
    public void disableScanner(PluginCall call) {
        Intent intent = implementation.disableScanner();

        try {
            broadcast(intent);
        } catch (ActivityNotFoundException e) {
            call.reject("DataWedge is not installed or not running");
        }
    }

    @PluginMethod
    public void startScanning(PluginCall call) {
         Intent intent = implementation.startScanning();

         try {
            broadcast(intent);
         } catch (ActivityNotFoundException e) {
            call.reject("DataWedge is not installed or not running");
         }
    }

    @PluginMethod
    public void stopScanning(PluginCall call) {
        Intent intent = implementation.stopScanning();

        try {
            broadcast(intent);
        } catch (ActivityNotFoundException e) {
            call.reject("DataWedge is not installed or not running");
        }
    }

    @PluginMethod
    public void __registerReceiver(PluginCall call) { 
        Context context = getBridge().getContext();

        if (isReceiverRegistered) {
          context.unregisterReceiver(broadcastReceiver);
        }

        final String intentName = call.getString("intent");
        if (intentName != null) this.scanIntent = intentName;

        try {
            IntentFilter filter = new IntentFilter(this.scanIntent);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              context.registerReceiver(broadcastReceiver, filter, RECEIVER_EXPORTED);
            } else {
              context.registerReceiver(broadcastReceiver, filter);
            }

            isReceiverRegistered = true;
        } catch(Exception e) {
            Log.d("Capacitor/DataWedge", "Failed to register event receiver");
        }
    }

    private void broadcast(Intent intent) {
        Context context = getBridge().getContext();
        context.sendBroadcast(intent);
    }

    /**
     * Serializza un extra dell'intent in una stringa leggibile:
     * - byte[]            -> testo UTF-8 (es. decode_data)
     * - array / liste     -> "[v1, v2, ...]" (ricorsivo)
     * - Bundle annidati   -> "{k=..., ...}" (ricorsivo)
     * - String/numeri/bool-> valore diretto
     * Evita che valori non-stringa finiscano come hashcode (es. "[B@1a2b3c").
     */
    private String serializeExtra(Object value) {
        if (value == null) return null;

        if (value instanceof byte[]) {
            return new String((byte[]) value, java.nio.charset.StandardCharsets.UTF_8);
        }

        if (value instanceof Object[]) {
            StringBuilder sb = new StringBuilder("[");
            Object[] arr = (Object[]) value;
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(serializeExtra(arr[i]));
            }
            return sb.append("]").toString();
        }

        if (value instanceof java.util.Collection) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : (java.util.Collection<?>) value) {
                if (!first) sb.append(", ");
                sb.append(serializeExtra(item));
                first = false;
            }
            return sb.append("]").toString();
        }

        if (value instanceof Bundle) {
            Bundle b = (Bundle) value;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (String key : b.keySet()) {
                if (!first) sb.append(", ");
                sb.append(key).append("=").append(serializeExtra(b.get(key)));
                first = false;
            }
            return sb.append("}").toString();
        }

        return value.toString();
    }

    private boolean isReceiverRegistered = false;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (!action.equals(scanIntent)) return;

            try {
                String data = intent.getStringExtra("com.symbol.datawedge.data_string");
                String type = intent.getStringExtra("com.symbol.datawedge.label_type");

                // Fallback: se mancano gli extra DataWedge, uso quelli generici
                if (data == null) data = intent.getStringExtra("data");
                if (type == null) type = intent.getStringExtra("aimId");

                JSObject ret = new JSObject();
                ret.put("data", data);
                ret.put("type", type);

                // Tutti gli extra dell'intent, serializzati, per ispezione/debug nella schermata "Scansioni"
                JSObject extras = new JSObject();
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    for (String key : bundle.keySet()) {
                        extras.put(key, serializeExtra(bundle.get(key)));
                    }
                }
                ret.put("extras", extras);

                notifyListeners("scan", ret);
            } catch(Exception e) {}
        }
    };
}
