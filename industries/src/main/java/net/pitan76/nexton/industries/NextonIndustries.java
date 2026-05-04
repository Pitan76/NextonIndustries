package net.pitan76.nexton.industries;

import net.pitan76.mcpitanlib.api.util.CompatIdentifier;
import net.pitan76.mcpitanlib.fabric.ExtendModInitializer;
import net.pitan76.mcpitanlib.midohra.registry.MidohraRegistry;

public class NextonIndustries extends ExtendModInitializer {

    public static final String MOD_ID = "nextonindustries";
    public static final String MOD_NAME = "Nexton Industries";
    public static final String MOD_NAMESPACE = "nexton";

    public static MidohraRegistry registry;

    @Override
    public void init() {

    }

    // ----

    @Override
    public String getId() {
        return MOD_ID;
    }

    @Override
    public String getName() {
        return MOD_NAME;
    }

    public static CompatIdentifier _id(String path) {
        return CompatIdentifier.of(MOD_NAMESPACE, path);
    }
}
