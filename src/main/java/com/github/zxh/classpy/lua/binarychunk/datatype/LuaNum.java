package com.github.zxh.classpy.lua.binarychunk.datatype;

import com.github.zxh.classpy.lua.binarychunk.BinaryChunkComponent;
import com.github.zxh.classpy.lua.binarychunk.BinaryChunkReader;

/**
 * Lua number.
 * typedef LUA_NUMBER lua_Number;
 * #define LUA_NUMBER	float
 * #define LUA_NUMBER	long double
 * #define LUA_NUMBER	double
 *
 * @see /lua/src/lua.h
 * @see /lua/src/luaconf.h
 * @see /lua/src/ldump.c#DumpNumber()
 */
public class LuaNum extends BinaryChunkComponent {

    private double value;

    @Override
    protected void readContent(BinaryChunkReader reader) {
        value = reader.readLuaNum();
        setDesc(Double.toString(value));
    }

}
