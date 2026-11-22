/*
 * Arbace (overlay/jdk, queue 416: StackWalker over the interpreted frames; doc/classes/JAVA-DEFERRED-TRIAGE.md,
 * F20): jrt's java.lang.StackWalker. jdk26u's (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/lang/StackWalker.java) walks the JVM's frames lazily through
 * StackStreamFactory and the VM's natives; here the frames are jrt's (go/arbace/jrt/throwable.ae, stackFrames:
 * the Go frames of translated code named by j2g's frame table, the interpreter's shadow stack of the Java forms
 * made at run time, Compiler$CF$Rt.snapshot), taken when walk or getCallerClass is called, as one array, and
 * streamed from it; a walk's stream is not valid after walk returns, as jdk26u's. No continuation walkers
 * (jdk.internal.vm.Continuation is not in the Go build), no locals and operands, no bytecode index (-1) and no
 * method type (UnsupportedOperationException: a frame of the Go build has no descriptor). The public API, its
 * checks and their messages are jdk26u's (Copyright (c) 2015, 2024, Oracle and/or its affiliates, GPL 2 with the
 * Classpath Exception: LICENSE.md). Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.lang;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** A stack walker: the frames of the current thread, from the method that walks. */
public final class StackWalker {

    /** A frame of the stack, its method's. */
    public interface StackFrame {
        /** The binary name of the declaring class of the frame's method. */
        public String getClassName();

        /** The name of the frame's method. */
        public String getMethodName();

        /** The declaring class of the frame's method (RETAIN_CLASS_REFERENCE). */
        public Class<?> getDeclaringClass();

        /** The method type of the frame's method. */
        public default java.lang.invoke.MethodType getMethodType() {
            throw new UnsupportedOperationException();
        }

        /** The descriptor of the frame's method. */
        public default String getDescriptor() {
            throw new UnsupportedOperationException();
        }

        /** The index of the current bytecode instruction, -1 when unknown. */
        public int getByteCodeIndex();

        /** The name of the source file of the frame's method, or null. */
        public String getFileName();

        /** The line number of the frame's execution point, a negative number when unknown. */
        public int getLineNumber();

        /** Whether the frame's method is native. */
        public boolean isNativeMethod();

        /** The frame as a StackTraceElement. */
        public StackTraceElement toStackTraceElement();
    }

    /** The options of a stack walker. */
    public enum Option {
        /** The frames keep their Class objects (getDeclaringClass, getCallerClass). */
        RETAIN_CLASS_REFERENCE,
        /** The frames drop their method information. */
        DROP_METHOD_INFO,
        /** The reflection frames are shown (Method.invoke, Constructor.newInstance). */
        SHOW_REFLECT_FRAMES,
        /** The hidden frames are shown (the reflection frames and those of hidden classes: a lambda's). */
        SHOW_HIDDEN_FRAMES;
    }

    private static final EnumSet<Option> DEFAULT_EMPTY_OPTION = EnumSet.noneOf(Option.class);

    private static final StackWalker DEFAULT_WALKER = new StackWalker(DEFAULT_EMPTY_OPTION);

    private final Set<Option> options;
    private final boolean retainClassRef;
    private final boolean dropMethodInfo;
    // the frames shown beside the others: bit 1 reflection frames, bit 2 hidden frames (frames0's show)
    private final int show;

    private StackWalker(EnumSet<Option> options) {
        this.options = options;
        this.retainClassRef = options.contains(Option.RETAIN_CLASS_REFERENCE);
        this.dropMethodInfo = options.contains(Option.DROP_METHOD_INFO);
        this.show = options.contains(Option.SHOW_HIDDEN_FRAMES) ? 3
                  : options.contains(Option.SHOW_REFLECT_FRAMES) ? 1 : 0;
    }

    /** The stack walker that skips the reflection and hidden frames and keeps no Class objects. */
    public static StackWalker getInstance() {
        return DEFAULT_WALKER;
    }

    /** A stack walker with option. */
    public static StackWalker getInstance(Option option) {
        return getInstance(EnumSet.of(Objects.requireNonNull(option)));
    }

    /** A stack walker with options. */
    public static StackWalker getInstance(Set<Option> options) {
        if (options.isEmpty()) {
            return DEFAULT_WALKER;
        }
        return new StackWalker(toEnumSet(options));
    }

    /** A stack walker with options; estimateDepth, the number of frames it expects, must be positive. */
    public static StackWalker getInstance(Set<Option> options, int estimateDepth) {
        if (estimateDepth <= 0) {
            throw new IllegalArgumentException("estimateDepth must be > 0");
        }
        return new StackWalker(toEnumSet(options));
    }

