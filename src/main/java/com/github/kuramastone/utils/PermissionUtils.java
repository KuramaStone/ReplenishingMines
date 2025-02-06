package com.github.kuramastone.utils;

import com.github.kuramastone.ReplenishingMines;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.luckperms.api.LuckPerms;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Does not use imports for luckperms to avoid
 */
public class PermissionUtils {

    public static boolean hasLuckPerms() {
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Check if a Player with this uuid has this permission. If the player is offline, it polls LuckPerms.
     *
     * @return True if they have permission.
     */
    public static boolean hasPermission(UUID uuid, String permission) {
        ServerPlayerEntity serverPlayer = ReplenishingMines.getServer().getPlayerManager().getPlayer(uuid);

        if (serverPlayer == null)
            return hasLuckPerms() && LuckPermUtils.hasPermissionOfflineCheck(uuid, permission);
        else
            return Permissions.check(serverPlayer, permission);
    }


}
