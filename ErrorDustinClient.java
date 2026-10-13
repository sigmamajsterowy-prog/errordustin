package pl.errordustin;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

public class ErrorDustinClient implements ClientModInitializer {
    // Ile tickow woda ma plynac, zanim ja zbierzemy przy zamianie dzwignia <-> cobweb.
    // Woda w Minecrafcie rusza dopiero po ok. 5 tickach, wiec nie da sie mniej.
    // Jesli zamiana nie dziala, sprobuj 8 albo 10.
    private static final int WATER_FLOW_TICKS = 6;

    public static KeyBinding webKey, waterKey, guiKey;

    private record Step(Runnable run, int waitAfter) {}

    private final ArrayDeque<Step> queue = new ArrayDeque<>();
    private int waitLeft = 0;
    private boolean internal = false;   // true, gdy to my sami klikamy (zeby nie odpalac siebie w kolko)
    private boolean flashWater = false; // PPM z wiadrem: zbierz wode od razu

    @Override
    public void onInitializeClient() {
        ErrorDustinConfig.load();

        webKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.errordustin.web", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.errordustin"));
        waterKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.errordustin.water", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.errordustin"));
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.errordustin.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.errordustin"));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        // PPM z wiadrem wody: vanilla stawia wode, a my w tym samym ticku ja zbieramy
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient && hand == Hand.MAIN_HAND && !internal
                    && ErrorDustinConfig.waterEnabled
                    && player.getMainHandStack().isOf(Items.WATER_BUCKET)) {
                flashWater = true;
            }
            return ActionResult.PASS;
        });

        // PPM: cobweb w rece na dzwigni albo dzwignia w rece na cobwebie
        UseBlockCallback.EVENT.register(this::onUseBlock);
    }

    private ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (!world.isClient || hand != Hand.MAIN_HAND || internal) return ActionResult.PASS;
        if (!ErrorDustinConfig.leverEnabled || !queue.isEmpty()) return ActionResult.PASS;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return ActionResult.PASS;

        Item held = player.getMainHandStack().getItem();
        Block target = world.getBlockState(hit.getBlockPos()).getBlock();
        boolean ok = (held == Items.COBWEB && target == Blocks.LEVER)
                || (held == Items.LEVER && target == Blocks.COBWEB);
        if (!ok) return ActionResult.PASS;

        int waterSlot = findHotbar(mc, Items.WATER_BUCKET);
        if (waterSlot == -1) return ActionResult.PASS;

        BlockPos pos = hit.getBlockPos();
        int itemSlot = player.getInventory().selectedSlot;

        // 1) postaw wode (zniszczy dzwignie / cobweba), 2) zbierz wode, 3) postaw nowy blok
        queue.add(new Step(() -> {
            selectSlot(mc, waterSlot);
            use(mc);
        }, WATER_FLOW_TICKS));
        queue.add(new Step(() -> use(mc), 0));
        queue.add(new Step(() -> {
            selectSlot(mc, itemSlot);
            placeAt(mc, pos);
            mc.player.swingHand(Hand.MAIN_HAND);
        }, 0));
        waitLeft = 0;
        return ActionResult.FAIL;
    }

    private void tick(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null || mc.world == null) {
            queue.clear();
            waitLeft = 0;
            flashWater = false;
            return;
        }

        while (guiKey.wasPressed()) mc.setScreen(new ErrorDustinScreen());

        // szybka woda: wiadro jest juz puste (vanilla postawilo wode) -> zbierz ja
        if (flashWater) {
            flashWater = false;
            if (mc.player.getMainHandStack().isOf(Items.BUCKET)) use(mc);
        }

        if (!queue.isEmpty()) {
            if (waitLeft > 0) {
                waitLeft--;
                return;
            }
            do {
                Step s = queue.poll();
                s.run().run();
                waitLeft = s.waitAfter();
            } while (waitLeft == 0 && !queue.isEmpty());
            return;
        }

        // klawisz: szybka pajeczyna
        while (webKey.wasPressed()) {
            if (!ErrorDustinConfig.webEnabled) continue;
            int slot = findHotbar(mc, Items.COBWEB);
            if (slot == -1) continue;
            if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) continue;
            int old = mc.player.getInventory().selectedSlot;
            selectSlot(mc, slot);
            placeBlock(mc, hit);
            mc.player.swingHand(Hand.MAIN_HAND);
            selectSlot(mc, old);
        }

        // klawisz: szybka woda (postaw i od razu zbierz)
        while (waterKey.wasPressed()) {
            if (!ErrorDustinConfig.waterEnabled) continue;
            int slot = findHotbar(mc, Items.WATER_BUCKET);
            if (slot == -1) continue;
            int old = mc.player.getInventory().selectedSlot;
            selectSlot(mc, slot);
            use(mc);
            if (mc.player.getMainHandStack().isOf(Items.BUCKET)) use(mc);
            mc.player.swingHand(Hand.MAIN_HAND);
            selectSlot(mc, old);
        }
    }

    private void use(MinecraftClient mc) {
        internal = true;
        try {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        } finally {
            internal = false;
        }
    }

    private void placeBlock(MinecraftClient mc, BlockHitResult hit) {
        internal = true;
        try {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        } finally {
            internal = false;
        }
    }

    // stawia trzymany przedmiot w miejscu pos, opierajac go o sasiedni pelny blok
    private void placeAt(MinecraftClient mc, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos n = pos.offset(d);
            if (mc.world.getBlockState(n).isSolidBlock(mc.world, n)) {
                Direction face = d.getOpposite();
                Vec3d v = Vec3d.ofCenter(n).add(
                        face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
                placeBlock(mc, new BlockHitResult(v, face, n, false));
                return;
            }
        }
    }

    private int findHotbar(MinecraftClient mc, Item wanted) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == wanted) return i;
        }
        return -1;
    }

    private void selectSlot(MinecraftClient mc, int slot) {
        if (slot < 0) return;
        mc.player.getInventory().selectedSlot = slot;
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }
}
