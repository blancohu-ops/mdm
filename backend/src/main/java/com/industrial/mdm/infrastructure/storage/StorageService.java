package com.industrial.mdm.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface StorageService {

    Path store(String businessType, String originalFilename, InputStream inputStream) throws IOException;
}
