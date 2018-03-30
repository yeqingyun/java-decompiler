package com.github.zxh.classpy.lua.binarychunk.datatype;

import com.github.zxh.classpy.lua.binarychunk.BinaryChunkComponent;
import com.github.zxh.classpy.lua.binarychunk.BinaryChunkReader;

/**
 * typedef LUA_INTEGER lua_Integer;
 * #define LUA_INTEGER		int
 * #define LUA_INTEGER		long
 * #define LUA_INTEGER		long long
 * #define LUA_INTEGER		__int64
 *
 * @see /lua/src/lua.h
 * @see /lua/src/luaconf.h
 * @see /lua/src/ldump.c#DumpInteger()
 */
public class LuaInt extends BinaryChunkComponent {

    private long value;

    public long getValue() {
        return value;
    }

    @Override
    protected void readContent(BinaryChunkReader reader) {
        value = reader.readLuaInt();
        setDesc(Long.toString(value));
    }

}
