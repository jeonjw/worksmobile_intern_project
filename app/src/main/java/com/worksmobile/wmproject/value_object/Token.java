package com.worksmobile.wmproject.value_object;


import com.google.gson.annotations.SerializedName;


public class Token {

    @SerializedName("access_token")
    private String accessToken;
    @SerializedName("expires_in")
    private long expiresIn;
    @SerializedName("token_type")
    private String tokenType;
    @SerializedName("refresh_token")
    private String refreshToken;

    private long tokenTimestamp;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setTokenTimeStamp(long tokenTimestamp) {
        this.tokenTimestamp = tokenTimestamp;
    }

    public boolean getNeedsTokenRefresh() {
        return refreshToken != null && System.currentTimeMillis() > tokenTimestamp + (expiresIn - 600) * 1000;
    }

    @Override
    public String toString() {
        return "Token <" + "accessToken = " + accessToken +
                ", expires_in = " + expiresIn + ", refreshToken = " + refreshToken + '>';
    }
}
