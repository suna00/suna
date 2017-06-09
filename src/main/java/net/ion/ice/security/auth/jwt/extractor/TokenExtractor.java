package net.ion.ice.security.auth.jwt.extractor;

public interface TokenExtractor {
    public String extract(String payload);
}
