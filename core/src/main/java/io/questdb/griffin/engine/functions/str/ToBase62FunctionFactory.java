/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2020 QuestDB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package io.questdb.griffin.engine.functions.str;

import io.questdb.cairo.CairoConfiguration;
import io.questdb.cairo.sql.Function;
import io.questdb.cairo.sql.Record;
import io.questdb.griffin.FunctionFactory;
import io.questdb.griffin.SqlExecutionContext;
import io.questdb.griffin.engine.functions.StrFunction;
import io.questdb.griffin.engine.functions.UnaryFunction;
import io.questdb.std.IntList;
import io.questdb.std.ObjList;

import java.nio.CharBuffer;
import java.util.Arrays;

// quick and dirty private function for uuid -> base64 conversion
public class ToBase62FunctionFactory implements FunctionFactory {
    private static final char[] BASE_DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int[] DIGIT_INDICES;

    static {
        DIGIT_INDICES = new int['z' + 1];
        Arrays.fill(DIGIT_INDICES, -1);

        for (int i = 0; i < BASE_DIGITS.length; i++) {
            char ch = BASE_DIGITS[i];
            DIGIT_INDICES[ch] = i;
        }
    }

    static char getDigit(int index) {
        return BASE_DIGITS[index];
    }

    static int getDigitIndex(char ch) {
        int index = (ch < DIGIT_INDICES.length) ? DIGIT_INDICES[ch] : -1;
        if (index < 0) {
            throw new IllegalArgumentException("Not a valid Base62 character: '" + ch + "'");
        }
        return index;
    }


    @Override
    public String getSignature() {
        return "to_base62(K)";
    }

    @Override
    public Function newInstance(final int position, final ObjList<Function> args, IntList argPositions, final CairoConfiguration configuration, SqlExecutionContext sqlExecutionContext) {
        return new ToBase62Func(args.get(0));
    }

    private static class ToBase62Func extends StrFunction implements UnaryFunction {
        private final CharBuffer charBuffer = CharBuffer.allocate(11);

        private void putDigit(int index, int charIndex) {
            charBuffer.put(index, ToBase62FunctionFactory.getDigit(charIndex));
        }

        public void acceptLong(long value) {
            long v = value;
            if (v < 0) {
                for (int i = 10; i > 0; i--) {
                    putDigit(i, (int) (-(v % 62)));
                    v /= 62;
                }
                putDigit(0, (int) (-(v - 31)));

            } else {
                for (int i = 10; i > 0; i--) {
                    putDigit(i, (int) (v % 62));
                    v /= 62;
                }
                putDigit(0, (int) v);
            }
        }

        private final Function arg;

        public ToBase62Func(final Function arg) {
            this.arg = arg;
        }

        @Override
        public Function getArg() {
            return arg;
        }

        @Override
        public CharSequence getStr(final Record rec) {
            final int anInt = getArg().getInt(rec);
            charBuffer.clear();
            acceptLong(anInt);
            return charBuffer;
        }

        @Override
        public CharSequence getStrB(final Record rec) {
            return getStr(rec);
        }

        @Override
        public int getStrLen(final Record rec) {
            return charBuffer.length();
        }
    }
}
