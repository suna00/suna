package net.ion.ice.core.data.bind;

import net.ion.ice.ApplicationContextManager;

public class NodeBindingUtils {

    static NodeBindingService nodeBindingService;

    public static NodeBindingService getNodeBindingService() {
        if (nodeBindingService == null) {
            nodeBindingService = ApplicationContextManager.getBean(NodeBindingService.class);
        }
        return nodeBindingService;
    }
}
