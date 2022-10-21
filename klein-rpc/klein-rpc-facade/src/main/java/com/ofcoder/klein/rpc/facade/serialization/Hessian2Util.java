package com.ofcoder.klein.rpc.facade.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.ofcoder.klein.common.util.StreamUtil;
import com.ofcoder.klein.rpc.facade.exception.SerializationException;

/**
 * @author far.liu
 */
public class Hessian2Util {

    public static <T> byte[] serialize(final T javaBean) {
        Hessian2Output ho = null;
        ByteArrayOutputStream baos = null;

        try {
            baos = new ByteArrayOutputStream();
            ho = new Hessian2Output(baos);
            ho.writeObject(javaBean);
            ho.flush();
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new SerializationException(ex.getMessage(), ex);
        } finally {
            if (ho != null) {
                try {
                    ho.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            StreamUtil.close(baos);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] serializeData)  {
        Hessian2Input hi = null;
        ByteArrayInputStream bais = null;

        try {
            bais = new ByteArrayInputStream(serializeData);
            hi = new Hessian2Input(bais);
            return (T) hi.readObject();
        } catch (Exception ex) {
            throw new SerializationException(ex.getMessage(), ex);
        } finally {
            if (null != hi) {
                try {
                    hi.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            StreamUtil.close(bais);
        }
    }
}
