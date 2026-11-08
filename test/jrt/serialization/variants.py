#!/usr/bin/env python3
# CDR10 (doc/CLOJURE-DESIGN-REVIEW.md; doc/go/JRT-NOTES.md, "Serialization") and JCS167 (doc/classes/
# JAVA-DEFERRED-TRIAGE.md, "As built: JCS167 (write-ahead)"): jrt's variants of jdk26u's
# java.io.ObjectStreamClass and ObjectInputStream (openjdk/jdk26u at baf63fb), made from jdk26u's text by
# these edits, so that they are replayed on jdk26u's next revision (JCS166 (a): to be ported to Arbace,
# bin/jrt-convert derive):
#   python3 test/jrt/serialization/variants.py JDK OUT   (JDK: jdk26u's checkout; OUT: a java.base tree,
#   overlay/jdk/java.base for the committed overlays)
# ObjectStreamClass: the serializable constructor is jrt's allocation (jdk.internal.jrt.SerialAlloc,
# ReflectionFactory.newConstructorForSerialization's), the field reflector reads and writes fields by
# java.lang.reflect.Field (Unsafe's offsets are not in the world), a record is made by its canonical
# Constructor from the stream's values (RecordSupport.Ctr, method handles are not in the world), a record's
# components are its instance fields in declaration order (java.lang.reflect.RecordComponent is not in the
# world; JLS 8.10.3: a record's instance fields are its components'), the misdeclaration event is not raised
# (JFR is not in the world). ObjectInputStream: readRecord calls RecordSupport.Ctr, no DeserializationEvent,
# freeze's fence is Unsafe.storeFence's absence: Go's memory model publishes a value through the
# synchronization that hands the graph over (jrt has no Unsafe). JavaLangAccess (jrt's has countPositives,
# inflateBytesToChars, countNonZeroAscii) and SharedSecrets' serialization accessors (j2g's, m/j2g-static?)
# stay jdk26u's; ObjectOutputStream is jdk26u's unchanged.
import re, sys, os
jdk, out = sys.argv[1], sys.argv[2]
src = jdk + '/src/java.base/share/classes/java/io/'
HDR = '''/*
 * Arbace (CDR10, doc/CLOJURE-DESIGN-REVIEW.md, the user's decision of 2026-11-06; JCS167 (a), the user's
 * decision of 2026-11-07, doc/classes/JAVA-DEFERRED-TRIAGE.md): jrt's variant of
 * jdk26u's java.io.%s (openjdk/jdk26u at baf63fb, src/java.base/share/classes/java/io/%s.java),
 * made by test/jrt/serialization/variants.py from jdk26u's text (doc/go/JRT-NOTES.md, "Serialization");
 * otherwise jdk26u's text, under its notice below.
 */
'''
def sub(s, old, new, count=1):
    if old not in s:
        raise SystemExit('variants.py: not found: ' + old[:80])
    return s.replace(old, new, count)

def member_end(s, start):
    # the end of the brace block that starts at the first '{' after start
    i = s.index('{', start); d = 0
    while True:
        c = s[i]
        if c == '{': d += 1
        elif c == '}':
            d -= 1
            if d == 0: return i + 1
        elif c == '"':
            i += 1
            while s[i] != '"':
                if s[i] == '\\': i += 1
                i += 1
        elif c == "'":
            i += 1
            while s[i] != "'":
                if s[i] == '\\': i += 1
                i += 1
        elif s.startswith('//', i):
            i = s.index('\n', i)
        elif s.startswith('/*', i):
            i = s.index('*/', i) + 1
        i += 1

def replace_member(s, head, new):
    i = s.index(head)
    j = member_end(s, i)
    return s[:i] + new + s[j:]

# ------------------------------------------------------------------- ObjectStreamClass
s = open(src + 'ObjectStreamClass.java').read()
for imp in ['import java.lang.invoke.MethodHandle;\n', 'import java.lang.invoke.MethodHandles;\n',
            'import java.lang.invoke.MethodType;\n', 'import java.lang.reflect.RecordComponent;\n',
            'import jdk.internal.event.SerializationMisdeclarationEvent;\n',
            'import jdk.internal.misc.Unsafe;\n', 'import jdk.internal.reflect.ReflectionFactory;\n']:
    s = sub(s, imp, '')