    private static EnumSet<Option> toEnumSet(Set<Option> options) {
        Objects.requireNonNull(options);
        if (options.isEmpty()) {
            return DEFAULT_EMPTY_OPTION;
        } else {
            return EnumSet.copyOf(options);
        }
    }

    /**
     * Applies function to the stream of the current thread's frames, from the method calling walk; the stream
     * is closed when walk returns.
     */
    public <T> T walk(Function<? super Stream<StackFrame>, ? extends T> function) {
        Objects.requireNonNull(function);
        Frames frames = new Frames(this, frames0(show));
        try {
            return function.apply(StreamSupport.stream(frames, false));
        } finally {
            frames.closed = true;
        }
    }

    /** Performs action on each frame of the current thread's stack, from the method calling forEach. */
    public void forEach(Consumer<? super StackFrame> action) {
        Objects.requireNonNull(action);
        walk(s -> {
            s.forEach(action);
            return null;
        });
    }

    /**
     * The class of the caller of the method that calls getCallerClass, the reflection and hidden frames
     * skipped whatever the options.
     */
    public Class<?> getCallerClass() {
        if (!retainClassRef) {
            throw new UnsupportedOperationException("This stack walker " +
                    "does not have RETAIN_CLASS_REFERENCE access");
        }
        Object[] fs = frames0(0);
        if (fs.length < 4) {
            throw new IllegalCallerException("no caller frame");
        }
        return (Class<?>) fs[2];
    }

    /**
     * The current thread's frames from the caller of the StackWalker method that calls frames0 (StackWalker's
     * own frames left out), two slots a frame: its Class and its StackTraceElement. The reflection frames
     * when show has bit 1, the hidden frames when it has bit 2. jrt's native (throwable.ae).
     */
    private static native Object[] frames0(int show);

    // a walk's frames: a spliterator over frames0's array, not valid once its walk returned
    private static final class Frames implements Spliterator<StackFrame> {
        private final StackWalker walker;
        private final Object[] fs;
        private int i;
        boolean closed;

        Frames(StackWalker walker, Object[] fs) {
            this.walker = walker;
            this.fs = fs;
        }

        private void checkState() {
            if (closed) {
                throw new IllegalStateException("This stack stream is not valid for walking.");
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super StackFrame> action) {
            checkState();
            Objects.requireNonNull(action);
            if (i >= fs.length) {
                return false;
            }
            Frame f = new Frame(walker, (Class<?>) fs[i], (StackTraceElement) fs[i + 1]);
            i += 2;
            action.accept(f);
            return true;
        }

        @Override
        public void forEachRemaining(Consumer<? super StackFrame> action) {
            checkState();
            Objects.requireNonNull(action);
            while (i < fs.length) {
                Frame f = new Frame(walker, (Class<?>) fs[i], (StackTraceElement) fs[i + 1]);
                i += 2;
                action.accept(f);
            }
        }

        @Override
        public Spliterator<StackFrame> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return (fs.length - i) / 2;
        }

        // not SIZED, as jdk26u's StackFrameTraverser: Stream.count() of a SIZED spliterator takes its size and
        // never walks, so a stream used after its walk ended did not throw IllegalStateException (StackStreamState)
        @Override
        public int characteristics() {
            return ORDERED | IMMUTABLE | NONNULL;
        }
    }

    // a frame of a walk
    private static final class Frame implements StackFrame {
        private final StackWalker walker;
        private final Class<?> declaringClass;
        private final StackTraceElement ste;

        Frame(StackWalker walker, Class<?> declaringClass, StackTraceElement ste) {
            this.walker = walker;
            this.declaringClass = declaringClass;
            this.ste = ste;
        }

        private void ensureMethodInfo() {
            if (walker.dropMethodInfo) {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public String getClassName() {
            return ste.getClassName();
        }

        @Override
        public String getMethodName() {
            ensureMethodInfo();
            return ste.getMethodName();
        }

        @Override
        public Class<?> getDeclaringClass() {
            if (!walker.retainClassRef) {
                throw new UnsupportedOperationException("No access to RETAIN_CLASS_REFERENCE");
            }
            return declaringClass;
        }

        @Override
        public int getByteCodeIndex() {
            ensureMethodInfo();
            return -1;
        }

        @Override
        public String getFileName() {
            ensureMethodInfo();
            return ste.getFileName();
        }

        @Override
        public int getLineNumber() {
            ensureMethodInfo();
            return ste.getLineNumber();
        }

        @Override
        public boolean isNativeMethod() {
            ensureMethodInfo();
            return false;
        }

        @Override
        public StackTraceElement toStackTraceElement() {
            ensureMethodInfo();
            return ste;
        }

        @Override
        public String toString() {
            return dropMethodInfo() ? ste.getClassName() : ste.toString();
        }

        private boolean dropMethodInfo() {
            return walker.dropMethodInfo;
        }
    }
}
