package org.cataclysm.server.motd;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.cataclysm.Cataclysm;
import org.cataclysm.api.listener.registrable.Registrable;

@Registrable
public class MotdListener implements Listener {

    @EventHandler
    private void onServerListPing(ServerListPingEvent event) {
        var topper = "              <#cd953f><obf>||</obf> <#a53131><b>Tʜᴇ Cᴀᴛᴀᴄʟʏꜱᴍ SMP</b> <#cd953f><obf>||</obf>";
        var footer = "          <#bcbcbc>Día ##/35 <#cd953f>• <#bcbcbc>#CataclysmSMP <#cd953f>• <#bcbcbc>1.21.5";

        footer = footer.replace("##", "" + Cataclysm.getDay());

        event.motd(MiniMessage.miniMessage().deserialize(topper + "\n" + footer));
    }

}