s = sub(s, 'import jdk.internal.util.ByteArray;\n', 'import jdk.internal.jrt.SerialAlloc;\nimport jdk.internal.util.ByteArray;\n')
s = sub(s, '    private MethodHandle canonicalCtr;', '    private Constructor<?> canonicalCtr;')
s = sub(s, '    private MethodHandle deserializationCtr;', '    private RecordSupport.Ctr deserializationCtr;')
# the serializable constructor: jrt's allocation, marked by serialAlloc
s = sub(s, '    private Constructor<?> cons;\n', '    private Constructor<?> cons;\n    /** Arbace: the objects are made by jrt\'s allocation (SerialAlloc), not by cons. */\n    private boolean serialAlloc;\n')
s = sub(s, '                    cons = getSerializableConstructor(cl);', '                    serialAlloc = SerialAlloc.canAllocate(cl);')
s = sub(s, '            } else if (cons == null && !isRecord) {', '            } else if (cons == null && !serialAlloc && !isRecord) {')
s = sub(s, '            cons = localDesc.cons;\n', '            cons = localDesc.cons;\n            serialAlloc = localDesc.serialAlloc;\n', 2)
s = sub(s, '        return (cons != null);', '        return (cons != null || serialAlloc);')
s = sub(s, '''        requireInitialized();
        if (cons != null) {''', '''        requireInitialized();
        if (serialAlloc) {
            try {
                return SerialAlloc.allocate(cl);
            } catch (InvalidClassException ex) {
                throw new UnsupportedOperationException(ex.getMessage());
            }
        }
        if (cons != null) {''')
s = replace_member(s, '    private static Constructor<?> getSerializableConstructor(Class<?> cl) {', '')
s = sub(s, '''        if (SerializationMisdeclarationEvent.enabled() && serializable) {
            SerializationMisdeclarationChecker.checkMisdeclarations(cl);
        }
''', '')
s = replace_member(s, '    private static MethodHandle canonicalRecordCtr(Class<?> cls) {', '''    /**
     * Arbace: the components of record class cls, as its instance fields in declaration order
     * (java.lang.reflect.RecordComponent is not in the Go build's world; JLS 8.10.3: a record
     * class's instance fields are its components').
     */
    static Field[] recordComponentFields(Class<?> cls) {
        ArrayList<Field> fs = new ArrayList<>();
        for (Field f : cls.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                fs.add(f);
            }
        }
        return fs.toArray(new Field[0]);
    }

    private static Constructor<?> canonicalRecordCtr(Class<?> cls) {
        assert cls.isRecord() : "Expected record, got: " + cls;
        Field[] components = recordComponentFields(cls);
        Class<?>[] paramTypes = new Class<?>[components.length];
        for (int i = 0; i < components.length; i++) {
            paramTypes[i] = components[i].getType();
        }
        try {
            Constructor<?> ctr = cls.getDeclaredConstructor(paramTypes);
            ctr.setAccessible(true);
            return ctr;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }''')
