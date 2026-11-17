package demo.outside;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/** Implements an interface outside the world: made and passed, its method never called by the main run. */
public class Recorder implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        System.out.println("changed: " + e.getPropertyName());
    }

    public static String name() {
        return "recorder";
    }
}
