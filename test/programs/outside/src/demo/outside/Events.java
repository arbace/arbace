package demo.outside;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/** Names java.desktop's java.beans (as log4j-core's LoggerContext): fire makes a PropertyChangeEvent. */
public class Events {
    private PropertyChangeListener listener;

    public static String describe() {
        return "events";
    }

    public void listen(PropertyChangeListener l) {
        listener = l;
    }

    public void fire(Object old, Object neu) {
        PropertyChangeEvent e = new PropertyChangeEvent(this, "config", old, neu);
        if (listener != null) {
            listener.propertyChange(e);
        }
    }
}