s = sub(s, '    MethodHandle getRecordConstructor() {', '    Constructor<?> getRecordConstructor() {')
# the field reflector: Field.get and Field.set in place of Unsafe's offsets
fr_head = '    private static final class FieldReflector {'
s = replace_member(s, fr_head, '''    private static final class FieldReflector {

        private final ObjectStreamField[] fields;
        private final int numPrimFields;
        /** the fields read (null: no field of the class) and the fields written (null: discarded) */
        private final Field[] readFields;
        private final Field[] writeFields;
        private final int[] offsets;
        private final char[] typeCodes;
        private final Class<?>[] types;

        FieldReflector(ObjectStreamField[] fields) {
            this.fields = fields;
            int nfields = fields.length;
            readFields = new Field[nfields];
            writeFields = new Field[nfields];
            offsets = new int[nfields];
            typeCodes = new char[nfields];
            ArrayList<Class<?>> typeList = new ArrayList<>();
            Set<Field> usedFields = new HashSet<>();

            for (int i = 0; i < nfields; i++) {
                ObjectStreamField f = fields[i];
                Field rf = f.getField();
                if (rf != null) {
                    rf.setAccessible(true);
                }
                readFields[i] = rf;
                writeFields[i] = (rf != null && usedFields.add(rf)) ? rf : null;
                offsets[i] = f.getOffset();
                typeCodes[i] = f.getTypeCode();
                if (!f.isPrimitive()) {
                    typeList.add((rf != null) ? rf.getType() : null);
                }
            }

            types = typeList.toArray(new Class<?>[typeList.size()]);
            numPrimFields = nfields - types.length;
        }

        ObjectStreamField[] getFields() {
            return fields;
        }

        void getPrimFieldValues(Object obj, byte[] buf) {
            if (obj == null) {
                throw new NullPointerException();
            }
            try {
                for (int i = 0; i < numPrimFields; i++) {
                    Field f = readFields[i];
                    int off = offsets[i];
                    switch (typeCodes[i]) {
                        case 'Z' -> ByteArray.setBoolean(buf, off, f.getBoolean(obj));
                        case 'B' -> buf[off] = f.getByte(obj);
                        case 'C' -> ByteArray.setChar(buf, off, f.getChar(obj));
                        case 'S' -> ByteArray.setShort(buf, off, f.getShort(obj));
                        case 'I' -> ByteArray.setInt(buf, off, f.getInt(obj));
                        case 'F' -> ByteArray.setFloat(buf, off, f.getFloat(obj));
                        case 'J' -> ByteArray.setLong(buf, off, f.getLong(obj));
                        case 'D' -> ByteArray.setDouble(buf, off, f.getDouble(obj));
                        default  -> throw new InternalError();
                    }
                }
            } catch (IllegalAccessException ex) {
                throw new InternalError(ex);    // access checks are suppressed
            }
        }

        void setPrimFieldValues(Object obj, byte[] buf) {
            if (obj == null) {
                throw new NullPointerException();
            }
            try {
                for (int i = 0; i < numPrimFields; i++) {
                    Field f = writeFields[i];
                    if (f == null) {
                        continue;           // discard value
                    }
                    int off = offsets[i];
                    switch (typeCodes[i]) {
                        case 'Z' -> f.setBoolean(obj, ByteArray.getBoolean(buf, off));
                        case 'B' -> f.setByte(obj, buf[off]);
                        case 'C' -> f.setChar(obj, ByteArray.getChar(buf, off));
                        case 'S' -> f.setShort(obj, ByteArray.getShort(buf, off));
                        case 'I' -> f.setInt(obj, ByteArray.getInt(buf, off));
                        case 'F' -> f.setFloat(obj, ByteArray.getFloat(buf, off));
                        case 'J' -> f.setLong(obj, ByteArray.getLong(buf, off));
                        case 'D' -> f.setDouble(obj, ByteArray.getDouble(buf, off));
                        default  -> throw new InternalError();
                    }
                }
            } catch (IllegalAccessException ex) {
                throw new InternalError(ex);
            }
        }

        void getObjFieldValues(Object obj, Object[] vals) {
            if (obj == null) {
                throw new NullPointerException();
            }
            try {
                for (int i = numPrimFields; i < fields.length; i++) {
                    vals[offsets[i]] = switch (typeCodes[i]) {
                        case 'L', '[' -> readFields[i].get(obj);
                        default       -> throw new InternalError();
                    };
                }
            } catch (IllegalAccessException ex) {
                throw new InternalError(ex);
            }
        }

        void checkObjectFieldValueTypes(Object obj, Object[] vals) {
            setObjFieldValues(obj, vals, true);
        }

        void setObjFieldValues(Object obj, Object[] vals) {
            setObjFieldValues(obj, vals, false);
        }

        private void setObjFieldValues(Object obj, Object[] vals, boolean dryRun) {
            if (obj == null) {
                throw new NullPointerException();
            }
            for (int i = numPrimFields; i < fields.length; i++) {
                Field wf = writeFields[i];
                if (wf == null) {
                    continue;           // discard value
                }
                switch (typeCodes[i]) {
                    case 'L', '[' -> {
                        Object val = vals[offsets[i]];
                        if (val != null &&
                            !types[i - numPrimFields].isInstance(val))
                        {
                            Field f = fields[i].getField();
                            throw new ClassCastException(
                                "cannot assign instance of " +
                                val.getClass().getName() + " to field " +
                                f.getDeclaringClass().getName() + "." +
                                f.getName() + " of type " +
                                f.getType().getName() + " in instance of " +
                                obj.getClass().getName());
                        }
                        if (!dryRun) {
                            try {
                                wf.set(obj, val);
                            } catch (IllegalAccessException ex) {
                                throw new InternalError(ex);
                            }
                        }
                    }
                    default -> throw new InternalError();
                }
            }
        }
    }''')
