package dotnet4j.util.compat;


import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;


/**
 * EventHandler.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-06-21 nsano initial version <br>
 */
public class EventHandler<T extends EventObject> {

    public interface EventListener<T> {
        void exec(T e);
    }

    private List<EventListener<T>> listeners = new ArrayList<>();

    public void addListener(EventListener<T> listener) {
        listeners.add(listener);
    }

    public void removeListener(EventListener<T> listener) { listeners.remove(listener); }

    public void fireEvent(T e) {
        listeners.forEach(l -> l.exec(e));
    }
}
