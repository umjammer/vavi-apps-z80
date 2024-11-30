package konamiman.z80;

import java.util.EventObject;
import java.util.Optional;

import konamiman.z80.interfaces.Z80InterruptSource;
import vavi.util.Debug;
import dotnet4j.util.compat.EventHandler;


public class InterruptSourceForTests implements Z80InterruptSource {

    public void fireNmi() {
        nmiInterruptPulse.fireEvent(new EventObject(this));
    }

    private final EventHandler<EventObject> nmiInterruptPulse = new EventHandler<>();

    @Override
    public /*event*/ EventHandler<EventObject> nmiInterruptPulse() {
        return nmiInterruptPulse;
    }

    private boolean intLineIsActive;

    @Override
    public boolean isIntLineIsActive() {
        return intLineIsActive;
    }

    public void setIntLineIsActive(boolean value) {
        intLineIsActive = value;
    }

    private Byte valueOnDataBus;

    @Override
    public Optional<Byte> getValueOnDataBus() {
        return valueOnDataBus != null ? Optional.of(valueOnDataBus) : Optional.empty();
    }

    public void setValueOnDataBus(Byte value) {
        valueOnDataBus = value;
    }
}