# the deserialization constructors' cache: of RecordSupport.Ctr
s = s.replace('extends ConcurrentHashMap<DeserializationConstructorsCache.Key, MethodHandle>',
              'extends ConcurrentHashMap<DeserializationConstructorsCache.Key, RecordSupport.Ctr>')
s = sub(s, '        MethodHandle get(ObjectStreamField[] fields) {', '        RecordSupport.Ctr get(ObjectStreamField[] fields) {')
s = sub(s, '        synchronized MethodHandle putIfAbsentAndGet(ObjectStreamField[] fields, MethodHandle mh) {',
        '        synchronized RecordSupport.Ctr putIfAbsentAndGet(ObjectStreamField[] fields, RecordSupport.Ctr mh) {')
s = s.replace('            MethodHandle oldMh = putIfAbsent(key, mh);', '            RecordSupport.Ctr oldMh = putIfAbsent(key, mh);')
# RecordSupport: the canonical constructor called with each component's stream value
s = replace_member(s, '    static final class RecordSupport {', '''    static final class RecordSupport {

        /** A record's construction from the stream's primitive and object field values (its
         *  canonical constructor's own exception thrown as the JDK's method handle throws it). */
        interface Ctr {
            Object construct(byte[] primValues, Object[] objValues) throws Throwable;
        }

        /** The values' place of a component: a primitive at an offset, an object at an index, or none. */
        private record Arg(Class<?> type, boolean prim, int at) { }

        /**
         * Returns the deserialization constructor of desc's record class: the canonical
         * constructor called with each component's value from the stream (its field of the same
         * name), or the type's default when the stream has none.
         */
        static Ctr deserializationCtr(ObjectStreamClass desc) {
            // check the cached value 1st
            Ctr mh = desc.deserializationCtr;
            if (mh != null) return mh;
            mh = desc.deserializationCtrs.get(desc.getFields(false));
            if (mh != null) return desc.deserializationCtr = mh;

            Field[] recordComponents = recordComponentFields(desc.forClass());
            Constructor<?> ctr = desc.getRecordConstructor();
            Arg[] args = new Arg[recordComponents.length];
            for (int i = 0; i < recordComponents.length; i++) {
                args[i] = streamFieldExtractor(recordComponents[i].getName(), recordComponents[i].getType(), desc);
            }
            mh = (primValues, objValues) -> {
                Object[] vs = new Object[args.length];
                for (int i = 0; i < args.length; i++) {
                    vs[i] = args[i].at() < 0 ? defaultValue(args[i].type())
                          : args[i].prim() ? primValue(args[i].type(), primValues, args[i].at())
                          : objValues[args[i].at()];
                }
                try {
                    return ctr.newInstance(vs);
                } catch (InvocationTargetException e) {
                    // the canonical constructor's own exception, as invokeExact throws it
                    throw e.getCause();
                }
            };
            return desc.deserializationCtr =
                desc.deserializationCtrs.putIfAbsentAndGet(desc.getFields(false), mh);
        }

        private static int numberPrimValues(ObjectStreamClass desc) {
            ObjectStreamField[] fields = desc.getFields();
            int primValueCount = 0;
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].isPrimitive())
                    primValueCount++;
                else
                    break;  // can be no more
            }
            return primValueCount;
        }

        private static Arg streamFieldExtractor(String pName, Class<?> pType, ObjectStreamClass desc) {
            ObjectStreamField[] fields = desc.getFields(false);

            for (int i = 0; i < fields.length; i++) {
                ObjectStreamField f = fields[i];
                String fName = f.getName();
                if (!fName.equals(pName))
                    continue;

                Class<?> fType = f.getField().getType();
                if (!pType.isAssignableFrom(fType))
                    throw new InternalError(fName + " unassignable, pType:" + pType + ", fType:" + fType);

                if (f.isPrimitive()) {
                    return new Arg(fType, true, f.getOffset());
                } else {
                    return new Arg(fType, false, i - numberPrimValues(desc));
                }
            }
            // the default value if no field matches pName
            return new Arg(pType, pType.isPrimitive(), -1);
        }

        private static Object primValue(Class<?> t, byte[] b, int off) {
            if (t == byte.class) return b[off];
            if (t == short.class) return ByteArray.getShort(b, off);
            if (t == int.class) return ByteArray.getInt(b, off);
            if (t == long.class) return ByteArray.getLong(b, off);
            if (t == float.class) return ByteArray.getFloat(b, off);
            if (t == double.class) return ByteArray.getDouble(b, off);
            if (t == char.class) return ByteArray.getChar(b, off);
            if (t == boolean.class) return ByteArray.getBoolean(b, off);
            throw new InternalError("Unexpected type: " + t);
        }

        private static Object defaultValue(Class<?> t) {
            if (!t.isPrimitive()) return null;
            if (t == boolean.class) return false;
            if (t == char.class) return (char) 0;
            if (t == byte.class) return (byte) 0;
            if (t == short.class) return (short) 0;
            if (t == int.class) return 0;
            if (t == long.class) return 0L;
            if (t == float.class) return 0f;
            return 0d;
        }
    }''')
