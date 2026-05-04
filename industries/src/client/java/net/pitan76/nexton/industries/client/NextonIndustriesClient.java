package net.pitan76.nexton.industries.client;

import net.fabricmc.api.ClientModInitializer;
import net.pitan76.mcpitanlib.api.client.registry.CompatRegistryClient;
import net.pitan76.nexton.industries.block.Blocks;

public class NextonIndustriesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CompatRegistryClient.registerCutoutBlock(Blocks.ENERGY_CABLE.get());
    }
}
