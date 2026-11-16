package demo.outside;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Names java.desktop's java.beans (as log4j-core's LoggerContext). PropertyChangeEvent and PropertyChangeListener are
 * in the world (GH5 (iii)'s first slice, 2026-11-16): fire runs. PropertyChangeSupport is not: support() is a stub.
 */
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

    public void support(Object old, Object neu) {
        PropertyChangeSupport s = new PropertyChangeSupport(this);
        s.addPropertyChangeListener(listener);
        s.firePropertyChange("config", old, neu);
    }
}
