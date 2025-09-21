package org.cataclysm.game.world.time;

import org.cataclysm.Cataclysm;
import org.cataclysm.api.data.json.JsonConfig;
import org.cataclysm.game.world.time.data.TimeData;
import org.cataclysm.game.world.time.events.ChangeDayEvent;
import org.jetbrains.annotations.NotNull;

public class TimeManager {
    private final TimeData data;

    public TimeManager(TimeData data) {
        this.data = data;
    }

    public void setDay(int day) {
        this.data.setDay(day);
        Cataclysm.setDay(day);
        new ChangeDayEvent(day).callEvent();
    }

    public int getDay() {
        return this.data.getDay();
    }

    public void save(@NotNull JsonConfig jsonConfig) {
        jsonConfig.setJsonObject(Cataclysm.getGson().toJsonTree(this.data).getAsJsonObject());
        try {jsonConfig.save();}
        catch (Exception e) {throw new RuntimeException(e);}
    }

    public static void restore(@NotNull JsonConfig jsonConfig) {
        if (!jsonConfig.getJsonObject().entrySet().isEmpty()) {
            TimeData restoredData = Cataclysm.getGson().fromJson(jsonConfig.getJsonObject(), TimeData.class);
            TimeManager timeManager = new TimeManager(restoredData);
            Cataclysm.setTimeManager(timeManager);
            Cataclysm.setDay(timeManager.getDay());
        }
        else Cataclysm.setTimeManager(new TimeManager(new TimeData()));
    }
}
