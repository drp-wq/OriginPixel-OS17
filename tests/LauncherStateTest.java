package com.originpixel.launcher;

public final class LauncherStateTest {
    public static void main(String[] args) {
        LauncherState state = new LauncherState(4, 5, 5);
        var mail = new LauncherState.AppEntry("mail", "Mail");
        check(state.placeOnHome(mail, 0), "place first home app");
        check(!state.placeInDock(mail), "duplicate app rejected across surfaces");
        check(state.placeInDock(new LauncherState.AppEntry("camera", "Camera")), "place dock app");
        check(state.openFolder("work"), "open folder");
        check("work".equals(state.snapshot().openFolderId()), "folder state is observable");
        state.closeFolder();
        check(state.snapshot().openFolderId() == null, "folder closes");
        check(!state.placeOnHome(new LauncherState.AppEntry("bad", "Bad"), 2), "reject sparse insertion index");
        boolean immutable = false;
        try { state.snapshot().home().add(new LauncherState.AppEntry("mutate", "Mutate")); }
        catch (UnsupportedOperationException expected) { immutable = true; }
        check(immutable, "snapshot list is immutable");
        System.out.println("LauncherStateTest PASS (7 assertions)");
    }
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
}
