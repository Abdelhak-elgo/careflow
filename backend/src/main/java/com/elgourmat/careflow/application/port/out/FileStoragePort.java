package com.elgourmat.careflow.application.port.out;

import java.io.InputStream;

public interface FileStoragePort {

    void put(String objectKey, InputStream data, long contentLength, String contentType);

    InputStream get(String objectKey);

    void delete(String objectKey);
}
