package com.github.kuramastone.utils;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Does not use imports for luckperms
 */
public class LuckPermsUtils {
    private static Object luckPermsApi;

    public static void connectWithLuckPerms() {
        luckPermsApi = net.luckperms.api.LuckPermsProvider.get();
    }

    public static boolean hasPermission(String uuid, String permission) {
        if (luckPermsApi == null) connectWithLuckPerms();

            try {
                // Things are not imported here so the plugin can run without Luckperms
                if (luckPermsApi != null) {
                    net.luckperms.api.LuckPerms luckPerms = (net.luckperms.api.LuckPerms) luckPermsApi;
                    try {
                        net.luckperms.api.model.user.User user = luckPerms.getUserManager().loadUser(UUID.fromString(uuid)).get();
                        if (user != null) {
                            net.luckperms.api.context.ContextManager contextManager = luckPerms.getContextManager();
                            net.luckperms.api.context.ImmutableContextSet contextSet = contextManager.getContext(user).orElseGet(contextManager::getStaticContext);

                            net.luckperms.api.cacheddata.CachedPermissionData permissionData = user.getCachedData().getPermissionData(net.luckperms.api.query.QueryOptions.contextual(contextSet));

                            return permissionData.checkPermission("*").asBoolean()
                                    || permissionData.checkPermission(permission).asBoolean();
                        }
                    }
                    catch (InterruptedException | ExecutionException e) {
                        return false;
                    }
                }
            }
            catch (NoClassDefFoundError e) {
                e.printStackTrace();
                // it does not exist on the classpath
            }
        return false;
    }
}
