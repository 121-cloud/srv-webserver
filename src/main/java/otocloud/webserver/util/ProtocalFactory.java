package otocloud.webserver.util;

import otocloud.webserver.protocal.BridgeProtocal;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by better/zhangye on 15/9/23.
 */
public class ProtocalFactory {
    static ConcurrentHashMap<String, BridgeProtocal> protocals = new ConcurrentHashMap<>();

    public static BridgeProtocal get(String name) {
        if (protocals.containsKey(name)) {
            return protocals.get(name);
        }

        if (name.equals(ProtocalKind.Basic)) {
            BridgeProtocal basic = BridgeProtocal.basicProtocal();
            protocals.put(name, basic);

            return BridgeProtocal.basicProtocal();
        }

        //默认返回基本桥接协议
        return BridgeProtocal.basicProtocal();
    }

    enum ProtocalKind {
        Basic("basic");

        private String name;

        ProtocalKind(String name) {
            this.setName(name);
        }

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
    }
}
