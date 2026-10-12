package org.telegram.messenger;

import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_stories;

public final class GramOpts {

    private GramOpts() {}

    public static final boolean SEND_LARGE_PHOTO = true;
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
}
