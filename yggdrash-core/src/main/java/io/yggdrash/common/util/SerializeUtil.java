package io.yggdrash.common.util;

import io.yggdrash.core.exception.SerializeException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SerializeUtil {
    public static byte[] serialize(Object val) {
        byte[] bytes;
        try {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bytesOut);
            oos.writeObject(val);
            oos.flush();
            bytes = bytesOut.toByteArray();
            bytesOut.close();
            oos.close();
        } catch (Exception e) {
            throw new SerializeException(e.getCause());
        }
        return bytes;
    }

    public static Object deserializeBytes(byte[] bytes) {
        Object obj;
        try {
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bytesIn);
            obj = ois.readObject();
            ois.close();
        } catch (Exception e) {
            throw new SerializeException(e.getCause());
        }
        return obj;
    }
}
