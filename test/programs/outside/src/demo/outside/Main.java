package demo.outside;

/** The main run touches no outside class; "reach" and "reach-jar" do (the negative controls). */
public class Main {
    public static void main(String[] args) {
        Events events = new Events();
        events.listen(new Recorder());
        System.out.println("events: " + Events.describe());
        System.out.println("store: " + Store.kind());
        System.out.println("texts: " + Texts.plain("plain") + " " + Texts.library());
        System.out.println("listener: " + Recorder.name());
        events.fire("old", "new");
        String what = args.length > 0 ? args[0] : "";
        if (what.equals("reach")) {
            events.support("old", "new");
        } else if (what.equals("reach-jar")) {
            System.out.println(Texts.fancy("x"));
        }
        System.out.println("done");
    }
}
