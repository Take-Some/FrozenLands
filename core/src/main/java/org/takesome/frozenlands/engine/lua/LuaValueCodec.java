package org.takesome.frozenlands.engine.lua;

import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.takesome.frozenlands.engine.runtime.EngineRuntimeOptions;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Canonical Java/Lua value codec used by the FrozenLands Lua ABI. */
public final class LuaValueCodec {
    private final int maxDepth;
    private final int maxTableEntries;

    public LuaValueCodec() {
        EngineRuntimeOptions options = EngineRuntimeOptions.defaultOptions();
        this.maxDepth = options.luaMaxDepth();
        this.maxTableEntries = options.luaMaxTableEntries();
    }

    public Map<String, Object> mapFromLua(LuaValue value) {
        if (value == null || value.isnil()) {
            return Map.of();
        }
        Object converted = toJava(value);
        if (converted instanceof Map<?, ?> source) {
            Map<String, Object> result = new LinkedHashMap<>();
            source.forEach((key, val) -> result.put(String.valueOf(key), val));
            return result;
        }
        return Map.of();
    }

    public Object toJava(LuaValue value) {
        return toJava(value, 0);
    }

    public LuaValue toLua(Object value) {
        return toLua(value, 0);
    }

    private Object toJava(LuaValue value, int depth) {
        if (depth > maxDepth) {
            throw new IllegalArgumentException("Lua table conversion exceeded maxDepth=" + maxDepth);
        }
        if (value == null || value.isnil()) {
            return null;
        }
        if (value.isboolean()) {
            return value.toboolean();
        }
        if (value.isinttype()) {
            return value.toint();
        }
        if (value.isnumber()) {
            return value.todouble();
        }
        if (value.isstring()) {
            return value.tojstring();
        }
        if (value.istable()) {
            return tableToJava((LuaTable) value, depth + 1);
        }
        return value.tojstring();
    }

    private Object tableToJava(LuaTable table, int depth) {
        Map<Object, Object> entries = new LinkedHashMap<>();
        LuaValue key = LuaValue.NIL;
        int count = 0;
        while (true) {
            Varargs next = table.next(key);
            key = next.arg1();
            if (key.isnil()) {
                break;
            }
            if (++count > maxTableEntries) {
                throw new IllegalArgumentException("Lua table conversion exceeded maxTableEntries=" + maxTableEntries);
            }
            entries.put(toJavaKey(key), toJava(next.arg(2), depth));
        }
        if (isArrayLike(entries)) {
            List<Object> list = new ArrayList<>();
            for (int i = 1; i <= entries.size(); i++) {
                list.add(entries.get(i));
            }
            return list;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        entries.forEach((entryKey, entryValue) -> map.put(String.valueOf(entryKey), entryValue));
        return map;
    }

    private Object toJavaKey(LuaValue key) {
        if (key instanceof LuaInteger || key.isinttype()) {
            return key.toint();
        }
        if (key instanceof LuaString || key.isstring()) {
            return key.tojstring();
        }
        if (key.isnumber()) {
            return key.todouble();
        }
        return key.tojstring();
    }

    private boolean isArrayLike(Map<Object, Object> entries) {
        if (entries.isEmpty()) {
            return false;
        }
        for (Object key : entries.keySet()) {
            if (!(key instanceof Integer index) || index < 1 || index > entries.size()) {
                return false;
            }
        }
        for (int i = 1; i <= entries.size(); i++) {
            if (!entries.containsKey(i)) {
                return false;
            }
        }
        return true;
    }

    private LuaValue toLua(Object value, int depth) {
        if (depth > maxDepth) {
            throw new IllegalArgumentException("Java value conversion exceeded maxDepth=" + maxDepth);
        }
        if (value == null) {
            return LuaValue.NIL;
        }
        if (value instanceof LuaValue luaValue) {
            return luaValue;
        }
        if (value instanceof Boolean bool) {
            return LuaValue.valueOf(bool);
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return LuaValue.valueOf(((Number) value).longValue());
        }
        if (value instanceof Number number) {
            return LuaValue.valueOf(number.doubleValue());
        }
        if (value instanceof CharSequence chars) {
            return LuaValue.valueOf(chars.toString());
        }
        if (value instanceof Map<?, ?> map) {
            LuaTable table = new LuaTable();
            int count = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (++count > maxTableEntries) {
                    throw new IllegalArgumentException("Java map conversion exceeded maxTableEntries=" + maxTableEntries);
                }
                table.set(String.valueOf(entry.getKey()), toLua(entry.getValue(), depth + 1));
            }
            return table;
        }
        if (value instanceof Iterable<?> iterable) {
            LuaTable table = new LuaTable();
            int index = 1;
            for (Object item : iterable) {
                if (index > maxTableEntries) {
                    throw new IllegalArgumentException("Java iterable conversion exceeded maxTableEntries=" + maxTableEntries);
                }
                table.set(index++, toLua(item, depth + 1));
            }
            return table;
        }
        if (value.getClass().isArray()) {
            LuaTable table = new LuaTable();
            int length = Math.min(Array.getLength(value), maxTableEntries);
            for (int i = 0; i < length; i++) {
                table.set(i + 1, toLua(Array.get(value, i), depth + 1));
            }
            return table;
        }
        return LuaValue.valueOf(String.valueOf(value));
    }
}
