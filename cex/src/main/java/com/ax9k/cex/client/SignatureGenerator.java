package com.ax9k.cex.client;

import org.apache.commons.codec.digest.HmacUtils;

import java.time.Instant;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.digest.HmacAlgorithms.HMAC_SHA_256;
import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public final class SignatureGenerator {
    private final HmacUtils mac;

    private final String apiKey;

    SignatureGenerator(String apiKey, String secretKey) {
        this(apiKey, secretKey.getBytes(UTF_8));
    }

    public SignatureGenerator(String apiKey, byte[] secretKey) {
        this.mac = new HmacUtils(HMAC_SHA_256, notNull(secretKey));
        this.apiKey = notBlank(apiKey, "Invalid API key");
    }

    String generate(Instant timestamp) {
        return generate(timestamp.getEpochSecond());
    }

    String generate(long epochSeconds) {
        return mac.hmacHex(epochSeconds + apiKey);
    }

    String getApiKey() {
        return apiKey;
    }
}
