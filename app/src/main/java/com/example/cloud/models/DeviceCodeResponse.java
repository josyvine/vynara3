package com.example.cloud.models;

import org.json.JSONObject;

public class DeviceCodeResponse {
    private final String deviceCode;
    private final String userCode;
    private final String verificationUri;
    private final int expiresIn;
    private final int interval;

    public DeviceCodeResponse(String deviceCode, String userCode, String verificationUri, int expiresIn, int interval) {
        this.deviceCode = deviceCode != null ? deviceCode : "";
        this.userCode = userCode != null ? userCode : "";
        this.verificationUri = verificationUri != null ? verificationUri : "";
        this.expiresIn = expiresIn;
        this.interval = interval > 0 ? interval : 5;
    }

    public static DeviceCodeResponse fromJson(JSONObject json) {
        if (json == null) {
            return new DeviceCodeResponse("", "", "", 0, 5);
        }
        String deviceCode = json.optString("device_code", "");
        String userCode = json.optString("user_code", "");
        String verificationUri = json.optString("verification_uri", "");
        int expiresIn = json.optInt("expires_in", 900);
        int interval = json.optInt("interval", 5);

        return new DeviceCodeResponse(deviceCode, userCode, verificationUri, expiresIn, interval);
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public String getUserCode() {
        return userCode;
    }

    public String getVerificationUri() {
        return verificationUri;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public int getInterval() {
        return interval;
    }

    public boolean isValid() {
        return !deviceCode.isEmpty() && !userCode.isEmpty() && !verificationUri.isEmpty();
    }

    @Override
    public String toString() {
        return "DeviceCodeResponse{" +
                "deviceCode='" + deviceCode + '\'' +
                ", userCode='" + userCode + '\'' +
                ", verificationUri='" + verificationUri + '\'' +
                ", expiresIn=" + expiresIn +
                ", interval=" + interval +
                '}';
    }
}