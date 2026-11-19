package demo.modapp;

import demo.modlib.Lib;
import java.awt.Point;

public class App {
    public static Point where() { return new Point(Lib.name().length(), 0); }
}
