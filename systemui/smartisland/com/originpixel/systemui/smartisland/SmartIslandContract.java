package com.originpixel.systemui.smartisland;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * OriginPixel OS 17 — Native SmartIsland Behavioral Contract (Framework-Agnostic).
 *
 * Pure Java 17 state machine covering:
 * - Concurrent activity arbitration & secondary pill selection
 * - High-priority interruption and clean restoration on dismiss
 * - Privacy redaction (collapsed & lock-screen) + non-suppressible privacy indicators
 * - Accessibility content descriptions, escalation announcements, and reduced-motion fallback
 * - System lifecycle hooks (screen-off / AOD, user switch)
 */
public final class SmartIslandContract {

    public enum Priority {
        CALL(100),
        PRIVACY_INDICATOR(90),
        TIMER(80),
        AIRPODS(60),
        CHARGING(50),
        MEDIA(40),
        NOTIFICATION(20);

        public final int weight;

        Priority(int weight) {
            this.weight = weight;
        }
    }

    public enum IslandState {
        HIDDEN,
        COLLAPSED,
        EXPANDED
    }

    public enum TransitionStyle {
        SPRING_MORPH,
        REDUCED_CROSSFADE
    }

    public record IslandPolicy(
            boolean enabled,
            boolean reducedMotion,
            boolean privateNotifications,
            boolean autoExpandHighPriority
    ) {
        public static IslandPolicy defaults() {
            return new IslandPolicy(true, false, true, true);
        }
    }

    public record IslandActivity(
            String id,
            Priority priority,
            String title,
            String subtitle,
            boolean sensitiveContent,
            long timestampMs
    ) {
        public IslandActivity {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(priority, "priority");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(subtitle, "subtitle");
        }
    }

    public record RenderSnapshot(
            IslandState state,
            IslandActivity primaryActivity,
            IslandActivity secondaryActivity,
            String displayTitle,
            String displaySubtitle,
            String accessibilityDescription,
            boolean announceForAccessibility,
            TransitionStyle transitionStyle,
            int activeCount
    ) {}

    private static final Comparator<IslandActivity> ACTIVITY_ORDER =
            Comparator.comparingInt((IslandActivity a) -> a.priority().weight)
                    .thenComparingLong(IslandActivity::timestampMs)
                    .thenComparing(IslandActivity::id);

    private final Map<String, IslandActivity> activities = new LinkedHashMap<>();
    private IslandPolicy policy = IslandPolicy.defaults();
    private IslandState state = IslandState.HIDDEN;
    private boolean deviceLocked = false;
    private boolean pendingAnnouncement = false;
    private String lastTopId = null;

    public synchronized void updatePolicy(IslandPolicy newPolicy) {
        this.policy = Objects.requireNonNull(newPolicy, "newPolicy");
        if (!policy.enabled()) {
            this.state = hasPrivacyIndicator() ? IslandState.COLLAPSED : IslandState.HIDDEN;
        } else if (!activities.isEmpty() && this.state == IslandState.HIDDEN) {
            this.state = IslandState.COLLAPSED;
        }
    }

    public synchronized void setDeviceLocked(boolean locked) {
        this.deviceLocked = locked;
        if (locked && state == IslandState.EXPANDED) {
            IslandActivity top = topActivity();
            if (top != null && top.sensitiveContent()) {
                state = IslandState.COLLAPSED;
            }
        }
    }

    public synchronized void onScreenOff() {
        if (state == IslandState.EXPANDED) {
            state = IslandState.COLLAPSED;
        }
    }

    public synchronized void onUserSwitched() {
        activities.clear();
        state = IslandState.HIDDEN;
        lastTopId = null;
        pendingAnnouncement = false;
    }

    public synchronized void post(IslandActivity activity) {
        Objects.requireNonNull(activity, "activity");
        activities.put(activity.id(), activity);
        IslandActivity newTop = topActivity();

        if (!policy.enabled() && !hasPrivacyIndicator()) {
            state = IslandState.HIDDEN;
            return;
        }

        boolean escalated = newTop != null && !Objects.equals(newTop.id(), lastTopId);
        if (escalated && newTop.priority().weight >= Priority.TIMER.weight) {
            pendingAnnouncement = true;
        }
        lastTopId = newTop == null ? null : newTop.id();

        if (policy.enabled() && policy.autoExpandHighPriority()
                && activity.priority().weight >= Priority.TIMER.weight
                && !(deviceLocked && activity.sensitiveContent())) {
            state = IslandState.EXPANDED;
        } else if (state == IslandState.HIDDEN) {
            state = IslandState.COLLAPSED;
        }
    }

    public synchronized void dismiss(String id) {
        activities.remove(id);
        if (activities.isEmpty()) {
            state = IslandState.HIDDEN;
            lastTopId = null;
            pendingAnnouncement = false;
            return;
        }
        IslandActivity newTop = topActivity();
        lastTopId = newTop == null ? null : newTop.id();
        if (!policy.enabled() && !hasPrivacyIndicator()) {
            state = IslandState.HIDDEN;
        } else if (state == IslandState.EXPANDED && newTop != null
                && newTop.priority().weight < Priority.TIMER.weight) {
            state = IslandState.COLLAPSED;
        }
    }

    public synchronized void setExpanded(boolean expanded) {
        if (activities.isEmpty() || (!policy.enabled() && !hasPrivacyIndicator())) {
            state = IslandState.HIDDEN;
            return;
        }
        if (expanded && deviceLocked) {
            IslandActivity top = topActivity();
            if (top != null && top.sensitiveContent()) {
                state = IslandState.COLLAPSED;
                return;
            }
        }
        state = expanded ? IslandState.EXPANDED : IslandState.COLLAPSED;
    }

    public synchronized void clearAll() {
        activities.clear();
        state = IslandState.HIDDEN;
        lastTopId = null;
        pendingAnnouncement = false;
    }

    public synchronized RenderSnapshot snapshot() {
        TransitionStyle style = policy.reducedMotion()
                ? TransitionStyle.REDUCED_CROSSFADE
                : TransitionStyle.SPRING_MORPH;

        boolean visible = (policy.enabled() && !activities.isEmpty()) || hasPrivacyIndicator();
        if (!visible) {
            return new RenderSnapshot(IslandState.HIDDEN, null, null, "", "", "", false, style, 0);
        }

        List<IslandActivity> ranked = activities.values().stream()
                .filter(a -> policy.enabled() || a.priority() == Priority.PRIVACY_INDICATOR)
                .sorted(ACTIVITY_ORDER.reversed())
                .toList();

        IslandActivity primary = ranked.isEmpty() ? null : ranked.get(0);
        IslandActivity secondary = ranked.size() > 1 ? ranked.get(1) : null;

        boolean redact = primary != null
                && primary.sensitiveContent()
                && (deviceLocked || (policy.privateNotifications() && state != IslandState.EXPANDED));

        String title = primary == null ? "" : primary.title();
        String subtitle = primary == null ? "" : (redact ? "Sensitive content hidden" : primary.subtitle());
        String a11y = primary == null ? "" : (title + (subtitle.isEmpty() ? "" : ": " + subtitle));
        boolean announce = pendingAnnouncement;
        pendingAnnouncement = false;

        return new RenderSnapshot(
                state, primary, secondary, title, subtitle, a11y, announce, style, ranked.size()
        );
    }

    private boolean hasPrivacyIndicator() {
        return activities.values().stream().anyMatch(a -> a.priority() == Priority.PRIVACY_INDICATOR);
    }

    private IslandActivity topActivity() {
        return activities.values().stream().max(ACTIVITY_ORDER).orElse(null);
    }
}
