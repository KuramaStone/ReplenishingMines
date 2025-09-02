package com.github.kuramastone.replenishingmines.utils;

import com.github.kuramastone.replenishingmines.ReplenishingMines;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Does not use imports for luckperms to avoid
 */
public class LuckPermUtils {

    private static Object luckPermsApi;
    private static boolean hasGivenLuckPermsMissingWarning = false;

    public static void connectWithLuckPerms() {
        luckPermsApi = net.luckperms.api.LuckPermsProvider.get();
    }

    /**
     * Check if a uuid registered with luckperms has this permission. Use only for offline-players as it can be performance heavy.
     *
     * @return True if they have permission.
     */
    public static boolean hasPermissionOfflineCheck(UUID uuid, String permission) {

        try {
            if (luckPermsApi == null) connectWithLuckPerms();

            // Things are not imported here so the plugin can run without Luckperms
            if (luckPermsApi != null) {
                net.luckperms.api.LuckPerms luckPerms = (net.luckperms.api.LuckPerms) luckPermsApi;
                try {
                    net.luckperms.api.model.user.User user = luckPerms.getUserManager().loadUser(uuid).get();
                    if (user != null) {
                        net.luckperms.api.context.ContextManager contextManager = luckPerms.getContextManager();
                        net.luckperms.api.context.ImmutableContextSet contextSet = contextManager.getContext(user).orElseGet(contextManager::getStaticContext);

                        net.luckperms.api.cacheddata.CachedPermissionData permissionData = user.getCachedData().getPermissionData(net.luckperms.api.query.QueryOptions.contextual(contextSet));

                        return permissionData.checkPermission("*").asBoolean()
                                || permissionData.checkPermission(permission).asBoolean();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    return false;
                }
            }
        } catch (NoClassDefFoundError e) {
            if (!hasGivenLuckPermsMissingWarning)
                ReplenishingMines.LOGGER.warn("LuckPerms not found. This mod will be unable to check the permissions of offline players.");
            hasGivenLuckPermsMissingWarning = true;
        }
        return false;
    }


}
