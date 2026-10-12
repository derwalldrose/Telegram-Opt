package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.utils.proxy.ProxySettings;

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

    private static ConnectivityManager.NetworkCallback defaultCallback;
    private static ConnectivityManager.NetworkCallback vpnCallback;
    private static boolean retriesScheduled;
    private static final Runnable applyRunnable = GramOpts::applyProxyForVpnNow;

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

    public static boolean inAppProxyBlockedByVpn() {
        return AUTO_DISABLE_PROXY_ON_VPN && isVpnEnabled();
    }

    public static boolean isVpnEnabled() {
        try {
            ConnectivityManager cm = (ConnectivityManager) ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return false;
            }
            Network[] networks = cm.getAllNetworks();
            if (networks != null) {
                for (Network network : networks) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        return true;
                    }
                }
            }
            Network active = cm.getActiveNetwork();
            NetworkCapabilities caps = cm.getNetworkCapabilities(active);
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return true;
            }
            NetworkInfo vpnInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_VPN);
            if (vpnInfo != null && vpnInfo.isConnectedOrConnecting()) {
                return true;
            }
            NetworkInfo[] all = cm.getAllNetworkInfo();
            if (all != null) {
                for (NetworkInfo info : all) {
                    if (info != null && info.getType() == ConnectivityManager.TYPE_VPN && info.isConnectedOrConnecting()) {
                        return true;
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    public static void applyProxyForVpn() {
        if (!AUTO_DISABLE_PROXY_ON_VPN) {
            return;
        }
        ensureVpnCallback();
        AndroidUtilities.cancelRunOnUIThread(applyRunnable);
        applyProxyForVpnNow();
        if (!retriesScheduled) {
            retriesScheduled = true;
            AndroidUtilities.runOnUIThread(applyRunnable, 500);
            AndroidUtilities.runOnUIThread(applyRunnable, 2000);
            AndroidUtilities.runOnUIThread(applyRunnable, 5000);
        }
    }

    private static SharedPreferences mainConfig() {
        return ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Context.MODE_PRIVATE);
    }

    public static void persistDisabledByVpn() {
        if (ApplicationLoader.applicationContext == null) {
            return;
        }
        SharedPreferences prefs = mainConfig();
        if (prefs.getBoolean("proxy_enabled", false) || prefs.getBoolean("gramProxyDisabledByVpn", false)) {
            prefs.edit().putBoolean("proxy_enabled", false).putBoolean("gramProxyDisabledByVpn", true).commit();
        }
    }

    private static void applyProxyForVpnNow() {
        if (!AUTO_DISABLE_PROXY_ON_VPN || ApplicationLoader.applicationContext == null) {
            return;
        }
        SharedConfig.loadProxyList();
        boolean vpn = isVpnEnabled();
        SharedPreferences prefs = mainConfig();
        boolean enabled = prefs.getBoolean("proxy_enabled", false);
        ProxySettings settings = ProxySettings.fromSharedPreferences(prefs);
        boolean weDisabled = prefs.getBoolean("gramProxyDisabledByVpn", false);

        if (vpn && enabled) {
            prefs.edit().putBoolean("proxy_enabled", false).putBoolean("gramProxyDisabledByVpn", true).commit();
            ConnectionsManager.setProxySettings(false, null);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
        } else if (!vpn && weDisabled) {
            if (SharedConfig.currentProxy == null && settings.isValid()) {
                SharedConfig.currentProxy = new SharedConfig.ProxyInfo(settings);
            }
            if (SharedConfig.currentProxy == null) {
                return;
            }
            prefs.edit().putBoolean("proxy_enabled", true).putBoolean("gramProxyDisabledByVpn", false).commit();
            ConnectionsManager.setProxySettings(true, SharedConfig.currentProxy.settings);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
        }
    }

    private static void ensureVpnCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || ApplicationLoader.applicationContext == null) {
            return;
        }
        try {
            ConnectivityManager cm = (ConnectivityManager) ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return;
            }
            if (defaultCallback == null) {
                defaultCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        scheduleApply();
                    }

                    @Override
                    public void onLost(Network network) {
                        scheduleApply();
                    }

                    @Override
                    public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                        scheduleApply();
                    }
                };
                cm.registerDefaultNetworkCallback(defaultCallback);
            }
            if (vpnCallback == null) {
                vpnCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        scheduleApply();
                    }

                    @Override
                    public void onLost(Network network) {
                        scheduleApply();
                    }
                };
                NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                        .build();
                cm.registerNetworkCallback(request, vpnCallback);
            }
        } catch (Exception ignore) {
        }
    }

    private static void scheduleApply() {
        AndroidUtilities.cancelRunOnUIThread(applyRunnable);
        AndroidUtilities.runOnUIThread(applyRunnable, 300);
    }
}
