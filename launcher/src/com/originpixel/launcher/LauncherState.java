package com.originpixel.launcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Framework-neutral home, dock, folder and grid contracts for the future Android launcher. */
public final class LauncherState {
    public record AppEntry(String id, String label) {
        public AppEntry { Objects.requireNonNull(id); Objects.requireNonNull(label); }
    }

    public record Snapshot(int columns, int rows, List<AppEntry> home, List<AppEntry> dock,
                           String openFolderId) {}

    private final int columns;
    private final int rows;
    private final int dockCapacity;
    private final List<AppEntry> home = new ArrayList<>();
    private final List<AppEntry> dock = new ArrayList<>();
    private String openFolderId;

    public LauncherState(int columns, int rows, int dockCapacity) {
        if (columns < 1 || rows < 1 || dockCapacity < 1) throw new IllegalArgumentException("Grid dimensions must be positive");
        this.columns = columns;
        this.rows = rows;
        this.dockCapacity = dockCapacity;
    }

    public synchronized boolean placeOnHome(AppEntry app, int index) {
        Objects.requireNonNull(app);
        if (containsId(app.id()) || home.size() >= columns * rows || index < 0 || index > home.size()) return false;
        home.add(index, app);
        return true;
    }

    public synchronized boolean placeInDock(AppEntry app) {
        Objects.requireNonNull(app);
        if (containsId(app.id()) || dock.size() >= dockCapacity) return false;
        dock.add(app);
        return true;
    }

    public synchronized boolean openFolder(String folderId) {
        if (folderId == null || folderId.isBlank()) return false;
        openFolderId = folderId;
        return true;
    }

    public synchronized void closeFolder() { openFolderId = null; }

    public synchronized Snapshot snapshot() {
        return new Snapshot(columns, rows, List.copyOf(home), List.copyOf(dock), openFolderId);
    }

    private boolean containsId(String id) {
        return home.stream().anyMatch(app -> app.id().equals(id)) || dock.stream().anyMatch(app -> app.id().equals(id));
    }
}
