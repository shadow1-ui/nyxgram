package org.telegram.messenger;

/** Pure lifecycle/page rules shared by the native chat and its encrypted local archive. */
public final class NyxDeletedMessages {
    private NyxDeletedMessages() {}
    public static boolean visible(boolean deleted, boolean localCopy) { return !deleted || localCopy; }
    public static boolean retain(boolean enabled, boolean regularMode, boolean primary, boolean scheduled,
                                 boolean sent, boolean moved, boolean purging, boolean eligible) {
        return enabled && regularMode && primary && !scheduled && !sent && !moved && !purging && eligible;
    }
    public static int[] pageBounds(int minimum, int maximum, int anchor, boolean latest, boolean oldest) {
        int lower = maximum > 0 && !oldest ? Math.max(1, minimum) : 1;
        int upper = latest ? Integer.MAX_VALUE : maximum > 0 ? Math.max(maximum, anchor) : Math.max(0, anchor);
        return new int[]{lower, upper};
    }
    public static boolean inThread(int mid, long topic, int reply, int top, long thread, boolean forum) {
        return thread == 0 || (forum ? topic == thread : mid == thread || reply == thread || top == thread);
    }
}
