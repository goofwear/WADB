package moe.haruue.wadb.service;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.annotation.RequiresApi;

import moe.haruue.wadb.R;
import moe.haruue.wadb.WadbApplication;
import moe.haruue.wadb.events.Events;
import moe.haruue.wadb.events.GlobalRequestHandler;
import moe.haruue.wadb.events.WadbStateChangedEvent;
import moe.haruue.wadb.util.NetworksUtils;

/**
 * @author Haruue Icymoon haruue@caoyue.com.cn
 */

@RequiresApi(api = Build.VERSION_CODES.N)
public abstract class WadbTileService extends TileService implements WadbStateChangedEvent {

    private static final String TAG = "WadbTileService";

    private final Runnable mStartWadbRunnable = () -> {
        GlobalRequestHandler.startWadb(WadbApplication.getWadbPort());
    };

    private static final Runnable STOP_WADB = GlobalRequestHandler::stopWadb;

    @Override
    public void onCreate() {
        super.onCreate();
        Events.registerAll(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Events.unregisterAll(this);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();

        int port;
        if ((port = GlobalRequestHandler.getWadbPort()) != -1) {
            showStateOn(NetworksUtils.getLocalIPAddress(this), port);
        } else {
            showStateOff();
        }
    }

    @Override
    public void onClick() {
        super.onClick();

        Log.d(TAG, "onClick");
        boolean enableScreenLockSwitch = WadbApplication.getDefaultSharedPreferences().getBoolean("pref_key_screen_lock_switch", false);
        if (getQsTile().getState() == Tile.STATE_ACTIVE) {
            if (enableScreenLockSwitch) {
                STOP_WADB.run();
            } else {
                unlockAndRun(STOP_WADB);
            }
        } else {
            if (enableScreenLockSwitch) {
                mStartWadbRunnable.run();
            } else {
                unlockAndRun(mStartWadbRunnable);
            }
        }
    }

    private void showStateOn(String ip, int port) {
        Log.d(TAG, "showStateOn");

        final Tile tile = getQsTile();
        if (tile.getState() == Tile.STATE_ACTIVE) {
            return;
        }
        final Context context = this;
        tile.setState(Tile.STATE_ACTIVE);
        tile.setIcon(Icon.createWithResource(context, R.drawable.ic_qs_network_adb_on));
        tile.setLabel(ip + ":" + port);
        tile.updateTile();
    }

    private void showStateOff() {
        Log.d(TAG, "showStateOff");

        final Tile tile = getQsTile();
        if (tile.getState() == Tile.STATE_INACTIVE) {
            return;
        }
        final Context context = this;
        tile.setState(Tile.STATE_INACTIVE);
        tile.setIcon(Icon.createWithResource(context, R.drawable.ic_qs_network_adb_off));
        tile.setLabel(context.getString(R.string.app_name));
        tile.updateTile();
    }

    private void showStateUnavailable() {
        Tile tile = getQsTile();
        tile.setState(Tile.STATE_UNAVAILABLE);
        tile.updateTile();
    }

    @Override
    public void onWadbStarted(int port) {
        showStateOn(NetworksUtils.getLocalIPAddress(this), port);
    }

    @Override
    public void onWadbStopped() {
        showStateOff();
    }
}
