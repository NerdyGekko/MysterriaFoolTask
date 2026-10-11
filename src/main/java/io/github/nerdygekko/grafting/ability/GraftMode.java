package io.github.nerdygekko.grafting.ability;

public enum GraftMode {
    SINGLE("Single Target"),
    AREA("Area"),
    FORWARD("Forward"),
    REVERSE("Reverse");

    private final String displayName;

    GraftMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
