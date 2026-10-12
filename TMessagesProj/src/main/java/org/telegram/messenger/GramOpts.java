package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_stories;

public final class GramOpts {

    private GramOpts() {}

    public static final boolean SEND_LARGE_PHOTO = true;
    public static final boolean AUTO_DISABLE_PROXY_ON_VPN = true;
    public static final boolean DISABLE_SECOND_ADDRESS = false;
    public static final boolean DISABLE_SEND_TYPING = true;
    public static final boolean FORCE_OFFLINE = true;
    public static final boolean STORY_STEALTH = true;
    public static final boolean HIDE_STORIES = true;
    public static final boolean HIDE_PHONE = true;
    public static final boolean IGNORE_BLOCKED = true;
    public static final boolean REVEAL_SPOILERS = true;
    public static final boolean NOQUOTE_FORWARD = true;
    public static final boolean REPEAT = true;

    private static ConnectivityManager.NetworkCallback vpnCallback;

    public static TLObject filterOutgoing(TLObject obj) {
        if (obj == null) {
            return null;
        }
        if (DISABLE_SEND_TYPING && (obj instanceof TLRPC.TL_messages_setTyping || obj instanceof TLRPC.TL_messages_setEncryptedTyping)) {
            return null;
        }
        if (STORY_STEALTH && obj instanceof TL_stories.TL_stories_readStories) {
            return null;
        }
        if (FORCE_OFFLINE && obj instanceof TL_account.updateStatus) {
            ((TL_account.updateStatus) obj).offline = true;
        }
        return obj;
    }

    public static boolean isVpnEnabled() {
        try {
            ConnectivityManager cm = (ConnectivityManager) ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return false;
            }
            Network network = cm.getActiveNetwork();
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
        } catch (Exception ignore) {
            return false;
        }
    }

    public static void applyProxyForVpn() {
        if (!AUTO_DISABLE_PROXY_ON_VPN) {
            return;
        }
        ensureVpnCallback();
        applyProxyForVpnNow();
    }

    private static void applyProxyForVpnNow() {
        boolean vpn = isVpnEnabled();
        boolean enabled = SharedConfig.isProxyEnabled();
        SharedPreferences prefs = MessagesController.getGlobalMainSettings();
        boolean weDisabled = prefs.getBoolean("gramProxyDisabledByVpn", false);
        if (vpn && enabled) {
            prefs.edit().putBoolean("proxy_enabled", false).putBoolean("gramProxyDisabledByVpn", true).apply();
            ConnectionsManager.setProxySettings(false, null);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
        } else if (!vpn && weDisabled && SharedConfig.currentProxy != null) {
            prefs.edit().putBoolean("proxy_enabled", true).putBoolean("gramProxyDisabledByVpn", false).apply();
            ConnectionsManager.setProxySettings(true, SharedConfig.currentProxy.settings);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
        }
    }

    private static void ensureVpnCallback() {
        if (vpnCallback != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        try {
            ConnectivityManager cm = (ConnectivityManager) ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return;
            }
            vpnCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    AndroidUtilities.runOnUIThread(GramOpts::applyProxyForVpnNow);
                }

                @Override
                public void onLost(Network network) {
                    AndroidUtilities.runOnUIThread(GramOpts::applyProxyForVpnNow);
                }

                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                    AndroidUtilities.runOnUIThread(GramOpts::applyProxyForVpnNow);
                }
            };
            cm.registerDefaultNetworkCallback(vpnCallback);
        } catch (Exception ignore) {
            vpnCallback = null;
        }
    }
}