os.makedirs(out + '/java/io', exist_ok=True)
open(out + '/java/io/ObjectStreamClass.java', 'w').write(HDR % ('ObjectStreamClass', 'ObjectStreamClass') + s)

# ------------------------------------------------------------------- ObjectInputStream
s = open(src + 'ObjectInputStream.java').read()
s = sub(s, 'import jdk.internal.misc.Unsafe;\n', '')
s = re.sub(r'import java\.lang\.invoke\.MethodHandle;\n', '', s)
s = sub(s, '''        MethodHandle ctrMH = RecordSupport.deserializationCtr(desc);

        try {
            return (Object) ctrMH.invokeExact(fieldValues.primValues, fieldValues.objValues);''', '''        RecordSupport.Ctr ctrMH = RecordSupport.deserializationCtr(desc);

        try {
            return ctrMH.construct(fieldValues.primValues, fieldValues.objValues);''')
s = sub(s, '    private static final Unsafe UNSAFE = Unsafe.getUnsafe();\n', '')
s = sub(s, '''        UNSAFE.storeFence();''', '''        // Arbace: no fence; Go's memory model publishes the graph through the synchronization
        // that hands it to another goroutine (jrt has no Unsafe)''')
s = sub(s, 'import jdk.internal.event.DeserializationEvent;\n', '')
i = s.index('        DeserializationEvent event = new DeserializationEvent();')
j = s.index('        if (serialFilter != null && (status == null', i)
s = s[:i] + '        // Arbace: no DeserializationEvent (JFR is not in the world)\n' + s[j:]
open(out + '/java/io/ObjectInputStream.java', 'w').write(HDR % ('ObjectInputStream', 'ObjectInputStream') + s)

print('variants.py: ObjectStreamClass, ObjectInputStream written into', out)
