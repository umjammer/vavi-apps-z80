package konamiman;

import java.util.EventObject;
import java.util.Optional;

import konamiman.DependenciesInterfaces.IZ80InterruptSource;
import vavi.util.dotnet.EventHandler;


public class InterruptSourceForTests implements IZ80InterruptSource {

    public void FireNmi() {
        if (nmiInterruptPulse != null)
            nmiInterruptPulse.fireEvent(new EventObject(this));
    }

    EventHandler<EventObject> nmiInterruptPulse = new EventHandler<>();
    public /*event*/ EventHandler<EventObject> NmiInterruptPulse() {
        return nmiInterruptPulse;
    }
    private boolean intLineIsActive;

    public boolean getIntLineIsActive() {
        return intLineIsActive;
    }

    public void setIntLineIsActive(boolean value) {
        intLineIsActive = value;
    }

    private Byte ValueOnDataBus;

    public Optional<Byte> getValueOnDataBus() {
        return Optional.of(ValueOnDataBus);
    }

    public void setValueOnDataBus(Byte value) {
        ValueOnDataBus = value;
    }
}
