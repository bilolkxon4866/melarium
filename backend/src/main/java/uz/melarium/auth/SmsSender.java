package uz.melarium.auth;

public interface SmsSender {
    void sendOtp(String phone, String code);
}
