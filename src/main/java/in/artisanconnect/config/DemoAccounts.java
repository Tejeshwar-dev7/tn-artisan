package in.artisanconnect.config;

public final class DemoAccounts {
    public static final String CUSTOMER_EMAIL = "demo.customer@tnartisan.example";
    public static final String MAKER_EMAIL = "demo.maker@tnartisan.example";
    public static final String PASSWORD = "ExploreTN2026!";

    private DemoAccounts() {}

    public static boolean isDemo(String email) {
        return CUSTOMER_EMAIL.equalsIgnoreCase(email) || MAKER_EMAIL.equalsIgnoreCase(email);
    }
}
