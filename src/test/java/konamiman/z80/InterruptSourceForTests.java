package konamiman.z80;

import java.util.EventObject;
import java.util.Optional;

import konamiman.z80.interfaces.Z80InterruptSource;
import vavi.util.Debug;
import dotnet4j.util.compat.EventHandler;


public class InterruptSourceForTests implements Z80InterruptSource {

    public void fireNmi() {
        if (nmiInterruptPulse != null)
            nmiInterruptPulse.fireEvent(new EventObject(this));
        else
            Debug.println("nmiInterruptPulse is null");
    }

    private EventHandler<EventObject> nmiInterruptPulse = new EventHandler<>();
    public /*event*/ EventHandler<EventObject> nmiInterruptPulse() {
        return nmiInterruptPulse;
    }

    private boolean intLineIsActive;

    public boolean isIntLineIsActive() {
        return intLineIsActive;
    }

    public void setIntLineIsActive(boolean value) {
        intLineIsActive = value;
    }

    private Byte valueOnDataBus;

    public Optional<Byte> getValueOnDataBus() {
        return valueOnDataBus != null ? Optional.of(valueOnDataBus) : Optional.empty();
    }

    public void setValueOnDataBus(Byte value) {
        valueOnDataBus = value;
    }
}
