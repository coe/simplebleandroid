package jp.coe.simpleble;

import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Collectors;

public class JavaUtil {

    public static String tostr(@Nullable ByteBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        byte[] array = buffer.array();
        for (byte d: array) {
            sb.append(String.format("%02X", d));
        }
        return sb.toString();
    }

    @Nullable
    public static String tostr(@Nullable byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (byte d: array) {
            sb.append(String.format("%02X", d));
        }
        return sb.toString();
    }

    public static int toint(@Nullable byte[] array) {
        Long l = ByteBuffer.wrap(array).getLong();
        return l.intValue();
    }

}
