package com.sharehub.files;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sharehub.storage.file")
public class FileStorageProperties {

    /**
     * Max file size in bytes.
     */
    private long maxSize = 5 * 1024 * 1024;

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }
}
